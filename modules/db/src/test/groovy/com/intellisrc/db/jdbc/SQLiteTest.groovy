package com.intellisrc.db.jdbc

import com.intellisrc.db.DB
import org.slf4j.event.Level

/**
 * @since 18/06/15.
 */
class SQLiteTest extends JDBCTest {
    final static File dbFile = File.get(File.tempDir, "sqlite.db")
    Level logLevel = Level.TRACE
    @Override
    JDBC getJdbConnector() {
        return new SQLite(
            dbname: dbFile.absolutePath
        )
    }
    @Override
    void clean(DB db, String table) {
        if(dbFile.exists()) {
            dbFile.delete()
        }
    }
}