package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.PostgreSQL
import com.intellisrc.db.jdbc.SQLServer
import com.intellisrc.db.jdbc.SQLServerTest
import spock.lang.Ignore

/*
TODO: SQLServer fails to Update tables. The reason is because of FKs.
      In order to fix it, we have to drop FKs and recreate them (too much work)

 */
@Ignore
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
