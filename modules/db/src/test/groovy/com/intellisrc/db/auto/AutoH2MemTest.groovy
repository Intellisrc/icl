package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.H2
import com.intellisrc.db.jdbc.JDBC

class AutoH2MemTest extends UpdateTest {
    @Override
    JDBC getConnJdbc() {
        return new H2(
            memory: true
        )
    }
}
