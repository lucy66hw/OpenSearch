/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache License, Version 2.0
 * or a compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.Arrays;

public class GetInferredFieldsRequestTests extends AbstractWireSerializingTestCase<GetInferredFieldsRequest> {

    @Override
    protected GetInferredFieldsRequest createTestInstance() {
        return new GetInferredFieldsRequest(generateRandomStringArray(1, 10, false, false));
    }

    @Override
    protected Writeable.Reader<GetInferredFieldsRequest> instanceReader() {
        return GetInferredFieldsRequest::new;
    }

    @Override
    protected GetInferredFieldsRequest mutateInstance(GetInferredFieldsRequest instance) throws IOException {
        String[] indices = instance.indices();
        String[] newIndices = Arrays.copyOf(indices, indices.length);
        if (indices.length > 0 && randomBoolean()) {
            newIndices[0] = randomValueOtherThan(indices[0], () -> randomAlphaOfLengthBetween(2, 10));
        } else {
            newIndices = new String[] { randomAlphaOfLengthBetween(2, 10) };
        }
        return new GetInferredFieldsRequest(newIndices);
    }

    public void testConstructorWithIndices() {
        GetInferredFieldsRequest request = new GetInferredFieldsRequest("idx1", "idx2");
        assertArrayEquals(new String[] { "idx1", "idx2" }, request.indices());
    }
}
