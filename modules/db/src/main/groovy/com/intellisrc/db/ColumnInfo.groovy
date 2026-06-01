package com.intellisrc.db

import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.UpdateActions
import groovy.transform.CompileStatic

/**
 * Representation of a database column
 *
 * https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html
 *
 * @since 2022/01/25.
 */
@CompileStatic
class ColumnInfo implements NormalizedColumn {
    int position = 0
    String name = ""
    ColumnType type = ColumnType.NULL
    int length = 0
    int charLength = 0
    int bufferLength = 0
    int decimalDigits = 0
    Object defaultValue = null
    String uniqueGroup = null
    String customType = null
    boolean primaryKey = false
    boolean index = false
    boolean autoIncrement = false
    boolean nullable = true
    boolean generated = false   //like functions
    boolean unique = false
    DeleteActions ondelete = DeleteActions.RESTRICT
    UpdateActions onupdate = UpdateActions.NO_ACTION

    String toString() {
        return "$position | " +
            "$name | " +
            "${type.toString()} ($length, $charLength, $bufferLength, $decimalDigits) | " +
            (defaultValue ? "Def: $defaultValue | " : "") +
            (primaryKey ? "PRI | " : "") +
            (index ? "INDEX | " : "") +
            (autoIncrement ? "AUTO | " : "") +
            (nullable ? "Nullable | " : "") +
            (generated ? "Generated | " : "") +
            (unique ? "Unique | " : "") +
            (uniqueGroup ? "Group | " : "")
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
            index       : index,
            autoIncrement : autoIncrement,
            nullable    : nullable,
            generated   : generated,
            unique      : unique
        ]
    }

    @Override
    ColumnDefinition getNormalized() {
        return new ColumnDefinition(
            autoIncrement: this.autoIncrement,
            index: this.index,
            nullable: this.nullable,
            primaryKey: this.primaryKey,
            unique: this.unique,
            length: this.length,
            name: this.name,
            uniqueGroup: this.uniqueGroup,
            customType: this.customType,
            ondelete: this.ondelete,
            onupdate: this.onupdate,
            type: this.type.javaClass,
            defaultValue: this.defaultValue
        )
    }
}
