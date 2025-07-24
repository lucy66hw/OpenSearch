/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search.query;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.protobufs.QueryContainer;

/**
 * Converter for Bool queries.
 * This class implements the QueryBuilderProtoConverter interface to provide Bool query support
 * for the gRPC transport plugin.
 */
public class BoolQueryBuilderProtoConverter implements QueryBuilderProtoConverter {

    /**
     * Constructs a new BoolQueryBuilderProtoConverter.
     */
    public BoolQueryBuilderProtoConverter() {
        // Default constructor
    }

    @Override
    public QueryContainer.QueryContainerCase getHandledQueryCase() {
        return QueryContainer.QueryContainerCase.BOOL;
    }

    @Override
    public QueryBuilder fromProto(QueryContainer queryContainer) {
        if (queryContainer == null || !queryContainer.hasBool()) {
            throw new IllegalArgumentException("QueryContainer does not contain a Bool query");
        }

        return BoolQueryBuilderProtoUtils.fromProto(queryContainer.getBool());
    }
}
