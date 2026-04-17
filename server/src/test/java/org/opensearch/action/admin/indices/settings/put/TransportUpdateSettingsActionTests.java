/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.settings.put;

import org.opensearch.Version;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.MetadataUpdateSettingsService;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.index.Index;
import org.opensearch.index.IndexSettings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;
import org.junit.After;
import org.junit.Before;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptySet;
import static org.mockito.Mockito.mock;

public class TransportUpdateSettingsActionTests extends OpenSearchTestCase {

    private ThreadPool threadPool;
    private TransportUpdateSettingsAction action;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("TransportUpdateSettingsActionTests");
        action = new TransportUpdateSettingsAction(
            mock(TransportService.class),
            mock(ClusterService.class),
            threadPool,
            mock(MetadataUpdateSettingsService.class),
            new ActionFilters(emptySet()),
            new IndexNameExpressionResolver(new ThreadContext(Settings.EMPTY)),
            null,
            null
        );
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    private IndexMetadata buildIndexMetadata(String name, Settings extraSettings) {
        return IndexMetadata.builder(name)
            .settings(
                Settings.builder()
                    .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
                    .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
                    .put(extraSettings)
            )
            .build();
    }

    private ClusterState clusterStateWith(IndexMetadata... indices) {
        Metadata.Builder metadataBuilder = Metadata.builder();
        for (IndexMetadata im : indices) {
            metadataBuilder.put(im, false);
        }
        return ClusterState.builder(
            org.opensearch.cluster.ClusterName.CLUSTER_NAME_SETTING.getDefault(Settings.EMPTY)
        ).metadata(metadataBuilder).build();
    }

    public void testNoPromotionWhenNotDisablingInferredMode() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put("index.number_of_replicas", 2)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertTrue("Should not promote when not disabling inferred mode", result.isEmpty());
    }

    public void testNoPromotionWhenEnablingInferredMode() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertTrue("Should not promote when enabling inferred mode", result.isEmpty());
    }

    public void testNoPromotionWhenIndexNotCurrentlyEnabled() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertTrue("Should not promote when index was not using inferred mode", result.isEmpty());
    }

    public void testPromotionWhenDisablingWithDefaultPromoteOnDisable() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertEquals("Should promote since promote_on_disable defaults to true", List.of("test-index"), result);
    }

    public void testNoPromotionWhenPromoteOnDisableIsFalseOnIndex() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.getKey(), false)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertTrue("Should not promote when index has promote_on_disable=false", result.isEmpty());
    }

    public void testPromoteOnDisableOverrideInRequest() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.getKey(), true)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.getKey(), false)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertTrue("Request-level promote_on_disable=false should override index setting", result.isEmpty());
    }

    public void testPromoteOnDisableOverrideInRequestTrue() {
        IndexMetadata index = buildIndexMetadata("test-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.getKey(), false)
            .build());
        ClusterState state = clusterStateWith(index);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.getKey(), true)
            .build();

        List<String> result = action.getIndicesRequiringPromotion(newSettings, new Index[] { index.getIndex() }, state);
        assertEquals("Request-level promote_on_disable=true should override index setting", List.of("test-index"), result);
    }

    public void testMultipleIndicesMixedPromotionDecisions() {
        IndexMetadata enabledIndex = buildIndexMetadata("enabled-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .build());
        IndexMetadata disabledIndex = buildIndexMetadata("disabled-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build());
        IndexMetadata noPromoteIndex = buildIndexMetadata("no-promote-index", Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_PROMOTE_ON_DISABLE.getKey(), false)
            .build());
        ClusterState state = clusterStateWith(enabledIndex, disabledIndex, noPromoteIndex);

        Settings newSettings = Settings.builder()
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), false)
            .build();

        Index[] allIndices = new Index[] {
            enabledIndex.getIndex(),
            disabledIndex.getIndex(),
            noPromoteIndex.getIndex()
        };

        List<String> result = action.getIndicesRequiringPromotion(newSettings, allIndices, state);
        assertEquals("Only the index that was enabled with promote=true should be promoted", 1, result.size());
        assertEquals("enabled-index", result.get(0));
    }
}
