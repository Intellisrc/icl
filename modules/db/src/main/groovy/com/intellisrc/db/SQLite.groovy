package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/14.
 */
@CompileStatic
class SQLite extends JDBC {
    String dbname = "local.db"
    @Override
    String getConnectionString() {
        return "sqlite://localhost/$dbname"
    }
}
