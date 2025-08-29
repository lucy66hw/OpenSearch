/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.transport.grpc.listeners;

import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.replication.ReplicationResponse;
import org.opensearch.core.index.Index;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

import io.grpc.stub.StreamObserver;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class BulkRequestActionListenerTests extends OpenSearchTestCase {

    @Mock
    private StreamObserver<org.opensearch.protobufs.BulkResponse> responseObserver;
    @Mock
    private BulkRequestActionListener listener;
    private org.opensearch.protobufs.BulkRequest protoRequest;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        protoRequest = org.opensearch.protobufs.BulkRequest.getDefaultInstance();
        listener = new BulkRequestActionListener(responseObserver);
    }

    public void testOnResponseWithSuccessfulResponse() {
        // Create a successful BulkResponse
        BulkItemResponse[] responses = new BulkItemResponse[1];
        Index index = new Index("test-index", "_na_");
        ShardId shardId = new ShardId(index, 1);
        IndexResponse indexResponse = new IndexResponse(shardId, "test-id", 1, 1, 1, true);
        ReplicationResponse.ShardInfo shardInfo = new ReplicationResponse.ShardInfo();
        indexResponse.setShardInfo(shardInfo);
        responses[0] = new BulkItemResponse(0, DocWriteRequest.OpType.INDEX, indexResponse);

        BulkResponse bulkResponse = new BulkResponse(responses, 100);

        // Call onResponse
        listener.onResponse(bulkResponse);

        // Verify that onNext and onCompleted were called
        verify(responseObserver).onNext(any(org.opensearch.protobufs.BulkResponse.class));
        verify(responseObserver).onCompleted();
    }

    public void testOnResponseWithException() {
        Exception exception = new IOException("Test exception");

        // Call onFailure
        listener.onFailure(exception);

        // Verify that onError was called
        verify(responseObserver).onError(any(Throwable.class));
    }
}
