package com.intellisrc.etc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.intellisrc.core.props.StringPropertiesYaml
import com.sleepycat.je.*
import groovy.transform.CompileStatic

/**
 * @author: A.Lepe
 * @since 18/06/29.
 */
@CompileStatic
class BerkeleyDB extends StringPropertiesYaml {
    static File databaseDir = Config.getFile("berkeley.dir", SysInfo.getFile(".berkeley"))
    boolean deferredWrite = Config.get("berkeley.deferred", true)
    String encoding = "UTF-8"
    final String keyPrefix
    String prefixSeparator = "."
    protected Environment dbEnv = null
    protected Database dbB = null
    protected final DatabaseConfig dbConfig = new DatabaseConfig()
    final String databaseName
    /**
     * Constructor specifying database file
     * @param dbFile
     * @param keyPrefix : If used all keys will be prefixed with it (useful to group keys)
     */
    BerkeleyDB(File dbFile, String keyPrefix = "", String prefixSeparator = ".") {
        super(keyPrefix, prefixSeparator)
        databaseDir = dbFile.parentFile
        databaseName = databaseDir.name
        this.keyPrefix = keyPrefix
        setupEnvironment()
    }
    /**
     * Constructor using default directory
     * @param dbName    : Database name (can be also specified in config.properties as: berkeley.db)
     * @param keyPrefix : If used all keys will be prefixed with it (useful to group keys)
     */
    BerkeleyDB(String dbName = "", String keyPrefix = "", String prefixSeparator = ".") {
        super(keyPrefix, prefixSeparator)
        if(!databaseDir.exists()) {
            databaseDir.mkdirs()
        }
        databaseName = Config.get("berkeley.db", dbName)
        this.keyPrefix = keyPrefix
        setupEnvironment()
    }
    /**
     * Setup database environment
     * @return
     */
    private void setupEnvironment() {
        try {
            // create a configuration for DB environment
            EnvironmentConfig envConf = new EnvironmentConfig()
            envConf.allowCreate = true
            dbEnv = new Environment(databaseDir, envConf)
            dbConfig.allowCreate = true
            dbConfig.transactional = !deferredWrite
            dbConfig.deferredWrite = deferredWrite
            dbB = dbEnv.openDatabase(null, databaseName, dbConfig)
        } catch (DatabaseException dbe) {
            Log.e("Error while creating database :", dbe)
        }
    }
    /**
     * Return key with prefix if set
     * @param key
     * @return
     */
    String getFullKey(String key) {
        return keyPrefix ? keyPrefix + prefixSeparator + key : key
    }
    //---------- In BerkeleyDB, keys and values are stored as bytes. ----------
    /**
     * Adds a record using bytes
     * @param key
     * @param value
     * @return
     */
    boolean setBytes(byte[] key, byte[] value) {
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
     * Get String value using raw key (byte[])
     * @param key
     * @return
     */
    String getBytesAsString(byte[] key) {
        if(!key) { return "" }
        return Bytes.toString(getBytes(key), encoding)
    }
    /**
     * Check if particular key exists
     * @param key
     * @return
     */
    boolean exists(byte[] key) {
        if(!key) { return false }
        DatabaseEntry value = new DatabaseEntry()
        value.partial = true
        value.partialLength = 0
        return dbB.get(null, new DatabaseEntry(key), value, LockMode.DEFAULT) == OperationStatus.SUCCESS
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
     * Get all data with keys and values as byte[]
     * For big values or sensitive values prefer getKeys()
     * @return
     */
    Map<byte[],byte[]> getAllByteMap() {
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
    //------------ Exposed methods -----------
    /**
     * Adds a record
     * @param key
     * @param value
     * @return
     */
    @Override
    boolean set(String key, String value) {
        if(!key) { return false }
        return setBytes(Bytes.fromString(getFullKey(key), encoding), Bytes.fromString(value, encoding))
    }
    /**
     * Adds a record with string as key and byte[] as value
     * This method is different from StringProperties as it doesn't required to base64 encode
     * @param key
     * @param value
     * @return
     */
    @Override
    boolean set(String key, byte[] value) {
        if(!key) { return false }
        return setBytes(Bytes.fromString(getFullKey(key), encoding), value)
    }
    /**
     * Get raw value using string key
     * This method is different from StringProperties as it doesn't required to base64 decode
     * @param key
     * @return
     */
    @Override
    Optional<byte[]> getBytes(String key) {
        byte[] bs = exists(key) ? getBytes(Bytes.fromString(getFullKey(key), encoding)) : null
        return Optional.ofNullable(bs)
    }

    /**
     * Get String value using string key
     * @param key
     * @return
     */
    @Override
    String get(String key, String defVal) {
        return exists(key) ? getBytesAsString(Bytes.fromString(getFullKey(key), encoding)) : defVal
    }

    /**
     * Return data as Map of strings
     * @return
     */
    Map<String,String> getAllStringMap() {
        Map<String, String> map = [:]
        getAllByteMap().each {
            byte[] key, byte[] val ->
                map[Bytes.toString(key, encoding)] = Bytes.toString(val, encoding)
        }
        return map
    }
    /**
     * Check if particular key exists using string
     * @param key
     * @return
     */
    @Override
    boolean exists(String key) {
        if(!key) { return false }
        return exists(Bytes.fromString(getFullKey(key)))
    }
    /**
     * Removes a record
     * @param key
     * @return
     */
    @Override
    boolean delete(String key) {
        if(!key) { return false }
        return delete(Bytes.fromString(getFullKey(key), encoding))
    }
    /**
     * Clear all keys
     * @return
     */
    @Override
    boolean clear() {
        return keys.each { delete(it) }
    }
    /**
     * Return all keys as String
     * @return
     */
    @Override
    Set<String> getKeys() {
        return keysAsBytes.collect {
            String key = Bytes.toString(it, encoding)
            if(key.startsWith(prefix + prefixSeparator)) {
                key = key.substring((prefix + prefixSeparator).length())
            }
            return key
        }.toSet()
    }
    /**
     * Return keys including prefix
     * @return
     */
    List<String> getFullKeys() {
        return keysAsBytes.collect { Bytes.toString(it, encoding) }
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
        } catch (Exception ignore){}
        dbEnv.removeDatabase(null, databaseName)
        dbEnv.close()
    }

    /**
     * Synchronize with disk (not required if syncOnSave is true)
     */
    void sync() {
        if(deferredWrite) {
            dbB.sync()
        }
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
