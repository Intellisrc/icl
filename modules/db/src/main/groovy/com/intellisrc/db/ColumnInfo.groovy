package com.intellisrc.db

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Representation of a database column
 *
 * https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html
 *
 * @since 2022/01/25.
 */
@CompileStatic
@Immutable
class ColumnInfo {
    int position = 0
    String name = ""
    ColumnType type = ColumnType.NULL
    int length = 0
    int charLength = 0
    int bufferLength = 0
    int decimalDigits = 0
    String defaultValue = ""
    boolean primaryKey = false
    boolean autoIncrement = false
    boolean nullable = true
    boolean generated = false
    boolean unique = false

    String toString() {
        return "$position | " +
            "$name | " +
            "${type.toString()} ($length, $charLength, $bufferLength, $decimalDigits) | " +
            (defaultValue ? "Def: $defaultValue | " : "") +
            (primaryKey ? "PRI | " : "") +
            (autoIncrement ? "AUTO | " : "") +
            (nullable ? "Nullable | " : "") +
            (generated ? "Generated | " : "") +
            (unique ? "Unique | " : "")
    }

    Map toMap() {
        return [
            position    : position,
            name        : name,
            type        : type.toString(),
            length      : length ?: charLength ?: bufferLength,
            digits      : decimalDigits,
            default     : defaultValue,
            primary     : primaryKey,
            autoIncrement : autoIncrement,
            nullable    : nullable,
            generated   : generated,
            unique      : unique
        ]
    }
}
