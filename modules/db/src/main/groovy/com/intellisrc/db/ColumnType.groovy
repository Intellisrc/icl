package com.intellisrc.db

import groovy.transform.CompileStatic

import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * @since 2022/01/25.
 */
@CompileStatic
enum ColumnType {
    // String / Text variants
    CHAR(false),
    VARCHAR(true),
    TEXT(true),
    CLOB(true),

    // Numeric variants
    BOOLEAN(false),
    TINYINT(false),
    SMALLINT(false),
    INTEGER(false),
    BIGINT(false),
    DECIMAL(true), // Supports scale/precision
    FLOAT(false),
    DOUBLE(false),

    // Date/Time variants
    DATE(false),
    TIME(false),
    TIMESTAMP(false),
    TIMESTAMP_TZ(false),

    // Binary / Blobs
    BINARY(true),
    VARBINARY(true),
    BLOB(true),

    NULL(false)

    final boolean requiresLength
    ColumnType(boolean requiresLength) {
        this.requiresLength = requiresLength
    }

    /**
     * Converts this enum to Java classes
     * @return
     */
    Class<?> getJavaClass() {
        return switch (this) {
            // Text types
            case CHAR, VARCHAR, TEXT, CLOB -> String
            // Logical types
            case BOOLEAN                   -> Boolean
            // Integer types
            case TINYINT, SMALLINT, INTEGER -> Integer
            case BIGINT                    -> Long
            // Floating point types
            case FLOAT                     -> Float
            case DOUBLE                    -> Double
            case DECIMAL                   -> BigDecimal
            // Date / Time types
            case DATE                      -> LocalDate
            case TIME                      -> LocalTime
            case TIMESTAMP                 -> LocalDateTime
            case TIMESTAMP_TZ              -> ZonedDateTime
            // Binary types
            case BINARY, VARBINARY, BLOB   -> byte[]

            default                        -> null
        }
    }
    /**
     * Converts Java SQL int to ColumnType:
     * https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs/guide/jdbc/getstart/mapping.doc.html
     * @param type
     * @return
     */
    static ColumnType fromJavaSQL(int type, int decimals) {
        //noinspection GroovyFallthrough
        switch (type) {
            case Types.CHAR:
            case Types.NCHAR:
                return CHAR
            case Types.VARCHAR:
            case Types.NVARCHAR:
                return VARCHAR
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                return TEXT
            case Types.CLOB:
            case Types.NCLOB:
            case Types.JAVA_OBJECT: // Kept here as legacy fallback
                return CLOB

            case Types.BIT:
            case Types.BOOLEAN:
                return BOOLEAN
            case Types.TINYINT:
                return TINYINT
            case Types.SMALLINT:
                return SMALLINT
            case Types.INTEGER:
                return INTEGER
            case Types.BIGINT:
                return BIGINT

            case Types.NUMERIC:
                // If it's numeric but has no decimals, treat as a precise high-capacity Integer/BigInt
                return decimals == 0 ? BIGINT : DECIMAL
            case Types.DECIMAL:
                return DECIMAL
            case Types.REAL:
            case Types.FLOAT:
                return FLOAT
            case Types.DOUBLE:
                return DOUBLE

            case Types.DATE:
                return DATE
            case Types.TIME:
                return TIME
            case Types.TIMESTAMP:
                return TIMESTAMP
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return TIMESTAMP_TZ

            case Types.BINARY:
                return BINARY
            case Types.VARBINARY:
                return VARBINARY
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return BLOB

            case Types.NULL:
            case Types.DISTINCT:
            case Types.ARRAY:
            case Types.OTHER:
                return NULL

            default:
                // Using an explicit class lookup or printing error
                System.err.println("Data type not supported: " + type)
                return NULL
        }
    }
}
