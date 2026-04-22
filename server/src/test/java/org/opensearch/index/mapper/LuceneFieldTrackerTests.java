/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.mapper;

import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Collections;

public class LuceneFieldTrackerTests extends OpenSearchTestCase {

    private static FieldInfos buildFieldInfos(String... fieldNames) {
        FieldInfo[] infos = new FieldInfo[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            infos[i] = new FieldInfo(
                fieldNames[i],
                i,
                false,
                false,
                false,
                IndexOptions.DOCS,
                DocValuesType.NONE,
                -1,
                Collections.emptyMap(),
                0,
                0,
                0,
                0,
                VectorEncoding.FLOAT32,
                VectorSimilarityFunction.EUCLIDEAN,
                false,
                false
            );
        }
        return new FieldInfos(infos);
    }

    public void testInitialFieldInfosIsEmpty() {
        LuceneFieldTracker tracker = new LuceneFieldTracker();
        assertEquals(0, tracker.getFieldInfos().size());
    }

    public void testSetAndGetFieldInfos() {
        LuceneFieldTracker tracker = new LuceneFieldTracker();
        FieldInfos infos = buildFieldInfos("field1", "field2", "field3");
        tracker.setFieldInfos(infos);
        assertSame(infos, tracker.getFieldInfos());
        assertEquals(3, tracker.getFieldInfos().size());
    }

    public void testOverwrite() {
        LuceneFieldTracker tracker = new LuceneFieldTracker();
        tracker.setFieldInfos(buildFieldInfos("a", "b"));
        tracker.setFieldInfos(buildFieldInfos("x", "y", "z"));
        assertEquals(3, tracker.getFieldInfos().size());
        assertNotNull(tracker.getFieldInfos().fieldInfo("x"));
        assertNull(tracker.getFieldInfos().fieldInfo("a"));
    }

    public void testFieldLookup() {
        LuceneFieldTracker tracker = new LuceneFieldTracker();
        tracker.setFieldInfos(buildFieldInfos("existing_field", "another_field"));
        assertNotNull(tracker.getFieldInfos().fieldInfo("existing_field"));
        assertNotNull(tracker.getFieldInfos().fieldInfo("another_field"));
        assertNull(tracker.getFieldInfos().fieldInfo("missing_field"));
    }
}
