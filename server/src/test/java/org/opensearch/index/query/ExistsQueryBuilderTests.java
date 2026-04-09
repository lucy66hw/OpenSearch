/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.index.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.NormsFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.IndexSettings;
import org.opensearch.test.AbstractQueryTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;

public class ExistsQueryBuilderTests extends AbstractQueryTestCase<ExistsQueryBuilder> {

    @Override
    protected Settings createTestIndexSettings() {
        // Enable inferred mapping but exclude "foo" so testMustRewrite still gets MatchNoDocsQuery for unmapped "foo"
        return Settings.builder()
            .put(super.createTestIndexSettings())
            .put(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_ENABLED.getKey(), true)
            .putList(IndexSettings.INDEX_INFER_DYNAMIC_FIELDS_EXCLUDED.getKey(), "foo")
            .build();
    }

    @Override
    protected ExistsQueryBuilder doCreateTestQueryBuilder() {
        String fieldPattern;
        if (randomBoolean()) {
            fieldPattern = randomFrom(MAPPED_FIELD_NAMES);
        } else {
            fieldPattern = randomAlphaOfLengthBetween(1, 10);
        }

        // Avoid patterns that would match the derived field "raw.derived_keyword"
        // which doesn't support exists queries
        if (fieldPattern.startsWith("raw")) {
            fieldPattern = TEXT_FIELD_NAME;
        }

        // also sometimes test wildcard patterns
        if (randomBoolean()) {
            if (randomBoolean() && !fieldPattern.equals("r") && !fieldPattern.equals("ra")) {
                fieldPattern = fieldPattern + "*";
            }
        }

        return new ExistsQueryBuilder(fieldPattern);
    }

    @Override
    protected void doAssertLuceneQuery(ExistsQueryBuilder queryBuilder, Query query, QueryShardContext context) throws IOException {
        String fieldPattern = queryBuilder.fieldName();
        Set<String> fields = new HashSet<>(context.simpleMatchToIndexNames(fieldPattern));
        // When wildcard matches no concrete fields but pattern is inferred, ExistsQueryBuilder still builds a query
        if (fields.isEmpty()
            && context.getIndexSettings().isInferDynamicFieldsEnabled()
            && context.getIndexSettings().shouldInferField(fieldPattern)) {
            fields = Collections.singleton(fieldPattern);
        }
        // Include inferred fields (unmapped but infer-enabled) as "mapped" for assertion
        Collection<String> mappedFields = fields.stream()
            .filter(
                (field) -> context.getObjectMapper(field) != null
                    || context.getMapperService().fieldType(field) != null
                    || (context.getIndexSettings().isInferDynamicFieldsEnabled() && context.getIndexSettings().shouldInferField(field))
            )
            .collect(Collectors.toList());
        if (mappedFields.size() == 0) {
            assertThat(query, instanceOf(MatchNoDocsQuery.class));
            return;
        }
        if (fields.size() == 1) {
            assertThat(query, instanceOf(ConstantScoreQuery.class));
            ConstantScoreQuery constantScoreQuery = (ConstantScoreQuery) query;
            String field = expectedFieldName(fields.iterator().next());
            if (context.getObjectMapper(field) != null) {
                Query inner = constantScoreQuery.getQuery();
                if (inner instanceof BooleanQuery) {
                    BooleanQuery booleanQuery = (BooleanQuery) inner;
                    List<String> childFields = new ArrayList<>();
                    context.getObjectMapper(field).forEach(mapper -> childFields.add(mapper.name()));
                    assertThat(booleanQuery.clauses().size(), equalTo(childFields.size()));
                    for (int i = 0; i < childFields.size(); i++) {
                        BooleanClause booleanClause = booleanQuery.clauses().get(i);
                        assertThat(booleanClause.getOccur(), equalTo(BooleanClause.Occur.SHOULD));
                    }
                }
                // else: object has a MappedFieldType so production uses fieldType.existsQuery (single query)
            } else {
                org.opensearch.index.mapper.MappedFieldType fieldType = context.getMapperService().fieldType(field);
                if (fieldType == null
                    && context.getIndexSettings().isInferDynamicFieldsEnabled()
                    && context.getIndexSettings().shouldInferField(field)) {
                    fieldType = context.failIfFieldMappingNotFound(field, null);
                }
                if (fieldType != null && fieldType.hasDocValues()) {
                    // DocValuesFieldExistsQuery extends FieldExistsQuery in Lucene 9.x
                    assertThat(constantScoreQuery.getQuery(), instanceOf(FieldExistsQuery.class));
                    assertEquals(field, ((FieldExistsQuery) constantScoreQuery.getQuery()).getField());
                } else if (fieldType != null && fieldType.getTextSearchInfo().hasNorms()) {
                    assertThat(constantScoreQuery.getQuery(), instanceOf(NormsFieldExistsQuery.class));
                    NormsFieldExistsQuery normsExistsQuery = (NormsFieldExistsQuery) constantScoreQuery.getQuery();
                    assertEquals(field, normsExistsQuery.getField());
                } else if (fieldType != null) {
                    assertThat(constantScoreQuery.getQuery(), instanceOf(TermQuery.class));
                    TermQuery termQuery = (TermQuery) constantScoreQuery.getQuery();
                    assertEquals(field, termQuery.getTerm().text());
                } else {
                    // Inferred or other: accept any non-null inner query
                    assertThat(constantScoreQuery.getQuery(), not(nullValue()));
                }
            }
        } else {
            assertThat(query, instanceOf(ConstantScoreQuery.class));
            ConstantScoreQuery constantScoreQuery = (ConstantScoreQuery) query;
            assertThat(constantScoreQuery.getQuery(), instanceOf(BooleanQuery.class));
            BooleanQuery booleanQuery = (BooleanQuery) constantScoreQuery.getQuery();
            assertThat(booleanQuery.clauses().size(), equalTo(mappedFields.size()));
            for (int i = 0; i < mappedFields.size(); i++) {
                BooleanClause booleanClause = booleanQuery.clauses().get(i);
                assertThat(booleanClause.getOccur(), equalTo(BooleanClause.Occur.SHOULD));
            }
        }
    }

    @Override
    public void testMustRewrite() throws IOException {
        QueryShardContext context = createShardContext();
        context.setAllowUnmappedFields(true);
        ExistsQueryBuilder queryBuilder = new ExistsQueryBuilder("foo");
        IllegalStateException e = expectThrows(IllegalStateException.class, () -> queryBuilder.toQuery(context));
        assertEquals("Rewrite first", e.getMessage());
        Query ret = ExistsQueryBuilder.newFilter(context, "foo", false);
        assertThat(ret, instanceOf(MatchNoDocsQuery.class));
    }

    public void testIllegalArguments() {
        expectThrows(IllegalArgumentException.class, () -> new ExistsQueryBuilder((String) null));
        expectThrows(IllegalArgumentException.class, () -> new ExistsQueryBuilder(""));
    }

    public void testFromJson() throws IOException {
        String json = "{\n" + "  \"exists\" : {\n" + "    \"field\" : \"user\",\n" + "    \"boost\" : 42.0\n" + "  }\n" + "}";

        ExistsQueryBuilder parsed = (ExistsQueryBuilder) parseQuery(json);
        checkGeneratedJson(json, parsed);

        assertEquals(json, 42.0, parsed.boost(), 0.0001);
        assertEquals(json, "user", parsed.fieldName());
    }

    /**
     * With inferred mapping mode enabled, an exists query on an unmapped (inferred) field
     * should be allowed and produce a concrete query (e.g. keyword exists), not MatchNoDocsQuery.
     */
    public void testInferredMappingModeExistsQuerySucceedsForInferredField() throws IOException {
        QueryShardContext context = createShardContext();
        String inferredField = "inferred_xyz";  // not in mapping, not in excluded list
        Query query = ExistsQueryBuilder.newFilter(context, inferredField, false);
        assertThat(query, instanceOf(ConstantScoreQuery.class));
        assertThat(query, not(instanceOf(MatchNoDocsQuery.class)));
    }
}
