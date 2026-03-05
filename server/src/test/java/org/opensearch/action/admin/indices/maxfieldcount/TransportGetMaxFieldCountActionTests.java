/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.indices.IndicesService;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.TransportService;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransportGetMaxFieldCountAction}, especially the merge logic in newResponse.
 */
public class TransportGetMaxFieldCountActionTests extends OpenSearchTestCase {

    public void testNewResponseMergesToMaxAndRecordsShardNumber() {
        TransportGetMaxFieldCountAction action = createAction();
        GetMaxFieldCountRequest request = new GetMaxFieldCountRequest("idx");
        AtomicReferenceArray<Object> shardsResponses = new AtomicReferenceArray<>(3);
        // Index "idx": shard 0 has 10, shard 1 has 20, shard 2 has 15 -> max 20, shard 1
        shardsResponses.set(0, new GetMaxFieldCountShardResponse(new ShardId("idx", "_na_", 0), 10));
        shardsResponses.set(1, new GetMaxFieldCountShardResponse(new ShardId("idx", "_na_", 1), 20));
        shardsResponses.set(2, new GetMaxFieldCountShardResponse(new ShardId("idx", "_na_", 2), 15));

        GetMaxFieldCountResponse response = action.newResponse(request, shardsResponses, ClusterState.EMPTY_STATE);

        assertEquals(1, response.getMaxFieldCountByIndex().size());
        assertEquals(20, response.getMaxFieldCountByIndex().get("idx").intValue());
        assertEquals(1, response.getShardNumberOfMaxByIndex().get("idx").intValue());
    }

    public void testNewResponseMultipleIndices() {
        TransportGetMaxFieldCountAction action = createAction();
        GetMaxFieldCountRequest request = new GetMaxFieldCountRequest("idx1", "idx2");
        AtomicReferenceArray<Object> shardsResponses = new AtomicReferenceArray<>(4);
        shardsResponses.set(0, new GetMaxFieldCountShardResponse(new ShardId("idx1", "_na_", 0), 100));
        shardsResponses.set(1, new GetMaxFieldCountShardResponse(new ShardId("idx2", "_na_", 0), 50));
        shardsResponses.set(2, new GetMaxFieldCountShardResponse(new ShardId("idx2", "_na_", 1), 60));

        GetMaxFieldCountResponse response = action.newResponse(request, shardsResponses, ClusterState.EMPTY_STATE);

        assertEquals(2, response.getMaxFieldCountByIndex().size());
        assertEquals(100, response.getMaxFieldCountByIndex().get("idx1").intValue());
        assertEquals(0, response.getShardNumberOfMaxByIndex().get("idx1").intValue());
        assertEquals(60, response.getMaxFieldCountByIndex().get("idx2").intValue());
        assertEquals(1, response.getShardNumberOfMaxByIndex().get("idx2").intValue());
    }

    public void testNewResponseSkipsNullAndFailures() {
        TransportGetMaxFieldCountAction action = createAction();
        GetMaxFieldCountRequest request = new GetMaxFieldCountRequest("idx");
        AtomicReferenceArray<Object> shardsResponses = new AtomicReferenceArray<>(3);
        shardsResponses.set(0, null);
        shardsResponses.set(1, new GetMaxFieldCountShardResponse(new ShardId("idx", "_na_", 1), 5));
        shardsResponses.set(2, new org.opensearch.action.support.broadcast.BroadcastShardOperationFailedException(
            new ShardId("idx", "_na_", 2), "simulated failure")
        );

        GetMaxFieldCountResponse response = action.newResponse(request, shardsResponses, ClusterState.EMPTY_STATE);

        assertEquals(1, response.getMaxFieldCountByIndex().size());
        assertEquals(5, response.getMaxFieldCountByIndex().get("idx").intValue());
        assertEquals(1, response.getShardNumberOfMaxByIndex().get("idx").intValue());
        assertEquals(1, response.getFailedShards());
    }

    private static TransportGetMaxFieldCountAction createAction() {
        return new TransportGetMaxFieldCountAction(
            mock(TransportService.class),
            mock(ClusterService.class),
            new ActionFilters(Collections.emptySet()),
            new IndexNameExpressionResolver(new org.opensearch.common.util.concurrent.ThreadContext(org.opensearch.common.settings.Settings.EMPTY)),
            mock(IndicesService.class)
        );
    }
}
