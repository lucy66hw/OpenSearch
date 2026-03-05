/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.action.support.broadcast.BroadcastShardRequest;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.index.shard.ShardId;

import java.io.IOException;

/**
 * Shard-level request for get max field count.
 *
 * @opensearch.internal
 */
public class GetMaxFieldCountShardRequest extends BroadcastShardRequest {

    public GetMaxFieldCountShardRequest(StreamInput in) throws IOException {
        super(in);
    }

    public GetMaxFieldCountShardRequest(ShardId shardId, GetMaxFieldCountRequest request) {
        super(shardId, request);
    }
}
