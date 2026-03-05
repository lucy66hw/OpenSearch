/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.Arrays;

public class GetMaxFieldCountRequestTests extends AbstractWireSerializingTestCase<GetMaxFieldCountRequest> {

    @Override
    protected GetMaxFieldCountRequest createTestInstance() {
        return new GetMaxFieldCountRequest(generateRandomStringArray(1, 10, false, false));
    }

    @Override
    protected Writeable.Reader<GetMaxFieldCountRequest> instanceReader() {
        return GetMaxFieldCountRequest::new;
    }

    @Override
    protected GetMaxFieldCountRequest mutateInstance(GetMaxFieldCountRequest instance) throws IOException {
        String[] indices = instance.indices();
        String[] newIndices = Arrays.copyOf(indices, indices.length);
        if (indices.length > 0 && randomBoolean()) {
            newIndices[0] = randomValueOtherThan(indices[0], () -> randomAlphaOfLengthBetween(2, 10));
        } else {
            newIndices = new String[] { randomAlphaOfLengthBetween(2, 10) };
        }
        return new GetMaxFieldCountRequest(newIndices);
    }

    public void testConstructorWithIndices() {
        GetMaxFieldCountRequest request = new GetMaxFieldCountRequest("idx1", "idx2");
        assertArrayEquals(new String[] { "idx1", "idx2" }, request.indices());
    }
}
