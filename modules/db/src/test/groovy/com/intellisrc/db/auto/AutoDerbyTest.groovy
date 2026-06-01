package com.intellisrc.db.auto

import com.intellisrc.db.jdbc.Derby
import com.intellisrc.db.jdbc.JDBC

class AutoDerbyTest extends AutoTest {
    static File derbyTmp = File.get(File.tempDir, "derby.test")
    @Override
    JDBC getConnJdbc() {
        return new Derby(
            create: true,
            memory: true,
            useFK: true,        // No update test
            dbname  : derbyTmp.absolutePath
        )
    }

    @Override
    def setup() {
        if(derbyTmp.exists()) {
            derbyTmp.deleteDir()
        }
    }

    @Override
    def cleanup() {
        if(derbyTmp.exists()) {
            derbyTmp.deleteDir()
        }
    }
}
