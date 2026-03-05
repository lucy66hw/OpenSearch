/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.master.info.ClusterInfoRequest;
import org.opensearch.common.annotation.PublicApi;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Transport request to get field mappings.
 *
 * @opensearch.api
 */
@PublicApi(since = "1.0.0")
public class GetMappingsRequest extends ClusterInfoRequest<GetMappingsRequest> {

    private boolean includeInferred = false;

    public GetMappingsRequest() {}

    public GetMappingsRequest(StreamInput in) throws IOException {
        super(in);
        this.includeInferred = in.readBoolean();
    }

    /** When true, include inferred (dynamic) field names in the response. Default is false. */
    public boolean includeInferred() {
        return includeInferred;
    }

    /** Set to true to include inferred field names in the mapping response. */
    public GetMappingsRequest includeInferred(boolean includeInferred) {
        this.includeInferred = includeInferred;
        return this;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(includeInferred);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetMappingsRequest that = (GetMappingsRequest) o;
        return includeInferred == that.includeInferred
            && Arrays.equals(indices(), that.indices())
            && Objects.equals(indicesOptions(), that.indicesOptions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(includeInferred, Arrays.hashCode(indices()), indicesOptions());
    }

}
