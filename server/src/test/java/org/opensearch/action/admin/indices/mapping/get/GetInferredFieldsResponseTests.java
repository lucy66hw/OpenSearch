/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache License, Version 2.0
 * or a compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.core.action.support.DefaultShardOperationFailedException;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetInferredFieldsResponseTests extends AbstractWireSerializingTestCase<GetInferredFieldsResponse> {

    @Override
    protected GetInferredFieldsResponse createTestInstance() {
        int totalShards = randomIntBetween(1, 10);
        int successfulShards = randomIntBetween(0, totalShards);
        int failedShards = totalShards - successfulShards;
        List<DefaultShardOperationFailedException> failures = Collections.emptyList();
        Map<String, Set<String>> byIndex = new HashMap<>();
        int numIndices = randomIntBetween(0, 5);
        for (int i = 0; i < numIndices; i++) {
            String indexName = "index-" + randomAlphaOfLength(3);
            Set<String> fields = IntStream.range(0, randomIntBetween(0, 5))
                .mapToObj(n -> "field_" + randomAlphaOfLength(5))
                .collect(Collectors.toSet());
            byIndex.put(indexName, Set.copyOf(fields));
        }
        return new GetInferredFieldsResponse(totalShards, successfulShards, failedShards, failures, byIndex);
    }

    @Override
    protected Writeable.Reader<GetInferredFieldsResponse> instanceReader() {
        return GetInferredFieldsResponse::new;
    }

    @Override
    protected GetInferredFieldsResponse mutateInstance(GetInferredFieldsResponse instance) throws IOException {
        Map<String, Set<String>> byIndex = new HashMap<>(instance.getInferredFieldsByIndex());
        if (randomBoolean() && !byIndex.isEmpty()) {
            String key = randomFrom(byIndex.keySet());
            Set<String> fields = new HashSet<>(byIndex.get(key));
            fields.add("mutated_" + randomAlphaOfLength(3));
            byIndex.put(key, Set.copyOf(fields));
        } else {
            byIndex.put("new_index_" + randomAlphaOfLength(3), Set.of("a", "b"));
        }
        return new GetInferredFieldsResponse(
            instance.getTotalShards(),
            instance.getSuccessfulShards(),
            instance.getFailedShards(),
            instance.getShardFailures() != null ? Arrays.asList(instance.getShardFailures()) : List.of(),
            byIndex
        );
    }

    public void testGetInferredFieldsByIndex() {
        Map<String, Set<String>> byIndex = Map.of("idx1", Set.of("f1", "f2"), "idx2", Set.of("f3"));
        GetInferredFieldsResponse response = new GetInferredFieldsResponse(2, 2, 0, List.of(), byIndex);
        assertEquals(byIndex, response.getInferredFieldsByIndex());
        assertTrue(response.getInferredFieldsByIndex().get("idx1").contains("f1"));
    }

    public void testEmptyInferredFieldsByIndex() {
        GetInferredFieldsResponse response = new GetInferredFieldsResponse(1, 1, 0, List.of(), null);
        assertTrue(response.getInferredFieldsByIndex().isEmpty());
    }
}
