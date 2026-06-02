package com.intellisrc.db.jdbc

import com.intellisrc.core.Log
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.TableDefinition
import groovy.transform.CompileStatic
/**
 * Dummy JDBC connector
 * @since 17/12/13.
 */
@CompileStatic
class Dummy extends JDBC {
    String dbname = ""
    String user = ""
    String password = ""
    String driver = ""
    @Override
    String getConnectionString() {
        return connectionURI ?: "dummy://dummy"
    }

    @Override
    String getInfoQuery(String table) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getLastIdQuery(String table, String pk) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getTablesQuery() {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getColumnDefinition(ColumnDefinition column) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getCopyTableStructureSQL(String from, String to, TableDefinition columns) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getIdentitySQL(String table, String columnName) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getIdentityUpdateSQL(String table, String columnName, int value) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getTurnFK(boolean on) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getVersionRead(String table) {
        Log.w("Method not implemented")
        return ""
    }

    @Override
    String getForeignKey(String tableName, ColumnDefinition column) {
        Log.w("Method not implemented")
        return ""
    }
}
