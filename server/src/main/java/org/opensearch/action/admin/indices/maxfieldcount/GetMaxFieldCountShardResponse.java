/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.action.support.broadcast.BroadcastShardResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.index.shard.ShardId;

import java.io.IOException;
import java.util.Objects;

/**
 * Shard-level response containing the raw Lucene field count for this shard.
 *
 * @opensearch.internal
 */
public class GetMaxFieldCountShardResponse extends BroadcastShardResponse {

    private final int fieldCount;

    public GetMaxFieldCountShardResponse(StreamInput in) throws IOException {
        super(in);
        fieldCount = in.readVInt();
    }

    public GetMaxFieldCountShardResponse(ShardId shardId, int fieldCount) {
        super(shardId);
        this.fieldCount = fieldCount;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetMaxFieldCountShardResponse that = (GetMaxFieldCountShardResponse) o;
        return fieldCount == that.fieldCount && Objects.equals(getShardId(), that.getShardId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getShardId(), fieldCount);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(fieldCount);
    }
}
