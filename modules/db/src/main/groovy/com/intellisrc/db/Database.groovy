package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.core.SysClock
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.JDBC.ErrorHandler
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Database initialization class
 * If one database is used, you can use static "default".
 * No need to initialize it if default values (config) are used
 * if other than default is needed, use: Database.defaultInit(...)
 *
 * @since 17/12/13.
 */
@CompileStatic
class Database {
    protected final DBPool pool
    Database(JDBC type = null, int timeout = 0, int expire = 0) {
        pool = new DBPool()
        pool.init(type ?: JDBC.fromSettings(), timeout, expire)
    }
    DB connect() {
        return new PoolConnector(pool).getDB()
    }
    int getConnections() {
        return pool?.active ?: 0
    }
    boolean isInitialized() {
        return pool?.initialized ?: false
    }
    /**
     * Wait for database to be ready
     * @param milliRetry
     * @param milliTimeout
     */
    void waitForConnection(int milliRetry = Millis.SECOND, int milliTimeout = 0) {
        boolean connected = false
        boolean timedOut = false
        LocalDateTime start = SysClock.now
        Log.i("Waiting for database [%s] to become ready...", pool.jdbc.name)
        while(!connected &&! timedOut) {
            DB db = connect()
            connected = db.openIfClosed()
            if(! connected) {
                if(milliRetry) {
                    sleep(milliRetry)
                }
            }
            db.close()
            if(milliTimeout) {
                timedOut = ChronoUnit.MILLIS.between(start, SysClock.now) < milliTimeout
            }
        }
        if(timedOut) {
            Log.w("Database was not ready for connections. Waited: %d ms", milliTimeout)
        }
        if(connected) {
            Log.i("Connected to database: %s", pool.jdbc.name)
        }
    }
    void onError(ErrorHandler handler) {
        pool?.jdbc?.onError = handler
    }
    void quit() {
        pool?.quit()
    }
    // Static --------------------------------
    static protected Database defaultDB
    static Database getDefault() {
        if(!defaultDB || !defaultDB?.initialized) {
            defaultInit()
        }
        return defaultDB
    }
    static void defaultInit(JDBC type = null, int timeout = 0) {
        defaultDB = new Database(type, timeout)
    }
}
