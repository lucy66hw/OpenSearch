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

import java.util.Arrays;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class DocumentGrpcCompositeListenerTests extends OpenSearchTestCase {

    @Mock
    private DocumentGrpcListener delegate1;
    @Mock
    private DocumentGrpcListener delegate2;
    @Mock
    private Logger logger;

    private final org.opensearch.protobufs.BulkRequest bulkReq = org.opensearch.protobufs.BulkRequest.getDefaultInstance();
    private final org.opensearch.protobufs.BulkResponse bulkResp = org.opensearch.protobufs.BulkResponse.getDefaultInstance();

    private DocumentGrpcListener.CompositeListener composite;

    @Before
    public void setUpTest() {
        MockitoAnnotations.openMocks(this);
        List<DocumentGrpcListener> delegates = Arrays.asList(delegate1, delegate2);
        composite = new DocumentGrpcListener.CompositeListener(delegates, logger);
    }

    public void testOnBulkRequestInvokesAllListeners() {
        composite.onBulkRequest(bulkReq);

        verify(delegate1, times(1)).onBulkRequest(bulkReq);
        verify(delegate2, times(1)).onBulkRequest(bulkReq);
        verifyNoInteractions(logger);
    }

    public void testOnBulkResponseInvokesAllListeners() {
        long took = randomNonNegativeLong();
        composite.onBulkResponse(bulkReq, bulkResp, took);

        verify(delegate1).onBulkResponse(bulkReq, bulkResp, took);
        verify(delegate2).onBulkResponse(bulkReq, bulkResp, took);
        verifyNoInteractions(logger);
    }

    public void testOnBulkErrorInvokesAllListeners() {
        Throwable failure = new RuntimeException("boom");
        composite.onBulkError(bulkReq, failure);

        verify(delegate1).onBulkError(bulkReq, failure);
        verify(delegate2).onBulkError(bulkReq, failure);
        verifyNoInteractions(logger);
    }

    public void testBulkRequestContinuesWhenOneListenerThrows() {
        doThrow(new IllegalStateException("delegate1 failed")).when(delegate1).onBulkRequest(bulkReq);

        composite.onBulkRequest(bulkReq);

        verify(delegate2).onBulkRequest(bulkReq);

    }
}
