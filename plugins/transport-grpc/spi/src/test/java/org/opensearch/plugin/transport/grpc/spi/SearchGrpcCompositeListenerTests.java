/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.spi;

import org.apache.logging.log4j.Logger;
import org.opensearch.test.OpenSearchTestCase;
import org.junit.Before;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SearchGrpcCompositeListenerTests extends OpenSearchTestCase {

    @Mock
    private SearchGrpcListener listenerA;
    @Mock
    private SearchGrpcListener listenerB;
    @Mock
    private Logger logger;

    private final org.opensearch.protobufs.SearchRequest searchReq = org.opensearch.protobufs.SearchRequest.getDefaultInstance();
    private final org.opensearch.protobufs.SearchResponse searchResp = org.opensearch.protobufs.SearchResponse.getDefaultInstance();

    private SearchGrpcListener.CompositeListener composite;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        composite = new SearchGrpcListener.CompositeListener(asList(listenerA, listenerB), logger);
    }

    public void testOnRequestDelegates() {
        composite.onRequest(searchReq);

        verify(listenerA).onRequest(searchReq);
        verify(listenerB).onRequest(searchReq);
        verifyNoInteractions(logger);
    }

    public void testOnResponseDelegates() {
        long took = randomLongBetween(1, 1_000);
        composite.onResponse(searchReq, searchResp, took);

        verify(listenerA).onResponse(searchReq, searchResp, took);
        verify(listenerB).onResponse(searchReq, searchResp, took);
    }

    public void testListenerExceptionDoesNotStopOthers() {
        doThrow(new RuntimeException("bad listener")).when(listenerA).onError(any(), any());

        Throwable err = new IllegalArgumentException("search failed");
        composite.onError(searchReq, err);

        verify(listenerB).onError(searchReq, err);

    }
}
