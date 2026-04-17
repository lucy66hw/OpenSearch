/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.mapper;

import org.apache.lucene.index.FieldInfos;
import org.opensearch.common.annotation.ExperimentalApi;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the merged Lucene {@link FieldInfos} to enforce
 * {@code index.mapping.total_fields.limit} when inferred mapping mode is
 * enabled.
 * <p>
 * The reference is updated on every Lucene refresh cycle by a refresh listener
 * registered in IndexShard, so it may be stale by up to one refresh interval
 * (default 1 second). This is an acceptable trade-off for near-zero
 * per-document overhead (an atomic reference read).
 *
 * @opensearch.experimental
 */
@ExperimentalApi
public class LuceneFieldTracker {

    private final AtomicReference<FieldInfos> fieldInfosRef = new AtomicReference<>(FieldInfos.EMPTY);

    /**
     * Updates the FieldInfos snapshot. Called by the refresh listener with
     * {@code FieldInfos.getMergedFieldInfos(reader)}.
     */
    public void setFieldInfos(FieldInfos fieldInfos) {
        this.fieldInfosRef.set(fieldInfos);
    }

    /**
     * Returns the current (possibly stale by one refresh interval) merged
     * FieldInfos across all segments.
     */
    public FieldInfos getFieldInfos() {
        return fieldInfosRef.get();
    }
}
