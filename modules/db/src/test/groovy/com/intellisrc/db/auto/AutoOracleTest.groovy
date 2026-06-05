package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.Oracle

class AutoOracleTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new Oracle(
            user: "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname: "FREEPDB1", //v.23 docker
            //dbname: "XEPDB1", //v.21
            port: 31521,
            convertToLowerCase: true
        )
    }
}
