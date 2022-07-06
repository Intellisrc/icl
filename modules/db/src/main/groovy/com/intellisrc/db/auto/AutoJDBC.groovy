package com.intellisrc.db.auto

import com.intellisrc.db.DB
import com.intellisrc.db.Data
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import groovy.transform.CompileStatic

import java.lang.reflect.Field

import static com.intellisrc.db.auto.Table.*

/**
 * @since 2022/07/05.
 * Methods used by TableUpdater
 */
@CompileStatic
trait AutoJDBC {
    /**
     * Initialize additional functionality
     */
    void autoInit(final DB db) {}
    /**
     * Shortcut for db.set
     * @param db
     * @param qry
     * @return
     */
    boolean set(final DB db, String qry) {
        return db.set(new Query(qry))
    }
    /**
     * Shortcut for db.get
     * @param db
     * @param qry
     * @return
     */
    Data get(final DB db, String qry) {
        return db.get(new Query(qry))
    }
    /**
     * Create a table
     * @param db
     * @param tableName
     * @param charset
     * @param engine
     * @param version : definedVersion (in Model code)
     * @param fields
     * @return
     */
    abstract boolean createTable(final DB db, String tableName, String charset, String engine, int version, List<ColumnDB> columns)
    /**
     * Get default statement
     * @param val
     * @param nullable
     * @return
     */
    String getDefaultQuery(Object val, boolean nullable) {
        return ""
    }
    /**
     * Turn Foreign Keys ON
     * @return
     */
    boolean turnFK(final DB db, boolean on) {
        return true
    }
    /**
     * Copy table (schema and data)
     * @param db
     * @param from
     * @param to
     * @return
     */
    boolean copyTable(final DB db, String from, String to) {
        return copyTableDesc(db, from, to) && copyTableData(db, from, to)
    }
    /**
     * Copy table description
     * @return
     */
    boolean copyTableDesc(final DB db, String from, String to) {
        return true
    }
    /**
     * Copy table data
     * @return
     */
    boolean copyTableData(final DB db, String from, String to) {
        return set(db, "INSERT INTO $to SELECT * FROM $from")
    }
    /**
     * Rename table
     * @param from
     * @param to
     * @return
     */
    boolean renameTable(final DB db, String from, String to) {
        return set(db, "ALTER TABLE ${from} RENAME TO ${to}")
    }
    /**
     * Add version to table
     * @param table
     * @param comment
     * @return
     */
    boolean setVersion(final DB db, String dbname, String table, int version) {
        return true
    }
    /**
     * Get version from table
     * @param table
     * @return
     */
    int getVersion(final DB db, String dbname, String table) {
        return 1
    }
    /**
     * Get representation of a field in the database
     * @param field
     * @param column
     * @return
     */
    abstract String getColumnDefinition(Class cType, Column column)
    abstract String getForeignKey(String tableName, final ColumnDB column)
}