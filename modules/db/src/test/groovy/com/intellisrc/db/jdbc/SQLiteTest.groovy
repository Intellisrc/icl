package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

/**
 * @since 18/06/15.
 */
class SQLiteTest extends JDBCTest {
    final static File dbFile = File.createTempFile("sqlite-",".db")
    @Override
    JDBC getDB() {
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