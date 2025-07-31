/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.transport.grpc.proto.request.search.query;

import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.search.MatchQuery;
import org.opensearch.protobufs.MultiMatchQuery;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting MultiMatchQuery Protocol Buffers to OpenSearch objects.
 * This class provides methods to transform Protocol Buffer representations of bool queries
 * into their corresponding OpenSearch MultiMatchQueryBuilder implementations for search operations.
 */
public class MultiMatchQueryBuilderProtoUtils {

    private MultiMatchQueryBuilderProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a Protocol Buffer MultiMatchQuery to an OpenSearch MultiMatchQueryBuilder.
     * Similar to {@link MultiMatchQueryBuilder#fromXContent(org.opensearch.core.xcontent.XContentParser)}, this method
     * parses the Protocol Buffer representation and creates a properly configured
     * MultiMatchQueryBuilder with the appropriate fields, type, analyzer, slop, fuzziness, etc.
     *
     * @param multiMatchQueryProto The Protocol Buffer MultiMatchQuery object
     * @return A configured MultiMatchQueryBuilder instance
     * @throws IllegalArgumentException if the query is null or missing required fields
     */
    public static MultiMatchQueryBuilder fromProto(MultiMatchQuery multiMatchQueryProto) {
        // Initialize all variables at the beginning
        Object value = multiMatchQueryProto.getQuery();
        Map<String, Float> fieldsBoosts = new HashMap<>();
        MultiMatchQueryBuilder.Type type = MultiMatchQueryBuilder.DEFAULT_TYPE;
        String analyzer = null;
        int slop = MultiMatchQueryBuilder.DEFAULT_PHRASE_SLOP;
        Fuzziness fuzziness = null;
        int prefixLength = MultiMatchQueryBuilder.DEFAULT_PREFIX_LENGTH;
        int maxExpansions = MultiMatchQueryBuilder.DEFAULT_MAX_EXPANSIONS;
        Operator operator = MultiMatchQueryBuilder.DEFAULT_OPERATOR;
        String minimumShouldMatch = null;
        String fuzzyRewrite = null;
        Float tieBreaker = null;
        Boolean lenient = null;
        Float cutoffFrequency = null;
        MatchQuery.ZeroTermsQuery zeroTermsQuery = MultiMatchQueryBuilder.DEFAULT_ZERO_TERMS_QUERY;
        boolean autoGenerateSynonymsPhraseQuery = true;
        boolean fuzzyTranspositions = MultiMatchQueryBuilder.DEFAULT_FUZZY_TRANSPOSITIONS;

        float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        String queryName = null;

        // Process fields
        if (multiMatchQueryProto.getFieldsCount() > 0) {
            for (String field : multiMatchQueryProto.getFieldsList()) {
                fieldsBoosts.put(field, AbstractQueryBuilder.DEFAULT_BOOST);
            }
        }

        // Process type
        if (multiMatchQueryProto.hasType()) {
            switch (multiMatchQueryProto.getType()) {
                case TEXT_QUERY_TYPE_BEST_FIELDS:
                    type = MultiMatchQueryBuilder.Type.BEST_FIELDS;
                    break;
                case TEXT_QUERY_TYPE_MOST_FIELDS:
                    type = MultiMatchQueryBuilder.Type.MOST_FIELDS;
                    break;
                case TEXT_QUERY_TYPE_CROSS_FIELDS:
                    type = MultiMatchQueryBuilder.Type.CROSS_FIELDS;
                    break;
                case TEXT_QUERY_TYPE_PHRASE:
                    type = MultiMatchQueryBuilder.Type.PHRASE;
                    break;
                case TEXT_QUERY_TYPE_PHRASE_PREFIX:
                    type = MultiMatchQueryBuilder.Type.PHRASE_PREFIX;
                    break;
                case TEXT_QUERY_TYPE_BOOL_PREFIX:
                    type = MultiMatchQueryBuilder.Type.BOOL_PREFIX;
                    break;
                default:
                    // Keep default
            }
        }

        // Process analyzer
        if (multiMatchQueryProto.hasAnalyzer()) {
            analyzer = multiMatchQueryProto.getAnalyzer();
        }

        // Process slop
        if (multiMatchQueryProto.hasSlop()) {
            slop = multiMatchQueryProto.getSlop();
        }

        // Do not process fuzziness (deprecated but still supported)
        /*
        if (multiMatchQueryProto.hasFuzziness()) {
            if (multiMatchQueryProto.getFuzziness().hasStringValue()) {
                fuzziness = Fuzziness.build(multiMatchQueryProto.getFuzziness().getStringValue());
            } else if (multiMatchQueryProto.getFuzziness().hasInt32Value()) {
                fuzziness = Fuzziness.build(multiMatchQueryProto.getFuzziness().getInt32Value());
            }
        }
        */

        // Process prefix_length
        if (multiMatchQueryProto.hasPrefixLength()) {
            prefixLength = multiMatchQueryProto.getPrefixLength();
        }

        // Process max_expansions
        if (multiMatchQueryProto.hasMaxExpansions()) {
            maxExpansions = multiMatchQueryProto.getMaxExpansions();
        }

        // Process operator
        if (multiMatchQueryProto.hasOperator()) {
            switch (multiMatchQueryProto.getOperator()) {
                case OPERATOR_AND:
                    operator = Operator.AND;
                    break;
                case OPERATOR_OR:
                    operator = Operator.OR;
                    break;
                default:
                    // Keep default
            }
        }

        // Process minimum_should_match
        if (multiMatchQueryProto.hasMinimumShouldMatch()) {
            if (multiMatchQueryProto.getMinimumShouldMatch().hasStringValue()) {
                minimumShouldMatch = multiMatchQueryProto.getMinimumShouldMatch().getStringValue();
            } else if (multiMatchQueryProto.getMinimumShouldMatch().hasInt32Value()) {
                minimumShouldMatch = String.valueOf(multiMatchQueryProto.getMinimumShouldMatch().getInt32Value());
            }
        }

        // Process fuzzy_rewrite
        if (multiMatchQueryProto.hasFuzzyRewrite()) {
            fuzzyRewrite = multiMatchQueryProto.getFuzzyRewrite();
        }

        // Process tie_breaker
        if (multiMatchQueryProto.hasTieBreaker()) {
            tieBreaker = multiMatchQueryProto.getTieBreaker();
        }

        // Process lenient
        if (multiMatchQueryProto.hasLenient()) {
            lenient = multiMatchQueryProto.getLenient();
        }

        // Do not process cutoff_frequency, as it's deprecated
        /*
        if (multiMatchQueryProto.hasCutoffFrequency()) {
            cutoffFrequency = multiMatchQueryProto.getCutoffFrequency();
        }
        */

        // Process zero_terms_query
        if (multiMatchQueryProto.hasZeroTermsQuery()) {
            switch (multiMatchQueryProto.getZeroTermsQuery()) {
                case ZERO_TERMS_QUERY_NONE:
                    zeroTermsQuery = MatchQuery.ZeroTermsQuery.NONE;
                    break;
                case ZERO_TERMS_QUERY_ALL:
                    zeroTermsQuery = MatchQuery.ZeroTermsQuery.ALL;
                    break;
                case ZERO_TERMS_QUERY_UNSPECIFIED:
                    // Keep default
                    break;
                default:
                    // Keep default
            }
        }

        // Process auto_generate_synonyms_phrase_query
        if (multiMatchQueryProto.hasAutoGenerateSynonymsPhraseQuery()) {
            autoGenerateSynonymsPhraseQuery = multiMatchQueryProto.getAutoGenerateSynonymsPhraseQuery();
        }

        // Process fuzzy_transpositions
        if (multiMatchQueryProto.hasFuzzyTranspositions()) {
            fuzzyTranspositions = multiMatchQueryProto.getFuzzyTranspositions();
        }

        // Process boost
        if (multiMatchQueryProto.hasBoost()) {
            boost = multiMatchQueryProto.getBoost();
        }

        // Process name
        if (multiMatchQueryProto.hasUnderscoreName()) {
            queryName = multiMatchQueryProto.getUnderscoreName();
        }

        // Create the builder with all the extracted values
        MultiMatchQueryBuilder builder = new MultiMatchQueryBuilder(value, fieldsBoosts.keySet().toArray(new String[0])).fields(
            fieldsBoosts
        )
            .type(type)
            .analyzer(analyzer)
            .slop(slop)
            .prefixLength(prefixLength)
            .maxExpansions(maxExpansions)
            .operator(operator)
            .minimumShouldMatch(minimumShouldMatch)
            .fuzzyRewrite(fuzzyRewrite)
            .tieBreaker(tieBreaker)
            .autoGenerateSynonymsPhraseQuery(autoGenerateSynonymsPhraseQuery)
            .fuzzyTranspositions(fuzzyTranspositions)
            .boost(boost)
            .queryName(queryName);

        // if (fuzziness != null) {
        // builder.fuzziness(fuzziness);
        // }

        if (lenient != null) {
            builder.lenient(lenient);
        }

        // if (cutoffFrequency != null) {
        // builder.cutoffFrequency(cutoffFrequency);
        // }

        builder.zeroTermsQuery(zeroTermsQuery);

        return builder;
    }
}
