/*
* SPDX-License-Identifier: Apache-2.0
*
* The OpenSearch Contributors require contributions made to
* this file be licensed under the Apache-2.0 license or a
* compatible open source license.
*/

package org.opensearch.plugin.transport.grpc.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.plugin.transport.grpc.proto.response.search.SearchResponseProtoUtils;
import org.opensearch.plugin.transport.grpc.spi.SearchGrpcListener;

import java.io.IOException;

import io.grpc.stub.StreamObserver;

/**
 * Listener for search request execution completion, handling successful and failure scenarios.
 */
public class SearchRequestActionListener implements ActionListener<SearchResponse> {
    private static final Logger logger = LogManager.getLogger(SearchRequestActionListener.class);

    private final StreamObserver<org.opensearch.protobufs.SearchResponse> responseObserver;
    private final org.opensearch.protobufs.SearchRequest request;
    private final SearchGrpcListener searchGrpcListener;
    private final long startTimeNanos;

    /**
     * Constructs a new SearchRequestActionListener.
     *
     * @param responseObserver the gRPC stream observer to send the search response to
     * @param request  original protobuf {@link org.opensearch.protobufs.SearchRequest}
     * @param searchGrpcListener  composite {@link SearchGrpcListener} that receives metrics callbacks
     */
    public SearchRequestActionListener(
        StreamObserver<org.opensearch.protobufs.SearchResponse> responseObserver,
        org.opensearch.protobufs.SearchRequest request,
        SearchGrpcListener searchGrpcListener
    ) {
        super();
        this.responseObserver = responseObserver;
        this.searchGrpcListener = searchGrpcListener;
        this.request = request;
        this.startTimeNanos = System.nanoTime();
    }

    @Override
    public void onResponse(SearchResponse response) {
        // Search execution succeeded. Convert the opensearch internal response to protobuf
        try {
            org.opensearch.protobufs.SearchResponse protoResponse = SearchResponseProtoUtils.toProto(response);

            final long latencyNanos = (System.nanoTime() - startTimeNanos);
            searchGrpcListener.onResponse(request, protoResponse, latencyNanos);
            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();
        } catch (RuntimeException | IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void onFailure(Exception e) {
        logger.error("SearchRequestActionListener failed to process search request:" + e.getMessage());
        try {
            searchGrpcListener.onError(request, e);
        } catch (Exception listenerException) {
            logger.error("Error in searchGrpcListener.onError", listenerException);
        } finally {
            responseObserver.onError(e);
        }
    }
}
