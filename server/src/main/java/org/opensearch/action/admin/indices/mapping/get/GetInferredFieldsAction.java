/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.ActionType;

/**
 * Internal action to collect inferred (dynamic) field names from each shard for mapping API.
 *
 * @opensearch.internal
 */
public class GetInferredFieldsAction extends ActionType<GetInferredFieldsResponse> {

    public static final GetInferredFieldsAction INSTANCE = new GetInferredFieldsAction();
    public static final String NAME = "indices:admin/mapping/get_inferred_fields";

    private GetInferredFieldsAction() {
        super(NAME, GetInferredFieldsResponse::new);
    }
}
