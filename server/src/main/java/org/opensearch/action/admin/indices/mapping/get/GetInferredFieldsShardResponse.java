/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.support.broadcast.BroadcastShardResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.index.shard.ShardId;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Shard-level response containing inferred field names for this shard.
 *
 * @opensearch.internal
 */
public class GetInferredFieldsShardResponse extends BroadcastShardResponse {

    private final Set<String> inferredFieldNames;

    public GetInferredFieldsShardResponse(StreamInput in) throws IOException {
        super(in);
        inferredFieldNames = in.readSet(StreamInput::readString);
    }

    /**
     * @param inferredFieldNames must be immutable (e.g. {@link Set#copyOf(Collection)} or {@link Set#of()});
     *                           callers must not modify after passing.
     */
    public GetInferredFieldsShardResponse(ShardId shardId, Set<String> inferredFieldNames) {
        super(shardId);
        this.inferredFieldNames = inferredFieldNames;
    }

    public Set<String> getInferredFieldNames() {
        return inferredFieldNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetInferredFieldsShardResponse that = (GetInferredFieldsShardResponse) o;
        return Objects.equals(getShardId(), that.getShardId()) && Objects.equals(inferredFieldNames, that.inferredFieldNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getShardId(), inferredFieldNames);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeCollection(inferredFieldNames, StreamOutput::writeString);
    }
}
