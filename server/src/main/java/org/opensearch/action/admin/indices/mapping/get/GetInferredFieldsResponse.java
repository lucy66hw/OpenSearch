/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.support.broadcast.BroadcastResponse;
import org.opensearch.core.action.support.DefaultShardOperationFailedException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Response containing merged inferred field names per index from all shards.
 *
 * @opensearch.internal
 */
public class GetInferredFieldsResponse extends BroadcastResponse {

    private final Map<String, Set<String>> inferredFieldsByIndex;

    public GetInferredFieldsResponse(StreamInput in) throws IOException {
        super(in);
        int n = in.readVInt();
        Map<String, Set<String>> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String index = in.readString();
            Set<String> fields = in.readSet(StreamInput::readString);
            map.put(index, fields);
        }
        inferredFieldsByIndex = Collections.unmodifiableMap(map);
    }

    public GetInferredFieldsResponse(
        int totalShards,
        int successfulShards,
        int failedShards,
        List<DefaultShardOperationFailedException> shardFailures,
        Map<String, Set<String>> inferredFieldsByIndex
    ) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.inferredFieldsByIndex = inferredFieldsByIndex != null
            ? Collections.unmodifiableMap(inferredFieldsByIndex)
            : Collections.emptyMap();
    }

    public Map<String, Set<String>> getInferredFieldsByIndex() {
        return inferredFieldsByIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetInferredFieldsResponse that = (GetInferredFieldsResponse) o;
        return getTotalShards() == that.getTotalShards()
            && getSuccessfulShards() == that.getSuccessfulShards()
            && getFailedShards() == that.getFailedShards()
            && Arrays.equals(getShardFailures(), that.getShardFailures())
            && Objects.equals(inferredFieldsByIndex, that.inferredFieldsByIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getTotalShards(),
            getSuccessfulShards(),
            getFailedShards(),
            Arrays.hashCode(getShardFailures()),
            inferredFieldsByIndex
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(inferredFieldsByIndex.size());
        for (Map.Entry<String, Set<String>> e : inferredFieldsByIndex.entrySet()) {
            out.writeString(e.getKey());
            out.writeCollection(e.getValue(), StreamOutput::writeString);
        }
    }
}
