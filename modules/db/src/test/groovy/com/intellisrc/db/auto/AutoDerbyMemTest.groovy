package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.Derby
import com.intellisrc.db.jdbc.JDBC

class AutoDerbyMemTest extends ViewTest {
    @Override
    JDBC getConnJdbc() {
        return new Derby(
            create: true,
            memory: true,
            useFK: false
        )
    }
}
