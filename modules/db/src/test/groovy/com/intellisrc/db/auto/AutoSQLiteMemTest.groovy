package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.SQLite

class AutoSQLiteMemTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new SQLite(
            memory: true
        )
    }
}
