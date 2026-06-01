package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.H2
import com.intellisrc.db.jdbc.JDBC

class AutoH2MemTest extends UpdateTest {
    static File h2Tmp = File.createTempFile("h2-", ".db")

    @Override
    JDBC getConnJdbc() {
        return new H2(
            memory: true
        )
    }

    @Override
    def setup() {
        if(h2Tmp.exists()) {
            h2Tmp.deleteDir()
        }
    }

    @Override
    def cleanup() {
        if(h2Tmp.exists()) {
            h2Tmp.deleteDir()
        }
    }
}
