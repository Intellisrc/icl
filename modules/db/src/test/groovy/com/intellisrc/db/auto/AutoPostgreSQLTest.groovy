package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.PostgreSQL

class AutoPostgreSQLTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new PostgreSQL(
            user: "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname: "test",
            port: 35432
        )

    }
}
