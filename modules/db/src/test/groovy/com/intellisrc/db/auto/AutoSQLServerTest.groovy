package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.PostgreSQL
import com.intellisrc.db.jdbc.SQLServer
import com.intellisrc.db.jdbc.SQLServerTest

class AutoSQLServerTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new SQLServer(
            user    : "sa",
            hostname: "127.0.0.1",
            password: "o2Aksm.A23asl",
            dbname  : "test",
            port    : 31433
        )

    }
}
