/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.support.broadcast.BroadcastShardRequest;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.index.shard.ShardId;

import java.io.IOException;

/**
 * Shard-level request for get inferred fields.
 *
 * @opensearch.internal
 */
public class GetInferredFieldsShardRequest extends BroadcastShardRequest {

    public GetInferredFieldsShardRequest(StreamInput in) throws IOException {
        super(in);
    }

    public GetInferredFieldsShardRequest(ShardId shardId, GetInferredFieldsRequest request) {
        super(shardId, request);
    }
}
