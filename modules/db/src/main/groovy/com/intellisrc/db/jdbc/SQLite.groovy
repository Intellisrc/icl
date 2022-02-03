package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * SQLite Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.sqlite.memory = false
 */
@CompileStatic
class SQLite extends JDBC {
    String dbname = ""
    String user = ""
    String password = ""
    String driver = "org.sqlite.JDBC"

    // SQLite specific parameters:
    boolean memory = Config.get("db.sqlite.memory", false)
    @Override
    String getConnectionString() {
        return memory ? "sqlite::memory:" : "sqlite:$dbname"
    }

    @Override
    String getCatalogSearchName() {
        return "%"
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String fieldsQuotation = '`'
    boolean supportsDate = false
    boolean useFetch = false

    @Override
    String getCreateDatabaseQuery() {
        return "" // Not needed
    }

    @Override
    String getDropDatabaseQuery() {
        return "" // Not needed
    }

    /**
     * Truncate a table
     * ... when you execute a DELETE statement without a WHERE clause, the TRUNCATE optimizer
     * is run instead of the normal delete behavior...
     * @param table
     * @return
     */
    @Override
    String getTruncateQuery(String table) {
        return "DELETE FROM $table"
    }
}
