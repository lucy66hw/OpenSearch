/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;

public class GetMaxFieldCountShardResponseTests extends AbstractWireSerializingTestCase<GetMaxFieldCountShardResponse> {

    @Override
    protected GetMaxFieldCountShardResponse createTestInstance() {
        ShardId shardId = new ShardId(randomAlphaOfLengthBetween(3, 10), randomAlphaOfLength(5), randomIntBetween(0, 5));
        int fieldCount = randomIntBetween(0, 10000);
        return new GetMaxFieldCountShardResponse(shardId, fieldCount);
    }

    @Override
    protected Writeable.Reader<GetMaxFieldCountShardResponse> instanceReader() {
        return GetMaxFieldCountShardResponse::new;
    }

    @Override
    protected GetMaxFieldCountShardResponse mutateInstance(GetMaxFieldCountShardResponse instance) throws IOException {
        return new GetMaxFieldCountShardResponse(
            instance.getShardId(),
            randomValueOtherThan(instance.getFieldCount(), () -> randomIntBetween(0, 10000))
        );
    }

    public void testGetFieldCountAndShardId() {
        ShardId shardId = new ShardId("idx", "_na_", 1);
        GetMaxFieldCountShardResponse response = new GetMaxFieldCountShardResponse(shardId, 42);
        assertEquals(42, response.getFieldCount());
        assertEquals(shardId, response.getShardId());
        assertEquals(1, response.getShardId().getId());
    }

    public void testZeroFieldCount() {
        ShardId shardId = new ShardId("idx", "_na_", 0);
        GetMaxFieldCountShardResponse response = new GetMaxFieldCountShardResponse(shardId, 0);
        assertEquals(0, response.getFieldCount());
    }
}
