package com.intellisrc.db.auto

import com.intellisrc.db.DB
import com.intellisrc.db.Data
import com.intellisrc.db.Query
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.lang.annotation.Annotation

import static com.intellisrc.db.auto.Relational.ColumnDB
import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.*

/**
 * @since 2022/07/05.
 * Methods used by TableUpdater
 */
@CompileStatic
trait AutoJDBC {
    /**
     * Property from JDBC
     * @return
     */
    abstract JDBC.BooleanHandle getBooleanHandle()
    /**
     * Property from JDBC
     * @return
     */
    abstract char getTrueChar()
    /**
     * Property from JDBC
     * @return
     */
    abstract char getFalseChar()
    /**
     * Some databases does not support JSON datatype
     * @return
     */
    boolean supportsJSON = false
    /**
     * AutoJDBC uses CreateTable so we need to store annotation
     */
    Annotation meta
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
    abstract boolean createTable(final DB db, String tableName, String charset, String engine, int version, Collection<ColumnDB> columns, Annotation meta)
    /**
     * Get default statement
     * @param val
     * @param column
     * @param supportsNull
     * @param supportBool
     * @return
     */
    String getDefaultQuery(ColumnDB column, boolean useParenthesis = false) {
        String definition = ""
        Object val = column.defaultVal
        if(! column.annotation.autoincrement()) {
            String dv = getDefaultForType(column)
            if (val != null) { // When default value is null, it will be set as nullable
                boolean isNum = val.toString().isNumber()
                boolean isBool = val instanceof Boolean
                if(isBool) {
                    switch (booleanHandle) {
                        case NUMBER:
                            val = Data.booleanAsInt(val as boolean)
                            isNum = true
                            isBool = false
                            break
                        case CHAR:
                            val = Data.booleanAsChar(val as boolean, trueChar, falseChar)
                            isBool = false
                            break
                    }
                }
                dv = (isNum || isBool) ? val.toString().toUpperCase() : "'${val}'".toString()
            }
            String notNull = ! column.annotation.nullable() ? "NOT NULL" : ""
            definition = [notNull, useParenthesis ? "DEFAULT ($dv)" : "DEFAULT $dv"].join(" ")
        }
        return definition
    }

    /**
     * Converts type to default possible value in database
     * For example, String should be ''
     * @param type
     * @return
     */
    String getDefaultForType(ColumnDB column) {
        String dv
        if(column.annotation.defaultValue() != "") {
            dv = column.annotation.defaultValue()
        } else {
            dv = switch (column.type) {
                case Collection -> "'[]'"
                case Map -> "'{}'"
                case int, short, Integer, BigInteger, long, Long, float, Float, double, Double, BigDecimal -> "0"
                case String, Character, char -> "''"
                default -> "NULL"
            }
        }
        return dv
    }
    /**
     * Create table
     * @param db
     * @param info
     * @return
     */
    boolean createTable(final DB db, Table table, String copyName) {
        return table.createTable(db, copyName)
    }
    /**
     * Copy table structure and data
     * @param db
     * @param from
     * @param to
     * @return
     */
    boolean cloneTable(final DB db, String from, String to, Collection<ColumnDB> columns = []) { copyTableStructure(db, from, to) && copyTableData(db, from, to, columns) }
    /**
     * Copy a table with all constraints
     * @param db
     * @param from
     * @param to
     * @param columns
     * @return
     */
    // Changed for createTable with a name instead
    boolean copyTableStructure(final DB db, String from, String to) { false }
    /**
     * Copy table data
     * @return
     */
    boolean copyTableData(final DB db, String from, String to, Collection<ColumnDB> columns) {
        return set(db, "INSERT INTO $to SELECT * FROM $from")
    }
    /**
     * Reset serial / identity / auto-increment if needed
     * @param db
     * @param from
     * @param to
     * @return
     */
    boolean resetAutoIncrement(final DB db, Table newTable, String name, String backup) {
       return true
    }
    /**
     * Rename table
     * @param from
     * @param to
     * @return
     *
     * WARNING: renaming a table which is referenced may leave incorrect references
     */
    boolean renameTable(final DB db, String from, String to) {
        return set(db, "ALTER TABLE ${from} RENAME TO ${to}")
    }
    /**
     * Turn Foreign Keys ON
     * @return
     */
    boolean turnFK(final DB db, boolean on) {
        return true
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
    abstract String getColumnDefinition(final ColumnDB column)
    abstract String getForeignKey(String tableName, final ColumnDB column)
}