/*
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Service-Provider Interface (SPI) for the transport-grpc plugin.
 * <p>
 * Plugins implement {@link org.opensearch.plugin.transport.grpc.spi.SearchGrpcListener}
 * or {@link org.opensearch.plugin.transport.grpc.spi.DocumentGrpcListener}
 * and register via {@link java.util.ServiceLoader} to receive gRPC metrics.
 * </p>
 *
 * @since 3.0.0 [or appropriate version]
 */
package org.opensearch.plugin.transport.grpc.spi;
