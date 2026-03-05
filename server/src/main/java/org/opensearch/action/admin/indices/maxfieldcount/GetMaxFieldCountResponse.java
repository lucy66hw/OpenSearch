/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.action.support.broadcast.BroadcastResponse;
import org.opensearch.core.action.support.DefaultShardOperationFailedException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response containing the maximum Lucene field count per index (max across shards),
 * and the shard number that has that max.
 *
 * @opensearch.internal
 */
public class GetMaxFieldCountResponse extends BroadcastResponse {

    private final Map<String, Integer> maxFieldCountByIndex;
    private final Map<String, Integer> shardNumberOfMaxByIndex;

    public GetMaxFieldCountResponse(StreamInput in) throws IOException {
        super(in);
        int n = in.readVInt();
        Map<String, Integer> maxMap = new HashMap<>();
        Map<String, Integer> shardMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String index = in.readString();
            maxMap.put(index, in.readVInt());
            shardMap.put(index, in.readVInt());
        }
        maxFieldCountByIndex = Collections.unmodifiableMap(maxMap);
        shardNumberOfMaxByIndex = Collections.unmodifiableMap(shardMap);
    }

    public GetMaxFieldCountResponse(
        int totalShards,
        int successfulShards,
        int failedShards,
        List<DefaultShardOperationFailedException> shardFailures,
        Map<String, Integer> maxFieldCountByIndex,
        Map<String, Integer> shardNumberOfMaxByIndex
    ) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.maxFieldCountByIndex = maxFieldCountByIndex != null
            ? Collections.unmodifiableMap(maxFieldCountByIndex)
            : Collections.emptyMap();
        this.shardNumberOfMaxByIndex = shardNumberOfMaxByIndex != null
            ? Collections.unmodifiableMap(shardNumberOfMaxByIndex)
            : Collections.emptyMap();
    }

    public Map<String, Integer> getMaxFieldCountByIndex() {
        return maxFieldCountByIndex;
    }

    /** Shard number (per index) that has the max field count. */
    public Map<String, Integer> getShardNumberOfMaxByIndex() {
        return shardNumberOfMaxByIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetMaxFieldCountResponse that = (GetMaxFieldCountResponse) o;
        return getTotalShards() == that.getTotalShards()
            && getSuccessfulShards() == that.getSuccessfulShards()
            && getFailedShards() == that.getFailedShards()
            && Arrays.equals(getShardFailures(), that.getShardFailures())
            && Objects.equals(maxFieldCountByIndex, that.maxFieldCountByIndex)
            && Objects.equals(shardNumberOfMaxByIndex, that.shardNumberOfMaxByIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getTotalShards(),
            getSuccessfulShards(),
            getFailedShards(),
            Arrays.hashCode(getShardFailures()),
            maxFieldCountByIndex,
            shardNumberOfMaxByIndex
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(maxFieldCountByIndex.size());
        for (Map.Entry<String, Integer> e : maxFieldCountByIndex.entrySet()) {
            out.writeString(e.getKey());
            out.writeVInt(e.getValue());
            out.writeVInt(shardNumberOfMaxByIndex.getOrDefault(e.getKey(), 0));
        }
    }

    @Override
    protected void addCustomXContentFields(XContentBuilder builder, Params params) throws IOException {
        builder.startObject("indices");
        for (Map.Entry<String, Integer> e : maxFieldCountByIndex.entrySet()) {
            builder.startObject(e.getKey());
            builder.field("max_field_count", e.getValue());
            builder.field("shard", shardNumberOfMaxByIndex.getOrDefault(e.getKey(), 0));
            builder.endObject();
        }
        builder.endObject();
    }
}
