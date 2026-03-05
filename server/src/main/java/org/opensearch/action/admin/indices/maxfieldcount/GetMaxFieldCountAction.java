/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.indices.maxfieldcount;

import org.opensearch.action.ActionType;

/**
 * Action to retrieve the maximum Lucene field count across shards of an index.
 *
 * @opensearch.internal
 */
public class GetMaxFieldCountAction extends ActionType<GetMaxFieldCountResponse> {

    public static final GetMaxFieldCountAction INSTANCE = new GetMaxFieldCountAction();
    public static final String NAME = "indices:admin/max_field_count";

    private GetMaxFieldCountAction() {
        super(NAME, GetMaxFieldCountResponse::new);
    }
}
