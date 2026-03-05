/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.core.action.support.DefaultShardOperationFailedException;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetMaxFieldCountResponseTests extends AbstractWireSerializingTestCase<GetMaxFieldCountResponse> {

    @Override
    protected GetMaxFieldCountResponse createTestInstance() {
        int totalShards = randomIntBetween(1, 10);
        int successfulShards = randomIntBetween(0, totalShards);
        int failedShards = totalShards - successfulShards;
        List<DefaultShardOperationFailedException> failures = Collections.emptyList();
        Map<String, Integer> maxByIndex = new HashMap<>();
        Map<String, Integer> shardByIndex = new HashMap<>();
        int numIndices = randomIntBetween(0, 5);
        for (int i = 0; i < numIndices; i++) {
            String indexName = "index-" + randomAlphaOfLength(3);
            maxByIndex.put(indexName, randomIntBetween(0, 10000));
            shardByIndex.put(indexName, randomIntBetween(0, 10));
        }
        return new GetMaxFieldCountResponse(totalShards, successfulShards, failedShards, failures, maxByIndex, shardByIndex);
    }

    @Override
    protected Writeable.Reader<GetMaxFieldCountResponse> instanceReader() {
        return GetMaxFieldCountResponse::new;
    }

    @Override
    protected GetMaxFieldCountResponse mutateInstance(GetMaxFieldCountResponse instance) throws IOException {
        Map<String, Integer> maxByIndex = new HashMap<>(instance.getMaxFieldCountByIndex());
        Map<String, Integer> shardByIndex = new HashMap<>(instance.getShardNumberOfMaxByIndex());
        if (randomBoolean() && !maxByIndex.isEmpty()) {
            String key = randomFrom(maxByIndex.keySet());
            maxByIndex.put(key, randomValueOtherThan(maxByIndex.get(key), () -> randomIntBetween(0, 10000)));
            shardByIndex.put(key, randomIntBetween(0, 10));
        } else {
            String newIndex = "new_index_" + randomAlphaOfLength(3);
            maxByIndex.put(newIndex, randomIntBetween(0, 1000));
            shardByIndex.put(newIndex, 0);
        }
        return new GetMaxFieldCountResponse(
            instance.getTotalShards(),
            instance.getSuccessfulShards(),
            instance.getFailedShards(),
            instance.getShardFailures() != null ? Arrays.asList(instance.getShardFailures()) : List.of(),
            maxByIndex,
            shardByIndex
        );
    }

    public void testGetMaxFieldCountAndShardByIndex() {
        Map<String, Integer> maxByIndex = new HashMap<>();
        maxByIndex.put("idx1", 100);
        maxByIndex.put("idx2", 200);
        Map<String, Integer> shardByIndex = new HashMap<>();
        shardByIndex.put("idx1", 0);
        shardByIndex.put("idx2", 1);
        GetMaxFieldCountResponse response = new GetMaxFieldCountResponse(2, 2, 0, List.of(), maxByIndex, shardByIndex);
        assertEquals(maxByIndex, response.getMaxFieldCountByIndex());
        assertEquals(shardByIndex, response.getShardNumberOfMaxByIndex());
        assertEquals(100, response.getMaxFieldCountByIndex().get("idx1").intValue());
        assertEquals(0, response.getShardNumberOfMaxByIndex().get("idx1").intValue());
    }

    public void testEmptyMaps() {
        GetMaxFieldCountResponse response = new GetMaxFieldCountResponse(1, 1, 0, List.of(), null, null);
        assertTrue(response.getMaxFieldCountByIndex().isEmpty());
        assertTrue(response.getShardNumberOfMaxByIndex().isEmpty());
    }
}
