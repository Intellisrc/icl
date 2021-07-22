package com.intellisrc.etc

import com.intellisrc.core.SysInfo
import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.sleepycat.je.Cursor
import com.sleepycat.je.Database
import com.sleepycat.je.DatabaseConfig
import com.sleepycat.je.DatabaseEntry
import com.sleepycat.je.DatabaseException
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig
import com.sleepycat.je.LockMode
import com.sleepycat.je.OperationStatus
import groovy.transform.CompileStatic

/**
 * @author: A.Lepe
 * @since 18/06/29.
 */
@CompileStatic
class BerkeleyDB {
    static File databaseDir = Config.getFile("berkeley.dir", SysInfo.getFile(".berkeley"))
    String encoding = "UTF-8"
    protected Environment dbEnv = null
    protected Database dbB = null
    protected final DatabaseConfig dbConfig = new DatabaseConfig()
    final String databaseName
    /**
     * Constructor specifying database file
     * @param dbFile
     */
    BerkeleyDB(File dbFile) {
        databaseDir = dbFile.parentFile
        databaseName = databaseDir.name
        setupEnvironment()
    }
    /**
     * Constructor using default directory
     * @param dbName
     */
    BerkeleyDB(String dbName = "") {
        if(!databaseDir.exists()) {
            databaseDir.mkdirs()
        }
        databaseName = Config.get("berkeley.db", dbName)
        setupEnvironment()
    }
    /**
     * Setup database environment
     * @return
     */
    private setupEnvironment() {
        try {
            // create a configuration for DB environment
            EnvironmentConfig envConf = new EnvironmentConfig()
            envConf.allowCreate = true
            dbEnv = new Environment(databaseDir, envConf)
            dbConfig.allowCreate = true
            dbB = dbEnv.openDatabase(null, databaseName, dbConfig)
        } catch (DatabaseException dbe) {
            Log.e("Error while creating database :", dbe)
        }
    }
    /**
     * Adds a record
     * @param key
     * @param value
     * @return
     */
    boolean set(String key, String value) {
        if(!key) { return false }
        return set(Bytes.fromString(key, encoding), Bytes.fromString(value, encoding))
    }
    /**
     * Adds a record with string as key and byte[] as value
     * @param key
     * @param value
     * @return
     */
    boolean set(String key, byte[] value) {
        if(!key) { return false }
        return set(Bytes.fromString(key, encoding), value)
    }
    /**
     * Adds a record using bytes
     * @param key
     * @param value
     * @return
     */
    boolean set(byte[] key, byte[] value) {
        if(!key) { return false }
        return dbB.put(null, new DatabaseEntry(key), new DatabaseEntry(value)) == OperationStatus.SUCCESS
    }
    /**
     * Get raw value using raw key
     * @param key
     * @return
     */
    byte[] getBytes(byte[] key) {
        if(!key) { return new byte[0] }
        DatabaseEntry value = new DatabaseEntry()
        dbB.get(null, new DatabaseEntry(key), value, LockMode.DEFAULT)
        return value.data
    }
    /**
     * Get raw value using string key
     * @param key
     * @return
     */
    byte[] getBytes(String key) {
        if(!key) { return new byte[0] }
        return getBytes(Bytes.fromString(key, encoding))
    }
    /**
     * Get String value using raw key (byte[])
     * @param key
     * @return
     */
    String get(byte[] key) {
        if(!key) { return "" }
        return Bytes.toString(getBytes(key), encoding)
    }
    /**
     * Get String value using string key
     * @param key
     * @return
     */
    String get(String key) {
        if(!key) { return "" }
        return get(Bytes.fromString(key, encoding))
    }

    /**
     * Return data as Map with keys as string and byte[] as values
     * @return
     */
    Map<String,byte[]> getMapAsBytes() {
        Map<String, byte[]> map = [:]
        getMap().each {
            byte[] key, byte[] val ->
                map[Bytes.toString(key, encoding)] = val
        }
        return map
    }
    /**
     * Return data as Map of strings
     * @return
     */
    Map<String,String> getMapAsString() {
        Map<String, String> map = [:]
        getMap().each {
            byte[] key, byte[] val ->
                map[Bytes.toString(key, encoding)] = Bytes.toString(val, encoding)
        }
        return map
    }
    /**
     * Get all data with keys and values as byte[]
     * For big values or sensitive values prefer getKeys()
     * @return
     */
    Map<byte[],byte[]> getMap() {
        Map<byte[],byte[]> data = [:]
        Cursor cursor = null
        try {
            cursor = dbB.openCursor(null, null)
            DatabaseEntry foundKey = new DatabaseEntry()
            DatabaseEntry value = new DatabaseEntry()
            while (cursor.getNext(foundKey, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                data[foundKey.data] = value.data
            }
        } catch (DatabaseException de) {
            Log.e("Error reading from database: ", de)
        } finally {
            try {
                cursor.close()
            } catch(DatabaseException dbe) {
                Log.e("Error closing cursor: ", dbe)
            }
        }
        return data
    }
    /**
     * Check if particular key exists
     * @param key
     * @return
     */
    boolean has(byte[] key) {
        if(!key) { return false }
        DatabaseEntry value = new DatabaseEntry()
        value.partial = true
        value.partialLength = 0
        return dbB.get(null, new DatabaseEntry(key), value, LockMode.DEFAULT) == OperationStatus.SUCCESS
    }
    /**
     * Check if particular key exists using string
     * @param key
     * @return
     */
    boolean has(String key) {
        if(!key) { return false }
        return has(Bytes.fromString(key))
    }
    /**
     * Removes a record
     * @param key
     * @return
     */
    boolean delete(String key) {
        if(!key) { return false }
        return delete(Bytes.fromString(key, encoding))
    }
    /**
     * Removes a record using byte[] key
     * @param key
     * @return
     */
    boolean delete(byte[] key) {
        if(!key) { return false }
        return dbB.delete(null, new DatabaseEntry(key)) == OperationStatus.SUCCESS
    }
    /**
     * Return all keys as String
     * @return
     */
    List<String> getKeys() {
        return keysAsBytes.collect { Bytes.toString(it, encoding) }
    }
    /**
     * Return all keys as raw byte[]
     * @return
     */
    List<byte[]> getKeysAsBytes() {
        List<byte[]> keys = []
        Cursor cursor = null
        try {
            cursor = dbB.openCursor(null, null)
            DatabaseEntry foundKey = new DatabaseEntry()
            DatabaseEntry value = new DatabaseEntry()
            value.partial = true
            value.partialLength = 0
            while (cursor.getNext(foundKey, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                keys << foundKey.data
            }
        } catch (DatabaseException de) {
            Log.e("Error reading from database: ", de)
        } finally {
            try {
                cursor.close()
            } catch(DatabaseException dbe) {
                Log.e("Error closing cursor: ", dbe)
            }
        }
        return keys
    }

    /**
     * Returns true if database has no records
     * @return
     */
    boolean isEmpty() {
        return dbB.count() > 0
    }

    /**
     * Returns number of records
     * @return
     */
    long getSize() {
        return dbB.count()
    }

    /**
     * Destroys a database
     */
    void destroy() {
        try {
            dbB.close()
        } catch (Exception e){}
        dbEnv.removeDatabase(null, databaseName)
        dbEnv.close()
    }

    /**
     * Close link
     */
    void close() {
        if(!dbEnv.closed) {
            dbB.close()
            dbEnv.close()
        }
    }
}
