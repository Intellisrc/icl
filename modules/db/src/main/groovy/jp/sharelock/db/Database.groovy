package jp.sharelock.db

import groovy.transform.CompileStatic

/**
 * Main Database initialization class
 *
 * When the application only uses a single database
 * this class must be used. If it uses more than one,
 * use "Databases"
 *
 * @since 17/12/13.
 */
@CompileStatic
class Database {
    static private DBPool pool
    static void init(String urlstr, int timeout = 0) {
		init(new JDBC(connectionString : urlstr), timeout)
	}
    static void init(DB.Starter type = null, int timeout = 0) {
		if(type == null) {
			type = new JDBC()
		}
        if(!pool) {
            pool = new DBPool()
              pool.init(type, timeout)
        }
    }
    static void quit() {
        pool?.quit()
    }
    static int getConnections() {
        return pool?.currentConnections ?: 0
    }
    static boolean isInitialized() {
        return pool?.initialized ?: false
    }
    static DB connect() {
        return new PoolConnector(pool).getDB()
    }
}
