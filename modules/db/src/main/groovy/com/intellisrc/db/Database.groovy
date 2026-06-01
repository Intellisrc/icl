package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.core.SysClock
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.db.jdbc.JDBC.ErrorHandler
import groovy.transform.CompileStatic

import java.sql.SQLNonTransientConnectionException
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
    DB connect() throws DatabaseConnectionException {
        DB db = new PoolConnector(pool).getDB()
        // Initialize once if needed:
        if(!db.jdbc.initialized) {
            db.jdbc.initialize(db)
            db.jdbc.initialized = true
        }
        db.openIfClosed() //Force connection here so we can throw the Exception
        return db
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
     * @param milliMaxWait : General time to wait until we can connect. This is
     *                      different from DB.connectionTimeout as that is the
     *                      time we wait in each attempt.
     */
    void waitForConnection(int milliRetry = Millis.SECOND, int milliMaxWait = 0) {
        boolean connected = false
        boolean timedOut = false
        LocalDateTime start = SysClock.now

        Log.i("Waiting for database [%s] to become ready...", pool.jdbc.name)
        while(!connected &&! timedOut) {
            try {
                connected = new PoolConnector(pool).open()
            } catch(DatabaseConnectionException ignore) {
                Log.v("Connection failed. Retrying...")
                pool.clear()
            }
            if(! connected) {
                if(milliRetry) {
                    sleep(milliRetry)
                }
            }
            if(milliMaxWait) {
                timedOut = ChronoUnit.MILLIS.between(start, SysClock.now) > milliMaxWait
            }
        }
        if(timedOut) {
            Log.w("Database was not ready for connections. Waited: %d ms", milliMaxWait)
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

    // timeout: Time before a connection is discarded if it is not returned to the pool (usually it means close() is missing)
    // expiration: Expiration time of a connection without being used. Once it expires, a new connection should be created.
    static void defaultInit(JDBC type = null, int timeout = 0, int expiration = 0) throws SQLNonTransientConnectionException {
        defaultDB = new Database(type, timeout, expiration)
    }
}
