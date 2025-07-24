/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.listeners;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.action.search.ShardSearchFailure;
import org.opensearch.plugin.transport.grpc.spi.SearchGrpcListener;
import org.opensearch.search.SearchHits;
import org.opensearch.test.OpenSearchTestCase;

import io.grpc.stub.StreamObserver;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SearchRequestActionListenerTests extends OpenSearchTestCase {

    @Mock
    private StreamObserver<org.opensearch.protobufs.SearchResponse> responseObserver;
    @Mock
    private SearchGrpcListener searchGrpcListener;

    private SearchRequestActionListener listener;
    private org.opensearch.protobufs.SearchRequest protoRequest;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        protoRequest = org.opensearch.protobufs.SearchRequest.getDefaultInstance();
        listener = new SearchRequestActionListener(responseObserver, protoRequest, searchGrpcListener);
    }

    public void testOnResponse() {
        // Create a SearchResponse
        SearchResponse mockSearchResponse = new SearchResponse(
            new SearchResponseSections(SearchHits.empty(), null, null, false, false, null, 1),
            randomAlphaOfLengthBetween(5, 10),
            5,
            5,
            0,
            100,
            ShardSearchFailure.EMPTY_ARRAY,
            SearchResponse.Clusters.EMPTY
        );

        // Call the method under test
        listener.onResponse(mockSearchResponse);

        // Verify that onNext and onCompleted were called
        verify(responseObserver, times(1)).onNext(any(org.opensearch.protobufs.SearchResponse.class));
        verify(responseObserver, times(1)).onCompleted();
        verify(searchGrpcListener, times(1)).onResponse(eq(protoRequest), any(org.opensearch.protobufs.SearchResponse.class), anyLong());
    }

    public void testOnFailure() {
        // Create a mock StreamObserver
        @SuppressWarnings("unchecked")
        StreamObserver<org.opensearch.protobufs.SearchResponse> mockResponseObserver = mock(StreamObserver.class);

        // Create a SearchRequestActionListener
        SearchRequestActionListener listener = new SearchRequestActionListener(mockResponseObserver, protoRequest, searchGrpcListener);

        // Create an exception
        Exception exception = new Exception("Test exception");

        // Call onFailure
        listener.onFailure(exception);

        // Verify that onError was called with the exception
        verify(mockResponseObserver, times(1)).onError(exception);
        verify(searchGrpcListener).onError(eq(protoRequest), eq(exception));
    }
}
