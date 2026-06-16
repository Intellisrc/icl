package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.MariaDB

class AutoMariaDBTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new MariaDB(
            user: "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname: "test",
            port: 33007
        )
    }
}
