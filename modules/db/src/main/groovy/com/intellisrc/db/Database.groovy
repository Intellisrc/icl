package com.intellisrc.db

import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic
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
    protected DBPool pool
    Database(JDBC type = null, int timeout = 0, int expire = 0) {
		if(type == null) {
            type = JDBC.fromSettings()
		}
        if(!pool) {
            pool = new DBPool()
            pool.init(type, timeout, expire)
        }
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
    void quit() {
        pool?.quit()
        pool = null
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
