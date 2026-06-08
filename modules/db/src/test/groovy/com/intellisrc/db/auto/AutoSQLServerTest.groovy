package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.PostgreSQL
import com.intellisrc.db.jdbc.SQLServer
import com.intellisrc.db.jdbc.SQLServerTest
import spock.lang.Ignore

/*
//FIXME: (2026-06) SQLServer is failing in one test : "Update with data"
//       Many attempts where done to try to fix it, but failed (due to FK constraints)
 */
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
