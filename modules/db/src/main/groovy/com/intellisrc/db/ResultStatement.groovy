package com.intellisrc.db

import groovy.transform.CompileStatic

import java.time.LocalDateTime

/**
 * @since 2022/08/09.
 */
@CompileStatic
interface ResultStatement {
    boolean next()
    void close()
    int columnCount()
    int firstColumn()
    int updatedCount()
    ColumnType columnType(int index)
    String columnName(int index)
    char columnChar(int index)
    char[] columnChars(int index)
    String columnStr(int index)
    boolean columnBool(int index)
    Integer columnInt(int index)
    Double columnDbl(int index)
    Float columnFloat(int index)

    LocalDateTime columnDate(int index)
    byte[] columnBlob(int index)
    boolean isColumnNull(int index)
}
