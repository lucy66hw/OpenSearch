/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.admin.indices.settings.put;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.action.admin.indices.mapping.get.GetInferredFieldsRequest;
import org.opensearch.action.admin.indices.mapping.get.TransportGetInferredFieldsAction;
import org.opensearch.action.admin.indices.mapping.put.PutMappingClusterStateUpdateRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.GroupedActionListener;
import org.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ack.ClusterStateUpdateResponse;
import org.opensearch.cluster.block.ClusterBlockException;
import org.opensearch.cluster.block.ClusterBlockLevel;
import org.opensearch.cluster.block.ClusterBlocks;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.MetadataMappingService;
import org.opensearch.cluster.metadata.MetadataUpdateSettingsService;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.index.Index;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.IndexSettings;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Transport action for updating index settings
 *
 * @opensearch.internal
 */
public class TransportUpdateSettingsAction extends TransportClusterManagerNodeAction<UpdateSettingsRequest, AcknowledgedResponse> {

    private static final Logger logger = LogManager.getLogger(TransportUpdateSettingsAction.class);

    private final static Set<String> ALLOWLIST_REMOTE_SNAPSHOT_SETTINGS = Set.of(
        "index.max_result_window",
        "index.max_inner_result_window",
        "index.max_rescore_window",
        "index.max_docvalue_fields_search",
        "index.max_script_fields",
        "index.max_terms_count",
        "index.max_regex_length",
        "index.highlight.max_analyzed_offset",
        "index.number_of_replicas"
    );

    private final static String[] ALLOWLIST_REMOTE_SNAPSHOT_SETTINGS_PREFIXES = { "index.search.slowlog", "index.routing.allocation" };

    private final MetadataUpdateSettingsService updateSettingsService;
    private final MetadataMappingService metadataMappingService;
    private final TransportGetInferredFieldsAction transportGetInferredFieldsAction;

    @Inject
    public TransportUpdateSettingsAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        MetadataUpdateSettingsService updateSettingsService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        MetadataMappingService metadataMappingService,
        TransportGetInferredFieldsAction transportGetInferredFieldsAction
    ) {
        super(
            UpdateSettingsAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            UpdateSettingsRequest::new,
            indexNameExpressionResolver
        );
        this.updateSettingsService = updateSettingsService;
        this.metadataMappingService = metadataMappingService;
        this.transportGetInferredFieldsAction = transportGetInferredFieldsAction;
    }

    @Override
    protected String executor() {
        // we go async right away....
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterBlockException checkBlock(UpdateSettingsRequest request, ClusterState state) {
        // allow for dedicated changes to the metadata blocks, so we don't block those to allow to "re-enable" it
        ClusterBlockException globalBlock = state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        if (globalBlock != null) {
            return globalBlock;
        }
        if (request.settings().size() == 1 &&  // we have to allow resetting these settings otherwise users can't unblock an index
            ClusterBlocks.INDEX_DATA_READ_ONLY_BLOCK_SETTINGS.stream()
                .anyMatch(booleanSetting -> booleanSetting.exists(request.settings()))) {
            return null;
        }

        final Index[] requestIndices = indexNameExpressionResolver.concreteIndices(state, request);
        boolean allowSearchableSnapshotSettingsUpdate = true;
        // check if all indices in the request are remote snapshot
        for (Index index : requestIndices) {
            if (state.blocks().indexBlocked(ClusterBlockLevel.METADATA_WRITE, index.getName())) {
                allowSearchableSnapshotSettingsUpdate = allowSearchableSnapshotSettingsUpdate
                    && state.getMetadata().getIndexSafe(index).isRemoteSnapshot();
            }
        }
        // check if all settings in the request are in the allow list
        if (allowSearchableSnapshotSettingsUpdate) {
            for (String setting : request.settings().keySet()) {
                allowSearchableSnapshotSettingsUpdate = allowSearchableSnapshotSettingsUpdate
                    && (ALLOWLIST_REMOTE_SNAPSHOT_SETTINGS.contains(setting)
                        || Stream.of(ALLOWLIST_REMOTE_SNAPSHOT_SETTINGS_PREFIXES).anyMatch(setting::startsWith));
            }
        }

        final String[] requestIndexNames = Arrays.stream(requestIndices).map(Index::getName).toArray(String[]::new);
        return allowSearchableSnapshotSettingsUpdate
            ? null
            : state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, requestIndexNames);
    }

    @Override
    protected AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        final UpdateSettingsRequest request,
        final ClusterState state,
        final ActionListener<AcknowledgedResponse> listener
    ) {
        final Index[] concreteIndices = indexNameExpressionResolver.concreteIndices(state, request);
        final Settings newSettings = Settings.builder().put(request.settings()).normalizePrefix(IndexMetadata.INDEX_SETTING_PREFIX).build();

        List<String> indicesToPromote = getIndicesRequiringPromotion(newSettings, concreteIndices, state);

        if (indicesToPromote.isEmpty()) {
            executeSettingsUpdate(request, concreteIndices, listener);
            return;
        }

        // Step 1: Get inferred fields from Lucene (while mode is still enabled)
        GetInferredFieldsRequest inferredRequest = new GetInferredFieldsRequest(indicesToPromote.toArray(new String[0]));
        transportGetInferredFieldsAction.execute(inferredRequest, ActionListener.wrap(inferredResponse -> {
            // Step 2: Promote inferred fields to explicit keyword mappings
            promoteInferredFields(inferredResponse.getInferredFieldsByIndex(), state, ActionListener.wrap(promoteResponse -> {
                // Step 3: Disable inferred mode (safe now — fields are already in the mapping)
                executeSettingsUpdate(request, concreteIndices, listener);
            }, listener::onFailure));
        }, listener::onFailure));
    }

    List<String> getIndicesRequiringPromotion(Settings newSettings, Index[] concreteIndices, ClusterState state) {
        if (IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.exists(newSettings) == false
            || IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.get(newSettings)) {
            return List.of();
        }

        List<String> indicesToPromote = new ArrayList<>();
        for (Index index : concreteIndices) {
            IndexMetadata indexMetadata = state.metadata().index(index);
            if (indexMetadata == null) continue;
            Settings indexSettings = indexMetadata.getSettings();
            boolean currentlyEnabled = IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.get(indexSettings);
            boolean promoteOnDisable = IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.exists(newSettings)
                ? IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.get(newSettings)
                : IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.get(indexSettings);
            if (currentlyEnabled && promoteOnDisable) {
                indicesToPromote.add(index.getName());
            }
        }
        return indicesToPromote;
    }

    private void executeSettingsUpdate(
        UpdateSettingsRequest request,
        Index[] concreteIndices,
        ActionListener<AcknowledgedResponse> listener
    ) {
        UpdateSettingsClusterStateUpdateRequest clusterStateUpdateRequest = new UpdateSettingsClusterStateUpdateRequest().indices(
            concreteIndices
        )
            .settings(request.settings())
            .setPreserveExisting(request.isPreserveExisting())
            .ackTimeout(request.timeout())
            .masterNodeTimeout(request.clusterManagerNodeTimeout());

        updateSettingsService.updateSettings(clusterStateUpdateRequest, new ActionListener<ClusterStateUpdateResponse>() {
            @Override
            public void onResponse(ClusterStateUpdateResponse response) {
                listener.onResponse(new AcknowledgedResponse(response.isAcknowledged()));
            }

            @Override
            public void onFailure(Exception t) {
                logger.debug(() -> new ParameterizedMessage("failed to update settings on indices [{}]", (Object) concreteIndices), t);
                listener.onFailure(t);
            }
        });
    }

    private void promoteInferredFields(Map<String, Set<String>> inferredByIndex, ClusterState state, ActionListener<Void> listener) {
        List<Map.Entry<String, Set<String>>> toPromote = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : inferredByIndex.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                toPromote.add(entry);
            }
        }

        if (toPromote.isEmpty()) {
            listener.onResponse(null);
            return;
        }

        GroupedActionListener<ClusterStateUpdateResponse> groupedListener = new GroupedActionListener<>(
            ActionListener.wrap(responses -> listener.onResponse(null), listener::onFailure),
            toPromote.size()
        );

        for (Map.Entry<String, Set<String>> entry : toPromote) {
            String indexName = entry.getKey();
            Set<String> fields = entry.getValue();

            try {
                XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject("properties");
                for (String fieldName : fields) {
                    mappingBuilder.startObject(fieldName).field("type", "keyword").endObject();
                }
                mappingBuilder.endObject().endObject();

                IndexMetadata indexMetadata = state.metadata().index(indexName);
                PutMappingClusterStateUpdateRequest updateRequest = new PutMappingClusterStateUpdateRequest(mappingBuilder.toString())
                    .indices(new Index[] { indexMetadata.getIndex() });

                logger.info("Promoting {} inferred fields to explicit mappings for index [{}]", fields.size(), indexName);
                metadataMappingService.putMapping(updateRequest, groupedListener);
            } catch (IOException e) {
                groupedListener.onFailure(e);
            }
        }
    }
}
