package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.H2
import com.intellisrc.db.jdbc.JDBC
import org.slf4j.event.Level

class AutoH2Test extends UpdateTest {
    static File h2Tmp = File.get(File.tempDir, "h2.db")

    Level logLevel = Level.TRACE

    @Override
    JDBC getConnJdbc() {
        Log.i("Database located at: %s", h2Tmp.absolutePath)
        return new H2(
            dbname: h2Tmp.absolutePath
        )
    }

    @Override
    def setup() {
        if(h2Tmp.exists()) {
            h2Tmp.delete()
        }
    }

    @Override
    def cleanup() {
        if(h2Tmp.exists()) {
            h2Tmp.delete()
        }
    }
}
