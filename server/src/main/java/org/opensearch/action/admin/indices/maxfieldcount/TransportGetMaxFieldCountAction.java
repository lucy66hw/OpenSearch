/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
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
import org.opensearch.index.shard.IndexShard;
import org.opensearch.indices.IndicesService;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Transport action that broadcasts to primary shards to get raw Lucene field count per shard,
 * then merges by taking the max per index.
 *
 * @opensearch.internal
 */
public class TransportGetMaxFieldCountAction extends TransportBroadcastAction<
    GetMaxFieldCountRequest,
    GetMaxFieldCountResponse,
    GetMaxFieldCountShardRequest,
    GetMaxFieldCountShardResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportGetMaxFieldCountAction(
        TransportService transportService,
        ClusterService clusterService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        IndicesService indicesService
    ) {
        super(
            GetMaxFieldCountAction.NAME,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            GetMaxFieldCountRequest::new,
            GetMaxFieldCountShardRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.indicesService = indicesService;
    }

    @Override
    protected GroupShardsIterator<ShardIterator> shards(
        ClusterState clusterState,
        GetMaxFieldCountRequest request,
        String[] concreteIndices
    ) {
        return clusterState.getRoutingTable().activePrimaryShardsGrouped(concreteIndices, false);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, GetMaxFieldCountRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(
        ClusterState state,
        GetMaxFieldCountRequest request,
        String[] concreteIndices
    ) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.READ, concreteIndices);
    }

    @Override
    protected GetMaxFieldCountShardRequest newShardRequest(
        int numShards,
        ShardRouting shard,
        GetMaxFieldCountRequest request
    ) {
        return new GetMaxFieldCountShardRequest(shard.shardId(), request);
    }

    @Override
    protected GetMaxFieldCountShardResponse readShardResponse(StreamInput in) throws IOException {
        return new GetMaxFieldCountShardResponse(in);
    }

    @Override
    protected GetMaxFieldCountShardResponse shardOperation(GetMaxFieldCountShardRequest request, Task task) throws IOException {
        ShardId shardId = request.shardId();
        IndexService indexService = indicesService.indexServiceSafe(shardId.getIndex());
        IndexShard indexShard = indexService.getShardOrNull(shardId.id());
        if (indexShard == null) {
            return new GetMaxFieldCountShardResponse(shardId, 0);
        }

        int fieldCount;
        try (Engine.Searcher searcher = indexShard.acquireSearcher("get_max_field_count")) {
            IndexReader reader = searcher.getIndexReader();
            FieldInfos fieldInfos = FieldInfos.getMergedFieldInfos(reader);
            fieldCount = fieldInfos.size();
        }

        return new GetMaxFieldCountShardResponse(shardId, fieldCount);
    }

    @Override
    protected GetMaxFieldCountResponse newResponse(
        GetMaxFieldCountRequest request,
        AtomicReferenceArray shardsResponses,
        ClusterState clusterState
    ) {
        Map<String, Integer> maxByIndex = new HashMap<>();
        Map<String, Integer> shardNumberOfMaxByIndex = new HashMap<>();
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
                GetMaxFieldCountShardResponse shardResp = (GetMaxFieldCountShardResponse) r;
                successfulShards++;
                String indexName = shardResp.getIndex();
                int count = shardResp.getFieldCount();
                int shardId = shardResp.getShardId().getId();
                if (!maxByIndex.containsKey(indexName) || count > maxByIndex.get(indexName)) {
                    maxByIndex.put(indexName, count);
                    shardNumberOfMaxByIndex.put(indexName, shardId);
                }
            }
        }

        return new GetMaxFieldCountResponse(
            totalShards,
            successfulShards,
            failedShards,
            failures,
            Collections.unmodifiableMap(maxByIndex),
            Collections.unmodifiableMap(shardNumberOfMaxByIndex)
        );
    }
}
