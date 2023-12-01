package com.intellisrc.db

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.etc.Cache
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentLinkedQueue

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
    static protected Cache<List<ColumnInfo>> colsInfo = new Cache<List<ColumnInfo>>(extend: false, quiet: true)
    static protected ConcurrentLinkedQueue<String> tableList = new ConcurrentLinkedQueue<>()
    static boolean enableCache = Config.get("db.cache", true) // By default, enabled
    static int cache = Config.get("db.cache.get", 0) // time in seconds to keep cache (for GET)
    static boolean clearCache = Config.get("db.cache.clear", false) // if true, will clear cache on table update

    protected Connector dbConnector
    protected String table = ""
    protected int last_id = 0
    // We need a global Query object to allow fluid building
    protected Query queryBuilder = null
    // Flag to mark connections which were returned already
    protected boolean returned = false

    /////////////////////////// Constructors /////////////////////////////
    DB(Connector connector) {
		dbConnector = connector
    }

    JDBC getJdbc() {
		return dbConnector?.jdbc ?: new Dummy()
	}

    ////////////////////////// Public ////////////////////////////////

	/**
	 * Reconnect in case it is not connected
	 */
	boolean openIfClosed() {
        boolean isopen = opened
        if(!isopen) {
            if(returned) {
                Log.w("Connection was previously returned (using db.close()). It might get disconnected unexpectedly.")
                Log.v("If you use a connection after calling close(), it can lead to a sudden disconnection " +
                    "on timeout (from pool). To prevent that from happening you need to call DB.connect() again.")
                Log.stackTrace()
            }
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
    Data getSQL(String query, Collection args = []) {
        queryBuilder = new Query(query, args)
        return execGet()
    }
    /**
     * Returns rows which matches specified ids
	 * @param ids
	 * @return 
     **/
    Data get(Collection ids) {
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
        if(tableList.empty) {
            if (openIfClosed()) {
                String tablesQuery = jdbc.getTablesQuery()
                if (!tablesQuery.empty) {
                    list = getSQL(tablesQuery).toList().collect { it.toString() }
                } else {
                    list = dbConnector.tables
                }
            }
            if(! list.empty && enableCache) {
                tableList.addAll(list)
            }
        } else {
            list = tableList.toList()
        }
        return list
    }

    /**
     * Update data (Map) where ID is the key(s) to use
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
    boolean update(Map updvals, Collection ids) {
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
     * Update multiple rows
     * @param rows
     * @return
     */
    boolean update(Collection<Map> rows, Collection<Object> keyvals) {
        boolean updated = false
        if(! rows.empty) {
            boolean sameSize = rows.size() == keyvals.size()
            if (sameSize) {
                List<Query> queries = rows.withIndex().collect({
                    Map row, int idx ->
                        removeAutoId(autoKeys(createQuery().setAction(UPDATE)).setWhere(keyvals[idx]).setValues(row))
                })
                updated = dbConnector.commit(queries)
                if(!updated) {
                    dbConnector.rollback()
                }
            } else {
                Log.w("Trying to update data with unequal number of rows and keys")
            }
        } else {
            Log.v("Update received an empty list")
        }
        return updated
    }

    /**
     * Inserts row using key => values
	 * @param insvals
	 * @return 
     **/
    boolean insert(Map insvals) {
        autoSetKeys()
        queryBuilder = removeAutoId(query.setAction(INSERT).setValues(insvals))
        return execSet()
    }
    /**
     * Inserts multiple rows using List(Map).
	 * @param insvals
	 * @return
     **/
    boolean insert(Collection<Map> insvalsList) {
        autoSetKeys()
        boolean ok = false
        if(!insvalsList.empty) {
            List<Query> queries = insvalsList.collect({
                removeAutoId(autoKeys(createQuery().setAction(INSERT)).setValues(it))
            })
            ok = dbConnector.commit(queries)
            if (!ok) {
                dbConnector.rollback()
            }
        } else {
            Log.v("Insert received an empty list")
        }
        return ok
    }
    /**
     * Upsert row using key => values (like insert)
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
     * Upsert data using List<Map> (like insert)
     * @param repvals
     * @return
     **/
    boolean replace(Collection<Map> repvals) {
        boolean ok = false
        if(!repvals.empty) {
            if (jdbc.supportsReplace) {
                List<Query> queries = repvals.collect({
                    autoKeys(createQuery().setAction(REPLACE)).setValues(it)
                })
                ok = dbConnector.commit(queries)
                if(!ok) {
                    dbConnector.rollback()
                }
            } else {
                if (repvals.size() > 100 && !jdbc.supportsReplace) {
                    Log.w("Using REPLACE with many records in [%s] may be too slow. Consider using INSERT or UPDATE instead", jdbc.class.simpleName)
                }
                ok = repvals.every {
                    replace(it)
                }
            }
        } else {
            Log.v("Replace received empty list")
        }
        return ok
    }
    /**
     * Upsert data using ID (like update)
     * @param vals
     * @param id
     * @return
     */
    boolean replace(Map vals, Object id) {
        return replace(vals, [id])
    }
    /**
     * Upsert data using a list of ids (like update)
     * @param vals
     * @param ids
     * @return
     */
    boolean replace(Map vals, List ids) {
        Map keyvals
        List pks = getPKs(table)
        if(pks.size() == 1) { // Single pk, single id
            if(ids.size() == 1) {
                keyvals = [(pks.first()) : ids.first()]
            } else { // Handle multiple ids in single pk
                boolean ok = ids.every {
                    replace(vals, [it])
                }
                return ok
            }
        } else if(ids.size() == ids.size()) { // Multiple ids matching pk columns
            keyvals = pks.withIndex().collectEntries { [(it.v1): ids[it.v2]] }
        } else { // Invalid case
            Log.w("Incorrect number of keys passed to REPLACE for table: %s", table)
            return false
        }
        return replace(vals, keyvals)
    }
    /**
     * Upsert data using Map (like update)
     * @param vals
     * @param keyvals
     * @return
     */
    boolean replace(Map vals, Map keyvals) {
        autoSetKeys()
        query.setAction(REPLACE).setValues((vals + keyvals) as Map)
        return execSet()
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
     * Performs a data deletion using an array of ids.
     * The list can be the PK ids in multiple-key tables
     * @param ids
     * @return true on success
     */
    boolean delete(Collection ids) {
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
        autoSetKeys()
        query.setAction(DELETE).setWhere(keyvals)
        return execSet()
    }
    /**
     * Delete all records in a table (basically: DELETE FROM <table>)
     * @return
     */
    boolean clear() {
        Log.i("Clearing all records in table: %s", table)
        query.setAction(DELETE)
        return execSet()
    }
    /**
     * Truncate a table (in some cases it will reset autoincrement ids as well)
     * @return
     */
    boolean truncate() {
        boolean ok = false
        if(table) {
            Log.i("Truncating table: %s", table)
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
            Log.i("Dropping table: %s", table)
            query.setAction(DROP_TABLE)
            ok = execSet()
        } else {
            Log.w("Can not drop: No table specified")
        }
        clearCache()
        return ok
    }
    /** Drops a view
     * @return true on success **/
    boolean dropView() {
        boolean ok = false
        if(table) {
            Log.i("Dropping view : %s", table)
            query.setAction(DROP_VIEW)
            ok = execSet()
        } else {
            Log.w("Can not drop: No table specified")
        }
        clearCache()
        return ok
    }
    /**
     * Drop all tables in database
     * @return
     */
    boolean dropAllTables() {
        boolean ok = tables.every {
            return table(it).drop()
        }
        clearCache()
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
    boolean setSQL(String query, Collection args = []) {
        queryBuilder = new Query(query, args)
        return execSet()
    }
    /**
     * Execute multiple queries (no arguments)
     * @param queries
     * @return
     */
    boolean setSQL(Collection<String> queries) {
        return queries.every { return setSQL(it)}
    }
    /**
     * Execute multiple queries with arguments
     * @param queriesWithArgs
     * @return
     */
    boolean setSQL(Map<String, Collection> queriesWithArgs) {
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
        List<ColumnInfo> columns = []
        if(table) {
            if (colsInfo.contains(jdbc.dbname + "." + table)) {
                columns = colsInfo.get(jdbc.dbname + "." + table) ?: []
            } else {
                String infoSQL = jdbc.getInfoQuery(table)
                if (infoSQL) {
                    columns = getSQL(infoSQL).toListMap().collect {
                        // This allows two different names (in case it might conflict with reserved words)
                        jdbc.fillColumn(new ColumnInfo(
                            position        : (it.position      ?: it.col_pos       ?: 0) as int,
                            name            : (it.column        ?: it.col_name      ?: "").toString(),
                            nullable        : ((it.nullable     ?: it.is_null       ?: 0) as int) == 1,
                            unique          : ((it.unique       ?: it.is_unique     ?: 0) as int) == 1,
                            primaryKey      : ((it.primary      ?: it.is_primary    ?: 0) as int) == 1,
                            autoIncrement   : ((it.autoinc      ?: it.identity      ?: 0) as int) == 1,
                            generated       : ((it.generated    ?: it.is_generated  ?: 0) as int) == 1,
                            type            : (it.type          ?: it.col_type      ?: "NULL").toString().toUpperCase() as ColumnType,
                            length          : (it.length        ?: it.col_length    ?: 0) as int,
                            charLength      : (it.clength       ?: it.char_length   ?: 0) as int,
                            bufferLength    : (it.blength       ?: it.buffer_length ?: 0) as int,
                            decimalDigits   : (it.decimals      ?: it.decimal_digits ?: 0) as int,
                            defaultValue    : it.default        ?: it.default_value ?: "",
                        ), it)
                    }
                } else {
                    if(! tables.collect { it.toLowerCase() }.contains(table.toLowerCase())) {
                        tableList.clear()
                    }
                    if(tables.collect { it.toLowerCase() }.contains(table.toLowerCase())) {
                        columns = dbConnector.getColumns(table)
                        if(columns.empty) {
                            Log.w("Columns were not found in table: %s", table)
                        }
                    } else {
                        Log.v("Table [%s] didn't exists (yet)", table)
                    }
                }
                if (!columns.empty && enableCache) {
                    colsInfo.set(jdbc.dbname + "." + table, columns)
                }
            }
        } else {
            Log.v("Table name was not specified")
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
        returned = true
    	return dbConnector.close()
    }
    /**
     * Return true if connection is closed
     * @return
     */
    boolean isClosed() {
        return returned || !dbConnector.isOpen()
    }
    /**
     * Return true if connection is opened
     * @return
     */
    boolean isOpened() {
        return ! returned && dbConnector.isOpen()
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
    static void clearCache() {
        dataCache.clear()
        colsInfo.clear()
        tableList.clear()
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
    DB fields(Collection<String> fields_arr) {
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
	DB where(String queryPart, Collection<Object> list) {
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
    DB keys(Collection<String> keys) {
        if(!keys.empty) {
            query.setKeys(keys)
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
     * Starts or reuse a query
     * @return Query
     */
    private Query getQuery() {
        if(queryBuilder == null) {
            queryBuilder = createQuery()
        }
        return queryBuilder
    }

    /**
     * Return new Query (do not reuse it)
     * @return
     */
    private Query createQuery() {
        Query q = new Query(jdbc)
        if(!table.isEmpty()) {
            q.setTable(table)
        }
        return q
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
                        ResultStatement st = dbConnector.execute(query, false)
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
                                                String val = st.columnStr(i).trim()
                                                if(["true","false"].contains(val.toLowerCase())) {
                                                    row.put(column, st.columnBool(i))
                                                } else {
                                                    row.put(column, st.columnStr(i))
                                                }
                                                break
                                            case BOOLEAN:
                                                row.put(column, st.columnBool(i))
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
                } else if(returned) {
                    Log.w("Connection was closed (returned to the pool). Unable to execute query: %s", query.queryStr)
                } else {
                    Log.e("Unable to read from server")
                    dbConnector.onError(new ConnectException())
                    return null
                }
            }, enableCache ? cache : 0)
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
                    if (! query.keys.empty) {
                        List keys = query.keys
                        replaceData = query.whereValues
                        Map allBut = replaceData.findAll {
                            ! info(it.key)?.primaryKey
                        }
                        Object id = replaceData.findAll {
                            info(it.key)?.primaryKey
                        }?.collect {
                            it.value
                        }
                        queryBuilder = new Query(jdbc, UPDATE).setTable(query.tableStr)
                        if(id) {
                            queryBuilder.setKeys(keys).setWhere(id)
                        }
                        queryBuilder.setValues(allBut)
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
                ResultStatement st
                try {
                    boolean upsert = ! replaceData.isEmpty()
                    boolean silent = upsert
                    st = dbConnector.execute(query, silent)
                    if (upsert && st && st.updatedCount() == 0) {
                        try {
                            // Copy query:
                            Query insert = Query.copyOf(query, INSERT)
                            // With original args:
                            insert.whereValues = replaceData
                            insert = removeAutoId(insert, true)
                            Log.v("SET ::: " + insert.toString())
                            insert.args.each {
                                Log.v(" --> " + it)
                            }
                            st?.close() // Close previous
                            st = dbConnector.execute(insert, false)
                        } catch (Exception e2) {
                            Log.e("Query Syntax error: ", e2)
                        }
                    }
                } catch (Exception e) {
                    Log.e("Query Syntax error: ", e)
                }
                if (st) {
                    try {
                        st.next()
                        List<String> pks = getPKs()
                        if (query.isIdentityUpdate && pks.size() == 1 && info(pks.first())?.autoIncrement) {
                            String id = st.columnStr(1)
                            if (id && id.isNumber()) {
                                last_id = st.columnInt(1)
                            } else {
                                if(! st.isColumnNull(1)) {
                                    Log.v("Received last id: %s", id)
                                }
                                last_id = 0
                            }
                            String lastIdQuery = jdbc.getLastIdQuery(query.table, pks.first())
                            if (!last_id && lastIdQuery) {
                                Log.v("Last ID not found. Using fallback method...")
                                String table = query.table
                                queryBuilder = new Query(lastIdQuery)
                                queryBuilder.table = table
                                if (queryBuilder) {
                                    last_id = execGet().toInt()
                                    Log.v("Fallback method returned [%d] as last id", last_id)
                                }
                            }
                            if (!last_id) {
                                Log.v("Last ID was not found in table (does it has identity/autoincrement field?): %s", table)
                            }
                        }
                        ok = true
                    } catch (Exception e) {
                        Log.e("%s failed. ", query.getAction().name(), e)
                    }
                    st?.close()
                }
                queryBuilder = null
            } else if(returned) {
                Log.w("Connection was closed (returned to the pool). Unable to execute query: %s", query.queryStr)
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
        autoKeys(query)
    }
    /**
     * Set keys for a Query
     * @param q
     * @return
     */
    Query autoKeys(Query q) {
        if(q.keyList.empty) {
            List<String> pks = getPKs(q.tableStr)
            if(! pks.empty) {
                q.setKeys(pks)
            }
        }
        return q
    }
    /**
     * Get Primary key(s) from table
     * @param table
     * @return
     */
    List<String> getPKs(String tbl = table) {
        info()
        return !colsInfo.isEmpty() ? colsInfo.get(jdbc.dbname + "." + tbl)?.findAll { it.primaryKey }?.collect { it.name } ?: [] : []
    }
    /**
     * Removes AutoID from Query (INSERT)
     * @param qry
     * @return
     */
    protected Query removeAutoId(Query qry, boolean force = false) {
        qry = autoKeys(qry)
        if(info(qry.key)?.autoIncrement) {
            int pki = qry.whereValues.keySet().toList().indexOf(qry.key)
            if (pki >= 0) {
                if(force || qry.whereValues.get(qry.key) as int == 0) {
                    qry.args.remove(pki)
                    qry.whereValues.remove(qry.key)
                }
            }
        }
        return qry
    }
}

