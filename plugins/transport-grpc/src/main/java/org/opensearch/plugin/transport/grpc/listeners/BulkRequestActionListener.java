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
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.plugin.transport.grpc.proto.response.document.bulk.BulkResponseProtoUtils;
import org.opensearch.plugin.transport.grpc.spi.DocumentGrpcListener;

import java.io.IOException;

import io.grpc.stub.StreamObserver;

/**
 * Listener for bulk request execution completion, handling successful and failure scenarios.
 */
public class BulkRequestActionListener implements ActionListener<BulkResponse> {
    private static final Logger logger = LogManager.getLogger(BulkRequestActionListener.class);
    private final StreamObserver<org.opensearch.protobufs.BulkResponse> responseObserver;
    private final org.opensearch.protobufs.BulkRequest request;
    private final DocumentGrpcListener documentGrpcListener;
    private final long startTimeNanos;

    /**
     * Creates a new BulkRequestActionListener.
     *
     * @param responseObserver The gRPC stream observer to send the response back to the client
     * @param request  original protobuf {@link org.opensearch.protobufs.BulkRequest}
     * @param documentGrpcListener  composite {@link DocumentGrpcListener} that receives metrics callbacks
     */
    public BulkRequestActionListener(
        StreamObserver<org.opensearch.protobufs.BulkResponse> responseObserver,
        org.opensearch.protobufs.BulkRequest request,
        DocumentGrpcListener documentGrpcListener
    ) {
        super();
        this.responseObserver = responseObserver;
        this.request = request;
        this.documentGrpcListener = documentGrpcListener;
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Handles successful bulk request execution.
     * Converts the OpenSearch internal response to protobuf format and sends it to the client.
     *
     * @param response The bulk response from OpenSearch
     */
    @Override
    public void onResponse(org.opensearch.action.bulk.BulkResponse response) {
        // Bulk execution succeeded. Convert the opensearch internal response to protobuf
        try {
            org.opensearch.protobufs.BulkResponse protoResponse = BulkResponseProtoUtils.toProto(response);
            final long latencyNanos = (System.nanoTime() - startTimeNanos);
            documentGrpcListener.onBulkResponse(request, protoResponse, latencyNanos);
            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();
        } catch (RuntimeException | IOException e) {
            responseObserver.onError(e);
        }
    }

    /**
     * Handles bulk request execution failures.
     * Converts the exception to an appropriate gRPC error and sends it to the client.
     *
     * @param e The exception that occurred during execution
     */
    @Override
    public void onFailure(Exception e) {
        logger.error("BulkRequestActionListener failed to process bulk request:" + e.getMessage());
        documentGrpcListener.onBulkError(request, e);
        responseObserver.onError(e);
    }
}
