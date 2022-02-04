package com.intellisrc.db

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.sql.Types

/**
 * @since 2022/01/25.
 */
@CompileStatic
enum ColumnType {
    TEXT, INTEGER, FLOAT, DOUBLE, BLOB, DATE, NULL

    /**
     * Converts Java SQL int to ColumnType:
     * https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs/guide/jdbc/getstart/mapping.doc.html
     * @param type
     * @return
     */
    static ColumnType fromJavaSQL(int type) {
        //noinspection GroovyFallthrough
        switch (type) {
            case Types.NCLOB: //N means: Unicode
            case Types.CHAR:
            case Types.NCHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                return TEXT
            case Types.BIT:
            case Types.BOOLEAN:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                return INTEGER
            case Types.REAL:
            case Types.FLOAT:
                return FLOAT
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.DOUBLE:
                return DOUBLE
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DATE
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
            case Types.CLOB:
                return BLOB
            case Types.NULL:
                return NULL
            case Types.JAVA_OBJECT:
                return TEXT
            default:
                Log.w("Data type not supported: %d", type)
                return NULL
        }
    }
}