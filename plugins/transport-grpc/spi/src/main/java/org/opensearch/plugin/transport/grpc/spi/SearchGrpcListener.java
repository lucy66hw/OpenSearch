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
 * Listener that receives a callback for every gRPC <em>Search</em> invocation handled
 * by the {@code transport-grpc} plugin.
 */
public interface SearchGrpcListener {
    /**
     * Called as soon as the request is decoded.
     *
     * @param request protobuf {@code SearchRequest}
     */
    void onRequest(org.opensearch.protobufs.SearchRequest request);

    /**
     * Called when the request completes successfully.
     *
     * @param request       original request
     * @param response      response returned to the client
     * @param tookInNanos the number of nanoseconds the proto request took
     */
    void onResponse(org.opensearch.protobufs.SearchRequest request, org.opensearch.protobufs.SearchResponse response, long tookInNanos);

    /**
     * Called when the search fails.
     *
     * @param request       original request
     * @param error         thrown exception
     */
    void onError(org.opensearch.protobufs.SearchRequest request, Throwable error);

    /**
     * Multiplexes callback invocations to a collection of listeners.
     */
    final class CompositeListener implements SearchGrpcListener {
        private final List<SearchGrpcListener> listeners;
        private final Logger logger;

        /**
         * Creates a composite listener that forwards search-related callbacks to all
         * supplied listeners, logging any exception a delegate throws.
         *
         * @param listeners the delegate listeners
         * @param logger    the logger used to report delegate failures
         */
        public CompositeListener(List<SearchGrpcListener> listeners, Logger logger) {
            this.listeners = listeners;
            this.logger = logger;
        }

        @Override
        public void onRequest(org.opensearch.protobufs.SearchRequest request) {
            for (SearchGrpcListener listener : listeners) {
                try {
                    listener.onRequest(request);
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("onRequest listener [{}] failed", listener), e);
                }
            }
        }

        @Override
        public void onResponse(
            org.opensearch.protobufs.SearchRequest request,
            org.opensearch.protobufs.SearchResponse response,
            long tookInNanos
        ) {
            for (SearchGrpcListener listener : listeners) {
                try {
                    listener.onResponse(request, response, tookInNanos);
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("onResponse listener [{}] failed", listener), e);
                }
            }
        }

        @Override
        public void onError(org.opensearch.protobufs.SearchRequest request, Throwable error) {
            for (SearchGrpcListener listener : listeners) {
                try {
                    listener.onError(request, error);
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("onError listener [{}] failed", listener), e);
                }
            }
        }
    }
}
