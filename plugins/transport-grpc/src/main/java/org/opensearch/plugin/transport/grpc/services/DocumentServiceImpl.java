/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.Client;
import org.opensearch.plugin.transport.grpc.listeners.BulkRequestActionListener;
import org.opensearch.plugin.transport.grpc.proto.request.document.bulk.BulkRequestProtoUtils;
import org.opensearch.plugin.transport.grpc.spi.DocumentGrpcListener;
import org.opensearch.protobufs.services.DocumentServiceGrpc;

import io.grpc.stub.StreamObserver;

/**
 * Implementation of the gRPC Document Service.
 */
public class DocumentServiceImpl extends DocumentServiceGrpc.DocumentServiceImplBase {
    private static final Logger logger = LogManager.getLogger(DocumentServiceImpl.class);
    private final Client client;
    private final DocumentGrpcListener documentGrpcListener;

    /**
     * Creates a new DocumentServiceImpl.
     *
     * @param client Client for executing actions on the local node
     * @param listener composite {@link DocumentGrpcListener} for metrics callbacks
     */
    public DocumentServiceImpl(Client client, DocumentGrpcListener listener) {
        this.client = client;
        this.documentGrpcListener = listener;
    }

    /**
     * Processes a bulk request.
     *
     * @param request The bulk request to process
     * @param responseObserver The observer to send the response back to the client
     */
    @Override
    public void bulk(org.opensearch.protobufs.BulkRequest request, StreamObserver<org.opensearch.protobufs.BulkResponse> responseObserver) {
        try {
            documentGrpcListener.onBulkRequest(request);
            org.opensearch.action.bulk.BulkRequest bulkRequest = BulkRequestProtoUtils.prepareRequest(request);
            BulkRequestActionListener listener = new BulkRequestActionListener(responseObserver, request, documentGrpcListener);
            client.bulk(bulkRequest, listener);
        } catch (RuntimeException e) {
            logger.error("DocumentServiceImpl failed to process bulk request, request=" + request + ", error=" + e.getMessage());
            responseObserver.onError(e);
        }
    }
}
