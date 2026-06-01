package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class SQLiteMemTest extends SQLiteTest {
    @Override
    JDBC getJdbConnector() {
        return new SQLite(
            memory: true
        )
    }
}