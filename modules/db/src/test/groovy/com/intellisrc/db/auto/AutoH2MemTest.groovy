package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.H2
import com.intellisrc.db.jdbc.JDBC
import org.slf4j.event.Level

class AutoH2MemTest extends UpdateTest {
    Level logLevel = Level.TRACE
    @Override
    JDBC getConnJdbc() {
        return new H2(
            memory: true
        )
    }
}
