package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.SQLite
import com.intellisrc.log.CommonLogger
import com.intellisrc.log.PrintLogger
import org.slf4j.event.Level
import spock.lang.Specification

class DBTest extends Specification {
    def setup() {
        Log.i("Initializing Test...")
        PrintLogger printLogger = CommonLogger.default.printLogger
        printLogger.setLevel(Level.TRACE)
    }
    /**
     * Currently it is possible to reuse a DB object (connection) even after returning it to the pool (close).
     * What is happening is that the connection is returned to the pool and DB executes 'openIfClosed()' in
     * which will reuse the connection without notifying the pool. One possible issue with that is that
     * a connection may unexpectedly be closed while it is being used. For now we will just test if the flag "returned"
     * is working
     */
    def "Close should not allow reusing it"() {
        setup:
            File dbFile = File.createTempFile("sqlite",".db")
            Database database = new Database(new SQLite(dbname: dbFile.absolutePath))
            DB db = database.connect()
        when:
            assert db.getSQL("SELECT 1").toInt() == 1
            db.close()
        then:
            assert db.closed
        cleanup:
            if(dbFile.exists()) {
                dbFile.delete()
            }
    }
    def "Memory DB should not close db objects"() {
        setup:
            Database database = new Database(new SQLite(memory: true))
            DB db = database.connect()
        when:
            assert db.getSQL("SELECT 1").toInt() == 1
            db.close()
        then:
            assert ! db.closed
    }
}
