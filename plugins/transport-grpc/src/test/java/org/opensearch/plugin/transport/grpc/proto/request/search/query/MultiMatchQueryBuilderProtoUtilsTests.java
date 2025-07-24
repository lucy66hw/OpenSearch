/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.transport.grpc.proto.request.search.query;

import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.search.MatchQuery;
import org.opensearch.protobufs.MinimumShouldMatch;
import org.opensearch.protobufs.MultiMatchQuery;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.opensearch.plugin.transport.grpc.proto.request.search.query.MultiMatchQueryBuilderProtoUtils.fromProto;

public class MultiMatchQueryBuilderProtoUtilsTests extends OpenSearchTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Set up the registry with all built-in converters
        QueryBuilderProtoTestUtils.setupRegistry();
    }

    public void testFromProtoWithRequiredFieldsOnly() {
        // Create a minimal MultiMatchQuery proto with only required fields
        MultiMatchQuery proto = MultiMatchQuery.newBuilder().setQuery("test query").addFields("field1").build();

        // Convert to MultiMatchQueryBuilder
        MultiMatchQueryBuilder builder = fromProto(proto);

        // Verify basic properties
        assertEquals("test query", builder.value());
        assertTrue(builder.fields().containsKey("field1"));
        assertEquals(1.0f, builder.fields().get("field1"), 0.001f);
        assertEquals(MultiMatchQueryBuilder.DEFAULT_TYPE, builder.type());
        assertNull(builder.analyzer());
        assertEquals(MultiMatchQueryBuilder.DEFAULT_PHRASE_SLOP, builder.slop());
        assertEquals(MultiMatchQueryBuilder.DEFAULT_PREFIX_LENGTH, builder.prefixLength());
        assertEquals(MultiMatchQueryBuilder.DEFAULT_MAX_EXPANSIONS, builder.maxExpansions());
        assertEquals(MultiMatchQueryBuilder.DEFAULT_OPERATOR, builder.operator());
        assertNull(builder.minimumShouldMatch());
        assertNull(builder.fuzzyRewrite());
        assertNull(builder.tieBreaker());
        assertEquals(1.0f, builder.boost(), 0.001f);
        assertNull(builder.queryName());
    }

    public void testFromProtoWithAllFields() {
        // Create a complete MultiMatchQuery proto with all fields set
        MultiMatchQuery proto = MultiMatchQuery.newBuilder()
            .setQuery("test query")
            .addFields("field1")
            .addFields("field2")
            .setType(MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_PHRASE)
            .setAnalyzer("standard")
            .setSlop(2)
            .setPrefixLength(3)
            .setMaxExpansions(10)
            .setOperator(org.opensearch.protobufs.Operator.OPERATOR_AND)
            .setMinimumShouldMatch(MinimumShouldMatch.newBuilder().setStringValue("2").build())
            .setFuzzyRewrite("constant_score")
            .setTieBreaker(0.5f)
            .setLenient(true)
            .setZeroTermsQuery(MultiMatchQuery.ZeroTermsQuery.ZERO_TERMS_QUERY_ALL)
            .setAutoGenerateSynonymsPhraseQuery(false)
            .setFuzzyTranspositions(false)
            .setBoost(2.0f)
            .setName("test_query")
            .build();

        // Convert to MultiMatchQueryBuilder
        MultiMatchQueryBuilder builder = fromProto(proto);

        // Verify all properties
        assertEquals("test query", builder.value());
        assertEquals(2, builder.fields().size());
        assertTrue(builder.fields().containsKey("field1"));
        assertTrue(builder.fields().containsKey("field2"));
        assertEquals(1.0f, builder.fields().get("field1"), 0.001f);
        assertEquals(1.0f, builder.fields().get("field2"), 0.001f);
        assertEquals(MultiMatchQueryBuilder.Type.PHRASE, builder.type());
        assertEquals("standard", builder.analyzer());
        assertEquals(2, builder.slop());
        assertEquals(3, builder.prefixLength());
        assertEquals(10, builder.maxExpansions());
        assertEquals(Operator.AND, builder.operator());
        assertEquals("2", builder.minimumShouldMatch());
        assertEquals("constant_score", builder.fuzzyRewrite());
        assertEquals(0.5f, builder.tieBreaker(), 0.001f);
        assertTrue(builder.lenient());
        assertEquals(MatchQuery.ZeroTermsQuery.ALL, builder.zeroTermsQuery());
        assertFalse(builder.autoGenerateSynonymsPhraseQuery());
        assertFalse(builder.fuzzyTranspositions());
        assertEquals(2.0f, builder.boost(), 0.001f);
        assertEquals("test_query", builder.queryName());
    }

    public void testFromProtoWithIntMinimumShouldMatch() {
        // Create a proto with int32 minimum_should_match
        MultiMatchQuery proto = MultiMatchQuery.newBuilder()
            .setQuery("test query")
            .addFields("field1")
            .setMinimumShouldMatch(MinimumShouldMatch.newBuilder().setInt32Value(2).build())
            .build();

        // Convert to MultiMatchQueryBuilder
        MultiMatchQueryBuilder builder = fromProto(proto);

        // Verify minimum_should_match
        assertEquals("2", builder.minimumShouldMatch());
    }

    public void testFromProtoWithStringMinimumShouldMatch() {
        // Create a proto with string minimum_should_match
        MultiMatchQuery proto = MultiMatchQuery.newBuilder()
            .setQuery("test query")
            .addFields("field1")
            .setMinimumShouldMatch(MinimumShouldMatch.newBuilder().setStringValue("75%").build())
            .build();

        // Convert to MultiMatchQueryBuilder
        MultiMatchQueryBuilder builder = fromProto(proto);

        // Verify minimum_should_match
        assertEquals("75%", builder.minimumShouldMatch());
    }

    public void testFromProtoWithDifferentTypes() {
        // Test all possible types
        MultiMatchQuery.TextQueryType[] types = {
            MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_BEST_FIELDS,
            MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_MOST_FIELDS,
            MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_CROSS_FIELDS,
            MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_PHRASE,
            MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_PHRASE_PREFIX,
            MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_BOOL_PREFIX };

        MultiMatchQueryBuilder.Type[] expectedTypes = {
            MultiMatchQueryBuilder.Type.BEST_FIELDS,
            MultiMatchQueryBuilder.Type.MOST_FIELDS,
            MultiMatchQueryBuilder.Type.CROSS_FIELDS,
            MultiMatchQueryBuilder.Type.PHRASE,
            MultiMatchQueryBuilder.Type.PHRASE_PREFIX,
            MultiMatchQueryBuilder.Type.BOOL_PREFIX };

        for (int i = 0; i < types.length; i++) {
            MultiMatchQuery proto = MultiMatchQuery.newBuilder().setQuery("test query").addFields("field1").setType(types[i]).build();

            MultiMatchQueryBuilder builder = fromProto(proto);
            assertEquals(expectedTypes[i], builder.type());
        }
    }

    public void testFromProtoWithDifferentOperators() {
        // Test all possible operators
        org.opensearch.protobufs.Operator[] operators = {
            org.opensearch.protobufs.Operator.OPERATOR_AND,
            org.opensearch.protobufs.Operator.OPERATOR_OR };

        Operator[] expectedOperators = { Operator.AND, Operator.OR };

        for (int i = 0; i < operators.length; i++) {
            MultiMatchQuery proto = MultiMatchQuery.newBuilder()
                .setQuery("test query")
                .addFields("field1")
                .setOperator(operators[i])
                .build();

            MultiMatchQueryBuilder builder = fromProto(proto);
            assertEquals(expectedOperators[i], builder.operator());
        }
    }

    public void testFromProtoWithDifferentZeroTermsQuery() {
        // Test all possible zero_terms_query values
        MultiMatchQuery.ZeroTermsQuery[] zeroTermsQueries = {
            MultiMatchQuery.ZeroTermsQuery.ZERO_TERMS_QUERY_NONE,
            MultiMatchQuery.ZeroTermsQuery.ZERO_TERMS_QUERY_ALL };

        MatchQuery.ZeroTermsQuery[] expectedZeroTermsQueries = { MatchQuery.ZeroTermsQuery.NONE, MatchQuery.ZeroTermsQuery.ALL };

        for (int i = 0; i < zeroTermsQueries.length; i++) {
            MultiMatchQuery proto = MultiMatchQuery.newBuilder()
                .setQuery("test query")
                .addFields("field1")
                .setZeroTermsQuery(zeroTermsQueries[i])
                .build();

            MultiMatchQueryBuilder builder = fromProto(proto);
            assertEquals(expectedZeroTermsQueries[i], builder.zeroTermsQuery());
        }
    }

    public void testFromProtoWithMultipleFields() {
        // Create a proto with multiple fields
        MultiMatchQuery proto = MultiMatchQuery.newBuilder()
            .setQuery("test query")
            .addFields("field1")
            .addFields("field2")
            .addFields("field3")
            .build();

        // Convert to MultiMatchQueryBuilder
        MultiMatchQueryBuilder builder = fromProto(proto);

        // Verify fields
        assertEquals(3, builder.fields().size());
        Set<String> expectedFields = new HashSet<>(Arrays.asList("field1", "field2", "field3"));
        assertEquals(expectedFields, builder.fields().keySet());
    }

    /**
     * Test that compares the results of fromXContent and fromProto to ensure they produce equivalent results.
     */
    public void testFromProtoMatchesFromXContent() throws IOException {
        // 1. Create a JSON string for XContent parsing
        String json = "{\n"
            + "  \"query\": \"test query\",\n"
            + "  \"fields\": [\"field1\", \"field2\"],\n"
            + "  \"type\": \"phrase\",\n"
            + "  \"analyzer\": \"standard\",\n"
            + "  \"slop\": 2,\n"
            + "  \"prefix_length\": 3,\n"
            + "  \"max_expansions\": 10,\n"
            + "  \"operator\": \"AND\",\n"
            + "  \"minimum_should_match\": \"2\",\n"
            + "  \"fuzzy_rewrite\": \"constant_score\",\n"
            + "  \"tie_breaker\": 0.5,\n"
            + "  \"lenient\": true,\n"
            + "  \"zero_terms_query\": \"ALL\",\n"
            + "  \"auto_generate_synonyms_phrase_query\": false,\n"
            + "  \"fuzzy_transpositions\": false,\n"
            + "  \"boost\": 2.0,\n"
            + "  \"_name\": \"test_query\"\n"
            + "}";

        // 2. Parse the JSON to create a MultiMatchQueryBuilder via fromXContent
        XContentParser parser = createParser(JsonXContent.jsonXContent, json);
        parser.nextToken(); // Move to the first token
        MultiMatchQueryBuilder fromXContent = MultiMatchQueryBuilder.fromXContent(parser);

        // 3. Create an equivalent MultiMatchQuery proto
        MultiMatchQuery proto = MultiMatchQuery.newBuilder()
            .setQuery("test query")
            .addFields("field1")
            .addFields("field2")
            .setType(MultiMatchQuery.TextQueryType.TEXT_QUERY_TYPE_PHRASE)
            .setAnalyzer("standard")
            .setSlop(2)
            .setPrefixLength(3)
            .setMaxExpansions(10)
            .setOperator(org.opensearch.protobufs.Operator.OPERATOR_AND)
            .setMinimumShouldMatch(MinimumShouldMatch.newBuilder().setStringValue("2").build())
            .setFuzzyRewrite("constant_score")
            .setTieBreaker(0.5f)
            .setLenient(true)
            .setZeroTermsQuery(MultiMatchQuery.ZeroTermsQuery.ZERO_TERMS_QUERY_ALL)
            .setAutoGenerateSynonymsPhraseQuery(false)
            .setFuzzyTranspositions(false)
            .setBoost(2.0f)
            .setName("test_query")
            .build();

        // 4. Convert the proto to a MultiMatchQueryBuilder
        MultiMatchQueryBuilder fromProto = MultiMatchQueryBuilderProtoUtils.fromProto(proto);

        // 5. Compare the two builders
        assertEquals(fromXContent.value(), fromProto.value());
        assertEquals(fromXContent.fields(), fromProto.fields());
        assertEquals(fromXContent.type(), fromProto.type());
        assertEquals(fromXContent.analyzer(), fromProto.analyzer());
        assertEquals(fromXContent.slop(), fromProto.slop());
        assertEquals(fromXContent.prefixLength(), fromProto.prefixLength());
        assertEquals(fromXContent.maxExpansions(), fromProto.maxExpansions());
        assertEquals(fromXContent.operator(), fromProto.operator());
        assertEquals(fromXContent.minimumShouldMatch(), fromProto.minimumShouldMatch());
        assertEquals(fromXContent.fuzzyRewrite(), fromProto.fuzzyRewrite());
        assertEquals(fromXContent.tieBreaker(), fromProto.tieBreaker(), 0.001f);
        assertEquals(fromXContent.lenient(), fromProto.lenient());
        assertEquals(fromXContent.zeroTermsQuery(), fromProto.zeroTermsQuery());
        assertEquals(fromXContent.autoGenerateSynonymsPhraseQuery(), fromProto.autoGenerateSynonymsPhraseQuery());
        assertEquals(fromXContent.fuzzyTranspositions(), fromProto.fuzzyTranspositions());
        assertEquals(fromXContent.boost(), fromProto.boost(), 0.001f);
        assertEquals(fromXContent.queryName(), fromProto.queryName());
    }
}
