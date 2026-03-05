/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache License, Version 2.0
 * or a compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.support.IndicesOptions;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GetMappingsRequestTests extends AbstractWireSerializingTestCase<GetMappingsRequest> {

    @Override
    protected GetMappingsRequest createTestInstance() {
        GetMappingsRequest request = new GetMappingsRequest();
        if (randomBoolean()) {
            request.indices(generateRandomStringArray(5, 10, false, false));
        }
        if (randomBoolean()) {
            request.indicesOptions(
                randomFrom(
                    IndicesOptions.strictExpandOpen(),
                    IndicesOptions.strictExpandOpenAndForbidClosed(),
                    IndicesOptions.lenientExpandOpen()
                )
            );
        }
        request.includeInferred(randomBoolean());
        return request;
    }

    @Override
    protected Writeable.Reader<GetMappingsRequest> instanceReader() {
        return GetMappingsRequest::new;
    }

    @Override
    protected GetMappingsRequest mutateInstance(GetMappingsRequest instance) throws IOException {
        List<Consumer<GetMappingsRequest>> mutators = new ArrayList<>();
        mutators.add(req -> req.indices(generateRandomStringArray(3, 8, false, false)));
        mutators.add(req -> req.includeInferred(!req.includeInferred()));
        mutators.add(req -> req.indicesOptions(randomValueOtherThan(req.indicesOptions(), () -> randomFrom(IndicesOptions.strictExpandOpen(), IndicesOptions.lenientExpandOpen()))));
        GetMappingsRequest mutated = copyInstance(instance);
        randomFrom(mutators).accept(mutated);
        return mutated;
    }

    public void testIncludeInferred() {
        GetMappingsRequest request = new GetMappingsRequest();
        assertFalse("default should be false", request.includeInferred());
        request.includeInferred(true);
        assertTrue(request.includeInferred());
        request.includeInferred(false);
        assertFalse(request.includeInferred());
    }
}
