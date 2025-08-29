/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.transport.grpc.proto.response.common;

import org.opensearch.protobufs.FieldValue;
import org.opensearch.protobufs.GeneralNumber;
import org.opensearch.protobufs.ObjectMap;

import java.util.Map;

/**
 * Utility class for converting generic Java objects to Protocol Buffer FieldValue type.
 * This class provides methods to transform Java objects of various types (primitives, strings,
 * maps, etc.) into their corresponding Protocol Buffer representations for gRPC communication.
 */
public class FieldValueProtoUtils {

    private FieldValueProtoUtils() {
        // Utility class, no instances
    }

    /**
     * Converts a generic Java Object to its Protocol Buffer FieldValue representation.
     * This method handles various Java types (Integer, Long, Double, Float, String, Boolean, Enum, Map)
     * and converts them to the appropriate FieldValue type.
     *
     * @param javaObject The Java object to convert
     * @return A Protocol Buffer FieldValue representation of the Java object
     * @throws IllegalArgumentException if the Java object type cannot be converted
     */
    public static FieldValue toProto(Object javaObject) {
        FieldValue.Builder fieldValueBuilder = FieldValue.newBuilder();
        toProto(javaObject, fieldValueBuilder);
        return fieldValueBuilder.build();
    }

    /**
     * Converts a generic Java Object to its Protocol Buffer FieldValue representation.
     * It handles various Java types (Integer, Long, Double, Float, String, Boolean, Enum, Map)
     * and converts them to the appropriate FieldValue type.
     *
     * @param javaObject The Java object to convert
     * @param fieldValueBuilder The builder to populate with the Java object data
     * @throws IllegalArgumentException if the Java object type cannot be converted
     */
    public static void toProto(Object javaObject, FieldValue.Builder fieldValueBuilder) {
        if (javaObject == null) {
            throw new IllegalArgumentException("Cannot convert null to FieldValue");
        }

        if (javaObject instanceof String) {
            fieldValueBuilder.setStringValue((String) javaObject);
        } else if (javaObject instanceof Integer) {
            fieldValueBuilder.setGeneralNumber(GeneralNumber.newBuilder().setInt32Value((Integer) javaObject).build());
        } else if (javaObject instanceof Long) {
            fieldValueBuilder.setGeneralNumber(GeneralNumber.newBuilder().setInt64Value((Long) javaObject).build());
        } else if (javaObject instanceof Double) {
            fieldValueBuilder.setGeneralNumber(GeneralNumber.newBuilder().setDoubleValue((Double) javaObject).build());
        } else if (javaObject instanceof Float) {
            fieldValueBuilder.setGeneralNumber(GeneralNumber.newBuilder().setFloatValue((Float) javaObject).build());
        } else if (javaObject instanceof Boolean) {
            fieldValueBuilder.setBoolValue((Boolean) javaObject);
        } else if (javaObject instanceof Enum) {
            fieldValueBuilder.setStringValue(javaObject.toString());
        } else if (javaObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) javaObject;
            handleMapValue(map, fieldValueBuilder);
        } else {
            throw new IllegalArgumentException("Cannot convert " + javaObject + " to FieldValue");
        }
    }

    /**
     * Helper method to handle Map values.
     *
     * @param map The map to convert
     * @param fieldValueBuilder The builder to populate with the map data
     */
    @SuppressWarnings("unchecked")
    private static void handleMapValue(Map<String, Object> map, FieldValue.Builder fieldValueBuilder) {
        ObjectMap.Builder objectMapBuilder = ObjectMap.newBuilder();

        // Process each map entry
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            objectMapBuilder.putFields(entry.getKey(), ObjectMapProtoUtils.toProto(entry.getValue()));
        }

        fieldValueBuilder.setObjectMap(objectMapBuilder.build());
    }
}
