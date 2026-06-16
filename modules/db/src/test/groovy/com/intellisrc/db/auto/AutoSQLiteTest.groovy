package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.SQLite
import org.slf4j.event.Level

class AutoSQLiteTest extends UpdateTest {
    final static File dbFile = File.get(File.tempDir, "sqlite.db")
    Level logLevel = Level.TRACE

    @Override
    JDBC getConnJdbc() {
        return new SQLite(
            dbname: dbFile.absolutePath
        )
    }

    @Override
    def setup() {
        if(dbFile.exists()) {
            dbFile.deleteDir()
        }
    }

    @Override
    def cleanup() {
        if(dbFile.exists()) {
            dbFile.deleteDir()
        }
    }
}
