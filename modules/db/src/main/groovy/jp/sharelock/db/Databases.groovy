package jp.sharelock.db

import jp.sharelock.etc.Log

/**
 * When using more than one database in an application,
 * use this class. If only one database is used, "Database"
 * must be used.
 * @since 17/12/14.
 */
class Databases {
    static private Map<String, DBPool> pools = [:]
    static void init(String key, DB.Starter type = null, int timeout = 0) {
        if(type == null) {
            type = new JDBC()
        }
        if(!pools.containsKey(key)) {
            pools[key] = new DBPool()
            pools[key].init(type, timeout)
        } else {
            Log.w("Pool with key: $key already initialized. Ignored")
        }
    }
    static void quitAll() {
        pools.keySet().each {
            String key ->
                pools[key].quit()
        }
        pools = [:]
    }
    static void quit(String key) {
        if(pools.containsKey(key)) {
            pools[key].quit()
            pools.remove(key)
        } else {

            Log.e("Pool with key: $key does not exists.")
        }
    }
    static int getTotalConnections() {
        int total = 0
        pools.keySet().each {
            String key ->
                total += getConnections(key)
        }
        return total
    }
    static int getConnections(String key) {
        int connections = 0
        if(pools.containsKey(key)) {
            connections = pools[key].currentConnections
        } else {
            Log.e("Pool with key: $key does not exists.")
        }
        return connections
    }
    static boolean isInitialized(String key) {
        boolean init = false
        if(pools.containsKey(key)) {
            init = pools[key].initialized
        } else {
            Log.e("Pool with key: $key does not exists.")
        }
        return init
    }
    static DB connect(String key) {
        DB db = null
        if(pools.containsKey(key)) {
            db = new PoolConnector(pools[key]).getDB()
        } else {
            Log.e("Pool with key: $key does not exists.")
        }
        return db
    }
}
