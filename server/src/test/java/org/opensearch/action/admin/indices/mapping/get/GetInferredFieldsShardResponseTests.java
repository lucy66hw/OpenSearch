/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache License, Version 2.0
 * or a compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.core.index.shard.ShardId;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetInferredFieldsShardResponseTests extends AbstractWireSerializingTestCase<GetInferredFieldsShardResponse> {

    @Override
    protected GetInferredFieldsShardResponse createTestInstance() {
        ShardId shardId = new ShardId(randomAlphaOfLengthBetween(3, 10), randomAlphaOfLength(5), randomIntBetween(0, 5));
        Set<String> inferred = IntStream.range(0, randomIntBetween(0, 10))
            .mapToObj(i -> "field_" + randomAlphaOfLength(5))
            .collect(Collectors.toSet());
        return new GetInferredFieldsShardResponse(shardId, Set.copyOf(inferred));
    }

    @Override
    protected Writeable.Reader<GetInferredFieldsShardResponse> instanceReader() {
        return GetInferredFieldsShardResponse::new;
    }

    @Override
    protected GetInferredFieldsShardResponse mutateInstance(GetInferredFieldsShardResponse instance) throws IOException {
        ShardId shardId = instance.getShardId();
        Set<String> inferred = new HashSet<>(instance.getInferredFieldNames());
        inferred.add("mutated_" + randomAlphaOfLength(3));
        return new GetInferredFieldsShardResponse(shardId, Set.copyOf(inferred));
    }

    public void testGetInferredFieldNames() {
        ShardId shardId = new ShardId("idx", "_na_", 0);
        Set<String> fields = Set.of("a", "b", "c");
        GetInferredFieldsShardResponse response = new GetInferredFieldsShardResponse(shardId, fields);
        assertEquals(fields, response.getInferredFieldNames());
        assertEquals(shardId, response.getShardId());
    }

    public void testEmptyInferredFields() {
        ShardId shardId = new ShardId("idx", "_na_", 0);
        GetInferredFieldsShardResponse response = new GetInferredFieldsShardResponse(shardId, Set.of());
        assertTrue(response.getInferredFieldNames().isEmpty());
    }
}
