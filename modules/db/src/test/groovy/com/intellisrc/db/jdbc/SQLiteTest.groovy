package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class SQLiteTest extends JDBCTest {

    String getTableCreate(String name) {
        return """CREATE TABLE `$name` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL UNIQUE,
                `version` REAL,
                `active` TEXT,
                `updated` TEXT 
        );"""
    }

    @Override
    JDBC getDB() {
        return new SQLite(
            memory: true
        )
    }
}