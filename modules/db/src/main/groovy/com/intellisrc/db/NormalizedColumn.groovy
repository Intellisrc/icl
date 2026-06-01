package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * Normalized column information to be used by ColumnInfo and ColumnDB
 * @since 2026/05/27.
 */
@CompileStatic
interface NormalizedColumn {
    ColumnDefinition getNormalized()
}