package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.MySQL

class AutoMySQLTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new MySQL(
            user: "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname: "test",
            port: 33006
        )
    }
}
