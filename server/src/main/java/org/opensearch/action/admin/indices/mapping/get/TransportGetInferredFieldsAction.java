/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.opensearch.Version;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.opensearch.action.support.broadcast.TransportBroadcastAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.block.ClusterBlockException;
import org.opensearch.cluster.block.ClusterBlockLevel;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.routing.GroupShardsIterator;
import org.opensearch.cluster.routing.ShardIterator;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.support.DefaultShardOperationFailedException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.IndexService;
import org.opensearch.index.engine.Engine;
import org.opensearch.index.mapper.MapperService;
import org.opensearch.index.shard.IndexShard;
import org.opensearch.indices.IndicesService;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Transport action that broadcasts to all shards to collect inferred (dynamic) field names,
 * then merges them per index (union).
 *
 * @opensearch.internal
 */
public class TransportGetInferredFieldsAction extends TransportBroadcastAction<
    GetInferredFieldsRequest,
    GetInferredFieldsResponse,
    GetInferredFieldsShardRequest,
    GetInferredFieldsShardResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportGetInferredFieldsAction(
        TransportService transportService,
        ClusterService clusterService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        IndicesService indicesService
    ) {
        super(
            GetInferredFieldsAction.NAME,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            GetInferredFieldsRequest::new,
            GetInferredFieldsShardRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.indicesService = indicesService;
    }

    @Override
    protected GroupShardsIterator<ShardIterator> shards(
        ClusterState clusterState,
        GetInferredFieldsRequest request,
        String[] concreteIndices
    ) {
        return clusterState.getRoutingTable().activePrimaryShardsGrouped(concreteIndices, false);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, GetInferredFieldsRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, GetInferredFieldsRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.READ, concreteIndices);
    }

    @Override
    protected GetInferredFieldsShardRequest newShardRequest(int numShards, ShardRouting shard, GetInferredFieldsRequest request) {
        return new GetInferredFieldsShardRequest(shard.shardId(), request);
    }

    @Override
    protected GetInferredFieldsShardResponse readShardResponse(StreamInput in) throws IOException {
        return new GetInferredFieldsShardResponse(in);
    }

    @Override
    protected GetInferredFieldsShardResponse shardOperation(GetInferredFieldsShardRequest request, Task task) throws IOException {
        ShardId shardId = request.shardId();
        IndexService indexService = indicesService.indexServiceSafe(shardId.getIndex());
        IndexShard indexShard = indexService.getShardOrNull(shardId.id());
        if (indexShard == null) {
            return new GetInferredFieldsShardResponse(shardId, Set.of());
        }

        MapperService mapperService = indexService.mapperService();
        Version indexVersion = indexService.getIndexSettings().getIndexVersionCreated();
        Set<String> inferred;

        try (Engine.Searcher searcher = indexShard.acquireSearcher("get_inferred_fields")) {
            IndexReader reader = searcher.getIndexReader();
            FieldInfos fieldInfos = FieldInfos.getMergedFieldInfos(reader);
            inferred = new HashSet<>(fieldInfos.size());
            for (FieldInfo fi : fieldInfos) {
                String name = fi.getName();
                if (indicesService.isMetadataField(indexVersion, name)) {
                    continue;
                }
                if (mapperService.fieldType(name) != null) {
                    continue;
                }
                inferred.add(name);
            }
        }

        return new GetInferredFieldsShardResponse(shardId, Set.copyOf(inferred));
    }

    @Override
    protected GetInferredFieldsResponse newResponse(
        GetInferredFieldsRequest request,
        AtomicReferenceArray shardsResponses,
        ClusterState clusterState
    ) {
        Map<String, Set<String>> byIndex = new HashMap<>();
        int totalShards = shardsResponses.length();
        int successfulShards = 0;
        int failedShards = 0;
        List<DefaultShardOperationFailedException> failures = new ArrayList<>();

        for (int i = 0; i < totalShards; i++) {
            Object r = shardsResponses.get(i);
            if (r == null) {
                // non-active shard, skip
            } else if (r instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                failures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) r));
            } else {
                GetInferredFieldsShardResponse shardResp = (GetInferredFieldsShardResponse) r;
                successfulShards++;
                String indexName = shardResp.getIndex();
                byIndex.computeIfAbsent(indexName, k -> new HashSet<>()).addAll(shardResp.getInferredFieldNames());
            }
        }

        for (Map.Entry<String, Set<String>> e : byIndex.entrySet()) {
            byIndex.put(e.getKey(), Collections.unmodifiableSet(e.getValue()));
        }

        return new GetInferredFieldsResponse(totalShards, successfulShards, failedShards, failures, Collections.unmodifiableMap(byIndex));
    }
}
