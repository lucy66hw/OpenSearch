/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.admin.indices;

import org.opensearch.action.admin.indices.maxfieldcount.GetMaxFieldCountAction;
import org.opensearch.action.admin.indices.maxfieldcount.GetMaxFieldCountRequest;
import org.opensearch.action.admin.indices.maxfieldcount.GetMaxFieldCountResponse;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.client.node.NodeClient;
import org.opensearch.core.common.Strings;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.GET;

/**
 * REST handler for the max field count API.
 * GET /{index}/_max_field_count or GET /_max_field_count
 *
 * @opensearch.internal
 */
public class RestGetMaxFieldCountAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(
                new Route(GET, "/_max_field_count"),
                new Route(GET, "/{index}/_max_field_count")
            )
        );
    }

    @Override
    public String getName() {
        return "get_max_field_count_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        String[] indices = Strings.splitStringByCommaToArray(request.param("index", "_all"));
        GetMaxFieldCountRequest getMaxFieldCountRequest = new GetMaxFieldCountRequest(indices);
        getMaxFieldCountRequest.indicesOptions(IndicesOptions.fromRequest(request, getMaxFieldCountRequest.indicesOptions()));

        return channel -> client.execute(
            GetMaxFieldCountAction.INSTANCE,
            getMaxFieldCountRequest,
            new RestToXContentListener<>(channel)
        );
    }
}
