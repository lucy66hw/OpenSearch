/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.support.broadcast.BroadcastRequest;
import org.opensearch.core.common.io.stream.StreamInput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Request to collect inferred field names from all shards of given indices.
 *
 * @opensearch.internal
 */
public class GetInferredFieldsRequest extends BroadcastRequest<GetInferredFieldsRequest> {

    public GetInferredFieldsRequest(String... indices) {
        super(indices);
    }

    public GetInferredFieldsRequest(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetInferredFieldsRequest that = (GetInferredFieldsRequest) o;
        return Arrays.equals(indices(), that.indices()) && Objects.equals(indicesOptions(), that.indicesOptions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(indices()), indicesOptions());
    }
}
