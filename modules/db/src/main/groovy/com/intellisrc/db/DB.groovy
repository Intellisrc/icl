package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.etc.Cache
import groovy.transform.CompileStatic

import java.time.LocalDateTime

import static com.intellisrc.db.ColumnType.*
import static com.intellisrc.db.Query.Action.*
import static com.intellisrc.db.Query.FieldType.*

@CompileStatic
/**
 * This class uses an interface of Connector to
 * interact with the Database
 * @author A.Lepe
 * @since 2016-10
 */
class DB {
    static protected Cache<Data> dataCache = new Cache<Data>(extend: false)
    static boolean disableCache = false // Disable all cache
    int cache = 0 // time in seconds to keep cache (for GET)
    boolean clearCache = false // if true, will clear cache on table update

    protected Connector dbConnector
    protected String table = ""
    protected int last_id = 0
    protected Query queryBuilder = null
	//Setter / Getters
	Map<String, List<String>> priKeys = [:]

    /////////////////////////// Constructors /////////////////////////////
    DB(Connector connector) {
		dbConnector = connector
    }

    JDBC getJdbc() {
		return dbConnector?.jdbc ?: new Dummy()
	}

    ////////////////////////// Interfaces ////////////////////////////////

	static interface Connector {
        String getName()
		boolean open()
		boolean close()
        boolean isOpen()
		Statement prepare(Query query, boolean silent)
		void onError(Throwable ex)
		JDBC getJdbc()
        long getLastUsed()
        void setLastUsed(long milliseconds)
        List<String> getTables()
        List<ColumnInfo> getColumns(String table)
	}

	static interface Statement {
		boolean next()
		void close()
		int columnCount()
		int firstColumn()
        int updatedCount()
		ColumnType columnType(int index)
		String columnName(int index)
		String columnStr(int index)
		Integer columnInt(int index)
		Double columnDbl(int index)
        Float columnFloat(int index)
		LocalDateTime columnDate(int index)
		byte[] columnBlob(int index)
		boolean isColumnNull(int index)
	}

    ////////////////////////// Public ////////////////////////////////

	/**
	 * Reconnect in case it is not connected
	 */
	boolean openIfClosed() {
        boolean isopen = opened
		if(!isopen) {
			Log.v( "Connecting...")
            isopen = dbConnector.open()
		}
        return isopen
	}

    /**
     * Returns all possible records
	 * @return 
     **/
    Data get() {
        query.setAction(SELECT)
        return execGet()
    }
    /**
     * Returns a single row based in an INT id
	 * @param id
	 * @return 
     **/
    Data get(Object id) {
        autoSetKeys()
        query.setAction(SELECT).setWhere(id)
        return execGet()
    }
    /**
     * Get a raw query
     * @param query
     * @param args
     * @return
     */
    Data getSQL(String query, List args = []) {
        queryBuilder = new Query(query, args)
        return execGet()
    }
    /**
     * Returns rows which matches specified ids
	 * @param ids
	 * @return 
     **/
    Data get(List ids) {
        return get(ids as Object)
    }
    /**
     * Returns rows based in criteria (key : value)
	 * @param keyvals
	 * @return 
     **/
    Data get(Map keyvals) {
        return get(keyvals as Object)
    }

    /**
     * Get all tables in database
     * @return
     */
    List<String> getTables() {
        List<String> list = []
        if(openIfClosed()) {
            String tablesQuery = jdbc.getTablesQuery()
            if (!tablesQuery.empty) {
                list = getSQL(tablesQuery).toList().collect { it.toString() }
            } else {
                list = dbConnector.tables
            }
        }
        return list
    }
    /**
     * Update data (Map) where ID is an int with specified value
	 * @param updvals
	 * @param id
	 * @return 
     **/
    boolean update(Map updvals, Object id) {
        autoSetKeys()
        query.setAction(UPDATE).setValues(updvals).setWhere(id)
        return execSet()
    }

    /**
     * Update data (Map) where IDs is in a list of IDs
     * @param updvals : Key => value
     * @param ids : list of IDs to update
     * @return true on success
     */
    boolean update(Map updvals, List ids) {
        return update(updvals, ids as Object)
    }

    /**
     * Update data (Map) where criteria matches.
     * @param updvals : Key => value
     * @param keyvals : Criteria key => value
     * @return true on success
     */
    boolean update(Map updvals, Map keyvals) {
        return update(updvals, keyvals as Object)
    }
    /**
     * Update data like:
     * update([
     *    14 : [ name : "Jennifer" ],
     *    29 : [ name : "Paul" ]
     * ])
     * @param updvals
     * @return
     **/
    boolean update(Map<Object, Map> updvals) {
        return updvals.every {
            boolean ok = false
            switch(it.key) {
                case String : ok = update(it.value, it.key.toString()); break
                case Integer: ok = update(it.value, it.key as int); break
            }
            return ok
        }
    }
    /**
     * Inserts row using key => values
	 * @param insvals
	 * @return 
     **/
    boolean insert(Map insvals) {
        query.setAction(INSERT).setValues(insvals)
        return execSet()
    }
    /**
     * Inserts multiple rows using List(Map).
	 * @param insvals
	 * @return 
     **/
    boolean insert(List<Map> insvals) {
		boolean ok = true
		for(Map<String, Object> row: insvals) {
	        ok = insert(row)
			if(!ok) {
				break
			}
		}
		return ok
    }
    /**
     * Upsert row using key => values
     * @param vals
     * @param id
     * @return
     **/
    boolean replace(Map vals) {
        autoSetKeys()
        query.setAction(REPLACE).setValues(vals)
        return execSet()
    }
    /**
     * Upsert data using List<Map>
     * @param repvals
     * @return
     **/
    boolean replace(List<Map> repvals) {
        return repvals.every {
            replace(it)
        }
    }
    /**
     * Delete a single ID
	 * @param id
	 * @return 
     */
    boolean delete(int id) {
        return delete([id])
    }
    /**
     * Delete a multiple ids as arguments
     * @param id
     * @return
     */
    boolean delete(int... ids) {
        return delete(Arrays.asList(ids))
    }

    /**
     * Delete rows which IDs are contained in a String array.
     * @param ids
     * @return true on success
     */
    boolean delete(String... ids) {
        return delete(Arrays.asList(ids))
    }

    /**
     * Performs a data deletion using an array of ids
     * @param ids
     * @return true on success
     */
    boolean delete(List ids) {
        autoSetKeys()
        query.setAction(DELETE).setWhere(ids)
        return execSet()
    }

    /**
     * Performs a data deletion using key => values pairs
     *
     * Example:
     * hm = new Map()
     * hm.put("year","2011")
     * hm.put("country","JP")
     * .delete(hm)
     *
     * will produce: DELETE FROM [table] WHERE `year` = 2011 AND `country` = "JP"
     *
     * @param keyvals
     * @return true on success
     */
    boolean delete(Map keyvals) {
        query.setAction(DELETE).setWhere(keyvals)
        return execSet()
    }
    boolean truncate() {
        boolean ok = false
        if(table) {
            Log.i("Truncating table: " + table)
            query.setAction(TRUNCATE)
            ok = execSet()
        } else {
            Log.w("Can not truncate: No table specified")
        }
        return ok
    }
    /** Drops the current table
	 * @return true on success **/
    boolean drop() {
        boolean ok = false
        if(table) {
            Log.i("Dropping table: " + table)
            query.setAction(DROP)
            ok = execSet()
        } else {
            Log.w("Can not drop: No table specified")
        }
        return ok
    }
    /**
     * Drop all tables in database
     * @return
     */
    boolean dropAllTables() {
        boolean ok = tables.any {
            ! table(it).drop()
        }
        return ok
    }
    /**
     * Create database
     * @return
     */
    boolean createDatabase() {
        Log.i("Creating database")
        return setSQL(jdbc.createDatabaseQuery)
    }
    /**
     * Drop database
     * @return
     */
    boolean dropDatabase() {
        Log.i("Dropping database")
        return setSQL(jdbc.dropDatabaseQuery)
    }
    //------------------------------ RAW set -------------------------------
    /** Executes a query with arguments directly
     * @param query
     * @param args
     * @return true on success **/
    boolean setSQL(String query, List args = []) {
        queryBuilder = new Query(query, args)
        return execSet()
    }
    /**
     * Execute multiple queries (no arguments)
     * @param queries
     * @return
     */
    boolean setSQL(List<String> queries) {
        return queries.every { return setSQL(it)}
    }
    /**
     * Execute multiple queries with arguments
     * @param queriesWithArgs
     * @return
     */
    boolean setSQL(Map<String, List> queriesWithArgs) {
        return queriesWithArgs.every { return setSQL(it.key, it.value )}
    }
    // -------------------------------- Tools -------------------------------
    /** Get last inserted ID (a INTEGER PRIMARY KEY column must exists)
	 * @return last inserted id **/
    int getLastID() {
        int ilast = last_id
        last_id = 0 //Prevent it to get it twice
        return ilast
    }
    /** Checks if a table exists or not
	 * @return boolean **/
    boolean exists() {
		Log.v( "Checking if table exists...")
        boolean exists = false
        if(table) {
            exists = tables.contains(table)
        }
        return exists
    }
    /** Get Table information:
     *  example:
     *      position, column, type, length, default, nullable, primary, comment
	 * @return  **/
    List<ColumnInfo> info() {
        List<ColumnInfo> columns
        String infoSQL = jdbc.getInfoQuery(table)
        if(infoSQL) {
            columns = getSQL(infoSQL).toListMap().collect {
                new ColumnInfo(
                    position: (it.position ?: 0) as int,
                    name: it.column?.toString() ?: "",
                    nullable: ((it.nullable ?: 0) as int) == 1,
                    unique: ((it.unique ?: 0) as int) == 1,
                    primaryKey: ((it.primary ?: 0) as int) == 1,
                    autoIncrement: ((it.autoinc ?: 0) as int) == 1
                )
            }
        } else {
            columns = dbConnector.getColumns(table)
        }
        return columns
    }
    /**
     * Shortcut to get only the information about a column
     * @param column
     * @return
     */
    ColumnInfo info(String column) {
        return info().find { it.name == column }
    }
    /** Quit **/
    boolean close() {
		Log.v( "Closing connection...")
    	return dbConnector.close()
    }
    /**
     * Return true if connection is closed
     * @return
     */
    boolean isClosed() {
        return ! dbConnector.isOpen()
    }
    /**
     * Return true if connection is opened
     * @return
     */
    boolean isOpened() {
        return dbConnector.isOpen()
    }

    Data get(Query q) {
        if(q) {
            queryBuilder = q
        }
        return execGet()
    }

    /** Execute a Query **/
    boolean set(Query q) {
        if(q) {
            queryBuilder = q
        }
        return execSet()
    }

    /**
     * Clear all keys in cache associated with current table
     */
    void clearCache() {
        dataCache.keys().findAll { it.startsWith(query.tableStr + ".") }.each {
            dataCache.del(it)
        }
    }

    //------------------- Fluent interfaces ----------------

    /**
     * Sets fields to the query:
     * e.g: .field("mycol")
	 * @param field
	 * @return 
     */
    DB field(String field) {
        List<String> afields = []
        afields.add(field)
        return fields(afields)
    }
    /**
     * Sets fields using a comma separated string
     * e.g.: .fields("col1,col2")
	 * @param fields
	 * @return
     **/
    DB fields(String fields_str) {
        def afields = fields_str.split(',')
        return fields(afields)
    }

    /**
     * Sets fields based in a String Array
	 * @param fields
	 * @return 
     */
    DB fields(String[] fields_arr) {
        List<String> afields = Arrays.asList(fields_arr)
        return fields(afields)
    }

    /**
     * Sets fields using an List
	 * @param fields
	 * @return 
     */
    DB fields(List<String> fields_arr) {
        query.setFields(fields_arr)
        return this
    }

	/**
	 * Sets a custom where with params, form example:
	 * @param queryPart
	 * @param params
	 * @return 
	 * @example : .where("mydate" > ?, somedate.toString())
	 */
	DB where(String queryPart, Object...params) {
		query.setWhere(queryPart, params)
		return this
	}
	/**
	 * Sets a custom where with params, form example:
	 * @param queryPart
	 * @param list
	 * @return 
	 * @example : .where("mydate" > ?, somedate.toString())
	 */
	DB where(String queryPart, List<Object> list) {
		query.setWhere(queryPart, list.toArray())
		return this
	}

    /**
     * Specify the table to be used
	 * @param tbl
	 * @return 
     */
    DB table(String tbl) {
        table = tbl
        query.setTable(tbl)
        return this
    }

    /**
     * Specify or modify the key to use as PK (for the query)
	 * @param key
	 * @return 
     */
    DB key(String key) {
        List<String> akeys = []
        if(key) {
            akeys.add(key)
        }
        return keys(akeys)
    }

    /**
     * Specify keys for the query (multiple keys)
	 * @param keys
	 * @return 
     */
    DB keys(List<String> keys) {
        if(!keys.empty) {
            query.setKeys(keys)
        }
        if(keys.size() > 1) {
            Log.w("Multiple keys is not yet supported, use a single key.")
        }
        return this
    }

    /**
     * If set, the result of get(...) will be the count of rows
	 * @return 
     */
    DB count() {
        query.setFieldsType(COUNT)
        return this
    }

    /**
     * Count using a column (useful if null are expected)
	 * @param column
	 * @return 
     */
    DB count(String column) {
        query.setFieldsType(COUNT)
        return fields(column)
    }
    /**
     * Returns the maximum value in a table
	 * @param column
	 * @return 
     **/
    DB max(String column) {
        query.setFieldsType(MAX)
        return fields(column)
    }
    /**
     * Returns the minimum value in a table
	 * @param column
	 * @return 
     **/
    DB min(String column) {
        query.setFieldsType(MIN)
        return fields(column)
    }
    /**
     * Returns the average value in a table
	 * @param column
	 * @return 
     **/
    DB avg(String column) {
        query.setFieldsType(AVG)
        return fields(column)
    }
    /**
     * Set Order by
     * e.g. : .order("mycol", Query.SortOrder.DESC)
	 * @param column
	 * @param order
	 * @return 
     **/
    DB order(String column, Query.SortOrder order) {
        query.setOrder(column, order)
        return this
    }
    /**
     * Set Order by multiple columns
     * e.g. : .order([
     *          mycol1 : Query.SortOrder.DESC
     *          mycol2 : Query.SortOrder.ASC
     *        ])
     * @param column
     * @param order
     * @return
     **/
    DB order(Map<String, Query.SortOrder> order) {
        query.setOrder(order)
        return this
    }
    /**
     * Set Limit
     * e.g. : .limit(10)
	 * @param limit
	 * @return 
     **/
    DB limit(int limit) {
        query.setLimit(limit)
        return this
    }
    /**
     * Set Limit
     * e.g. : .limit(10, 100)
	 * @param limit
	 * @param offset
	 * @return 
     **/
    DB limit(int limit, int offset) {
        query.setLimit(limit).setOffset(offset)
        return this
    }
    /**
     * Set Group By
     * e.g. : .group("year")
	 * @param column
	 * @return 
     **/
    DB group(String column) {
        query.setGroupBy(column)
        return this
    }
    ////////////////////////// Private ////////////////////////////////

    /**
     * Starts or recycle a query
     * @return Query
     */
    private Query getQuery() {
        if(queryBuilder == null) {
            queryBuilder = new Query(jdbc)
            Log.v("Initializing Query")
            if(!table.isEmpty()) {
                queryBuilder.setTable(table)
            }
        }
        return queryBuilder
    }

    /**
     * Executes Query and retrieves data
     * Last stop for read queries
     * @return Data (List<Map>)
     */
    protected Data execGet() {
        String qryStr = query.toString()
        Data data
        if(! qryStr.empty) {
            Log.v("GET ::: " + qryStr)
            query.args.each {
                Log.v(" --> " + it)
            }
            String cacheKey = cache && query.tableStr ? query.tableStr + "." + (qryStr + query.args?.join(",")).md5() : ""
            data = dataCache.get(cacheKey, {
                // Connect if its not connected
                if (openIfClosed()) {
                    List<Map> rows = []
                    try {
                        Statement st = dbConnector.prepare(query, false)
                        if (st) {
                            queryBuilder = null
                            while (st.next()) {
                                Map row = [:]
                                for (int i = st.firstColumn(); i < st.columnCount() + st.firstColumn(); i++) {
                                    if (!st.isColumnNull(i)) {
                                        String column = jdbc.convertToLowerCase ? st?.columnName(i)?.toLowerCase() : st?.columnName(i)
                                        ColumnType type = st?.columnType(i)
                                        switch (type) {
                                            case TEXT:
                                                row.put(column, st.columnStr(i))
                                                break
                                            case INTEGER:
                                                row.put(column, st.columnInt(i))
                                                break
                                            case FLOAT:
                                                row.put(column, st.columnFloat(i))
                                                break
                                            case DOUBLE:
                                                row.put(column, st.columnDbl(i))
                                                break
                                            case BLOB:
                                                row.put(column, st.columnBlob(i))
                                                break
                                            case DATE:
                                                row.put(column, st.columnDate(i))
                                                break
                                            default:
                                                Log.w("Type was NULL")
                                                break
                                        }
                                    }
                                }
                                rows.add(row)
                            }
                            st?.close()
                        }
                    } catch (Exception e) {
                        dbConnector.onError(e)
                    }
                    return new Data(rows)
                } else {
                    Log.e("Unable to read from server")
                    dbConnector.onError(new ConnectException())
                    return null
                }
            }, disableCache ? 0 : cache)
        } else {
            data = new Data([])
        }
        return data
    }

    /**
     * Executes Query (Final stop for write queries)
     * @return true on success
     */
    protected boolean execSet() {
		boolean ok = false
        Map<String,Object> replaceData = [:]
        query.isSetQuery = true
        String qryStr = query.toString()
        if(! qryStr.empty) {
            if (openIfClosed()) {
                Log.v("SET ::: " + qryStr)
                query.args.each {
                    Log.v(" --> " + it)
                }
                if (query.actionType == REPLACE && !jdbc.supportsReplace) {
                    if (query.key) {
                        replaceData = query.whereValues
                        Map allBut = replaceData.findAll { it.key != query.key }
                        Object id = replaceData.get(query.key)
                        queryBuilder = new Query(jdbc, UPDATE).setTable(query.tableStr)
                            .setKeys(query.keys).setValues(allBut).setWhere(id)
                        queryBuilder.isSetQuery = true
                        Log.v("SET ::: " + queryBuilder.toString())
                        queryBuilder.args.each {
                            Log.v(" --> " + it)
                        }
                        // query will import queryBuilder
                    } else {
                        Log.w("Unable to find key when emulating REPLACE command in table: %s", query.tableStr)
                    }
                }
                if (cache && clearCache && query.tableStr) {
                    clearCache()
                }
                Statement st
                try {
                    boolean upsert = ! replaceData.isEmpty()
                    boolean silent = upsert
                    st = dbConnector.prepare(query, silent)
                    if (upsert && st.updatedCount() == 0) {
                        try {
                            // Copy query:
                            Query insert = Query.copyOf(query, INSERT)
                            // With original args:
                            insert.whereValues = replaceData
                            // If its autoincrement, remove the insert value
                            if(info(insert.key).autoIncrement) {
                                int pki = insert.whereValues.keySet().toList().indexOf(insert.key)
                                if (pki >= 0) {
                                    insert.args.remove(pki)
                                    insert.whereValues.remove(insert.key)
                                }
                            }
                            Log.v("SET ::: " + insert.toString())
                            insert.args.each {
                                Log.v(" --> " + it)
                            }
                            st = dbConnector.prepare(insert, false)
                        } catch (Exception e2) {
                            Log.e("Query Syntax error: ", e2)
                        }
                    }
                } catch (Exception e) {
                    Log.e("Query Syntax error: ", e)
                }
                if (st != null) {
                    try {
                        st.next()
                        if (query.isIdentityUpdate) {
                            String id = st.columnStr(1)
                            if (id && id.isNumber()) {
                                last_id = st.columnInt(1)
                            } else {
                                if(! st.isColumnNull(1)) {
                                    Log.d("Received last id: %s", id)
                                }
                                last_id = 0
                            }
                            if (!last_id && jdbc.getLastIdQuery(query.table)) {
                                Log.d("Last ID not found. Using fallback method...")
                                String table = query.table
                                queryBuilder = new Query(jdbc, LASTID)
                                queryBuilder.table = table
                                if (queryBuilder) {
                                    last_id = execGet().toInt()
                                    Log.d("Fallback method returned [%d] as last id", last_id)
                                }
                            }
                            if (!last_id) {
                                Log.d("Last ID was not found in table (does it has identity/autoincrement field?): %s", table)
                            }
                        }
                        ok = true
                    } catch (Exception e) {
                        Log.e("%s failed. ", query.getAction().name(), e)
                    }
                    st.close()
                }
                queryBuilder = null
            } else {
                Log.e("No changes done: database is not open")
                dbConnector.onError(new ConnectException())
            }
        }
        return ok
    }

    /**
     * Set Primary Keys automatically if they are not set
     */
    void autoSetKeys() {
        if(query.keyList.empty) {
            searchPriKeys()
            if(priKeys.containsKey(table)) {
                keys(priKeys[table])
            }
        }
    }
    /**
     * Search and set PKs for tables in the database
     */
    void searchPriKeys() {
        boolean ok = false
        if(openIfClosed()) {
            if (!priKeys.containsKey(table)) {
                List<ColumnInfo> info = info()
                List<ColumnInfo> pks = info.findAll {
                    it.primaryKey
                }
                if(!pks.empty) {
                    priKeys.put(table, pks.collect { it.name })
                }
            } else {
                ok = true
            }
            if (ok) {
                query.setKeys(priKeys.get(table))
            }
        }
    }

}

