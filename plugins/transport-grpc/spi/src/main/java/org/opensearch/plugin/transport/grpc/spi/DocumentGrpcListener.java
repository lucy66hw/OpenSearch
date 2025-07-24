/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.spi;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.util.List;

/**
 * Listener that receives callbacks for every gRPC <em>Bulk/Document</em> invocation.
 */
public interface DocumentGrpcListener {
    /**
     * Invoked when the bulk request is received.
     *
     * @param request protobuf {@code BulkRequest}
     */
    void onBulkRequest(org.opensearch.protobufs.BulkRequest request);

    /**
     * Invoked on successful completion.
     *
     * @param request       original request
     * @param response      response returned to the client
     * @param tookInNanos the number of nanoseconds the bulk request took
     */
    void onBulkResponse(org.opensearch.protobufs.BulkRequest request, org.opensearch.protobufs.BulkResponse response, long tookInNanos);

    /**
     * Invoked on failure.
     *
     * @param request       original request
     * @param error         thrown exception
     */
    void onBulkError(org.opensearch.protobufs.BulkRequest request, Throwable error);

    /** Composite delegator for multiple listeners. */
    final class CompositeListener implements DocumentGrpcListener {

        private final List<DocumentGrpcListener> listeners;
        private final Logger logger;

        /**
         * Creates a composite listener that forwards search-related callbacks to all
         * supplied listeners, logging any exception a delegate throws.
         *
         * @param listeners the delegate listeners
         * @param logger    the logger used to report delegate failures
         */
        public CompositeListener(List<DocumentGrpcListener> listeners, Logger logger) {
            this.listeners = listeners;
            this.logger = logger;
        }

        @Override
        public void onBulkRequest(org.opensearch.protobufs.BulkRequest request) {
            for (DocumentGrpcListener listener : listeners) {
                try {
                    listener.onBulkRequest(request);
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("onBulkRequest listener [{}] failed", listener), e);
                }
            }
        }

        @Override
        public void onBulkResponse(
            org.opensearch.protobufs.BulkRequest request,
            org.opensearch.protobufs.BulkResponse response,
            long tookInNanos
        ) {
            for (DocumentGrpcListener listener : listeners) {
                try {
                    listener.onBulkResponse(request, response, tookInNanos);
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("onBulkResponse listener [{}] failed", listener), e);
                }
            }
        }

        @Override
        public void onBulkError(org.opensearch.protobufs.BulkRequest request, Throwable error) {
            for (DocumentGrpcListener listener : listeners) {
                try {
                    listener.onBulkError(request, error);
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("onBulkError listener [{}] failed", listener), e);
                }
            }
        }
    }
}
