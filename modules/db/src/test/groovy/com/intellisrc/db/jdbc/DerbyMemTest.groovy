package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class DerbyMemTest extends DerbyTest {
    /**
     * Launch test:
     * (Nothing is needed as it will run in memory).
     * To test with file, set "dbname : something"
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new Derby(
            create  : true,
            memory  : true
        )
    }
}