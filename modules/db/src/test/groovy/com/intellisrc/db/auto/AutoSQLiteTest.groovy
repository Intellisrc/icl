package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.SQLite

class AutoSQLiteTest extends UpdateTest {
    static File sqliteTmp = File.createTempFile("sqlite-", ".db")

    @Override
    JDBC getConnJdbc() {
        return new SQLite(
            dbname: sqliteTmp.absolutePath
        )
    }

    @Override
    def setup() {
        if(sqliteTmp.exists()) {
            sqliteTmp.deleteDir()
        }
    }

    @Override
    def cleanup() {
        if(sqliteTmp.exists()) {
            sqliteTmp.deleteDir()
        }
    }
}
