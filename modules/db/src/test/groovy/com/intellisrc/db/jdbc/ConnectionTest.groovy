package com.intellisrc.db.jdbc

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.core.Secs
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.DatabaseConnectionException
import com.intellisrc.log.CommonLogger
import com.intellisrc.log.PrintLogger
import com.intellisrc.net.LocalHost
import org.slf4j.event.Level
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @since 2026/01/28.
 */
class ConnectionTest extends Specification {
    def setup() {
        Log.i("Setting up Test...")
        PrintLogger printLogger = CommonLogger.default.printLogger
        printLogger.setLevel(Level.DEBUG)
    }

    def "Must handle nicely connection issues using Default"() {
        setup:
            JDBC jdbc = new MySQL(
                user    : "nobody",
                hostname: "127.0.0.1",
                password: "invalid",
                dbname  : "non_existent",
                port    : LocalHost.getFreePort() // Random unused port
            )
            Database.defaultInit(jdbc)
        when:
            println "Opening connection:"
            Database.default.connect()
        then:
            thrown(DatabaseConnectionException)
    }

    def "Must handle nicely connection issues with JDBC object"() {
        setup:
            DB.connectionTimeout = Secs.SECOND_10
            long startTime = System.currentTimeMillis()
            JDBC jdbc = new Oracle(     // SQLServer may fail as it handles different the connection timeout
                user: "nobody",
                hostname: "10.255.255.1", // RFC 1918 blackhole
                password: "invalid",
                dbname: "non_existent"
            )
        when:
            println "Opening connection:"
            new Database(jdbc).connect()
        then:
            thrown(DatabaseConnectionException)
            long duration = Math.round((System.currentTimeMillis() - startTime) / 1000d)
            assert duration > 5
            println "Total time: $duration sec."
    }

    def "Must handle nicely connection issues with Database"() {
        setup:
            JDBC jdbc = new PostgreSQL(
                user    : "nobody",
                hostname: "127.0.0.1",
                password: "invalid",
                dbname  : "non_existent",
                port    : LocalHost.getFreePort(), // Random unused port
            )
            // Here we just prepare the pool, but no actual connection happens
            Database database = new Database(jdbc)
        when:
            println "Opening connection:"
            database.connect()
        then:
            thrown(DatabaseConnectionException)
    }

    def "Must handle nicely connection onError"() {
        setup:
            DB.handleConnectionExceptions = true
            boolean handled = false
            CountDownLatch called = new CountDownLatch(1)
            JDBC jdbc = new PostgreSQL(
                user    : "nobody",
                hostname: "127.0.0.1",
                password: "invalid",
                dbname  : "non_existent",
                port    : LocalHost.getFreePort(), // Random unused port
                onError : {
                    Throwable th ->
                        switch (th) {
                            case DatabaseConnectionException:
                                called.countDown()
                                handled = true
                                break
                        }
                } as JDBC.ErrorHandler
            )
            // Here we just prepare the pool, but no actual connection happens
            Database database = new Database(jdbc)
        when:
            println "Opening connection:"
            database.connect()
            called.await()
        then:
            assert handled
    }

    /*
        What we expect here is to reconnect after connection lost
        Tested: OK
     */
    @Ignore
    def "Manual test of sudden disconnection"() {
        setup:
            String tableName = "samples"
            CountDownLatch errCount = new CountDownLatch(1)
            Database database
            JDBC jdbc = new MariaDB(
                user    : "test",
                hostname: "127.0.0.1",
                password: "test",
                dbname  : "test",
                port    : 33007,
                onError : {
                    Throwable th ->
                        Log.w("Received error: %s", th)
                        switch (th) {
                            case DatabaseConnectionException:
                                println "Now start database..."
                                database.waitForConnection(Millis.SECOND, Millis.SECOND_30)
                                sleep(Millis.SECOND)
                                errCount.countDown()
                                break
                        }
                } as JDBC.ErrorHandler
            )
            // Here we just prepare the pool, but no actual connection happens
            database = new Database(jdbc)
        when:
            println "Opening connection:"
            DB db = database.connect()
            db.setSQL("CREATE TABLE $tableName (`id` TINYINT PRIMARY KEY, `name` VARCHAR(10))")
            db.table(tableName).insert([id: 1, name: "one"])
            db.table(tableName).insert([id: 2, name: "two"])
        then:
            assert db.table(tableName).field("name").get(1).toString() == "one"
        when:
            db.close()
            println "Shutdown database now...."
            sleep(Millis.SECOND_10)
            DB db2 = database.connect()
            println "connect() will reuse the previous connection, so no exception."
            def tryToGet = db2.table(tableName).field("name").get(2).toString() // This should send an exception onError
        then:
            errCount.await(Millis.MINUTE, TimeUnit.MILLISECONDS)
            println "Connection restored..."
            DB db3 = database.connect()
            assert db3.table(tableName).field("name").get(2).toString() == "two" // This should send an exception onError
        cleanup:
            db3.table(tableName).drop()
            db3.close()
            database.quit()
    }
}
