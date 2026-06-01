package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.ColumnDefinition
import groovy.transform.CompileStatic
import java.lang.annotation.Annotation

/**
 * @since 2022/07/05.
 * Methods used by TableUpdater
 */
@CompileStatic
trait AutoJDBC {
    /**
     * AutoJDBC uses CreateTable so we need to store annotation
     */
    Annotation meta
    /**
     * Initialize additional functionality
     */
    void autoInit() {}
    /**
     * Create table
     * @param db
     * @param info
     * @return
     */
    boolean createTable(Table table, String copyName) {
        return table.createTable(copyName)
    }
    /**
     * Default way to return foreign keys declaration
     * @param tableName
     * @param column
     * @return
     */
    String getForeignKey(String tableName, ColumnDefinition column) {
        Log.w("Getting foreign keys from table: %s is not supported", tableName)
        return ""
    }
}