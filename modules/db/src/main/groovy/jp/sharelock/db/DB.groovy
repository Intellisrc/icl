package jp.sharelock.db

import jp.sharelock.etc.Log
import static jp.sharelock.db.DB.ColumnType.*

@groovy.transform.CompileStatic
/**
 * This class uses an interface of Connector to
 * interact with the Database
 * @author A.Lepe (lepe@support.ne.jp)
 * @since 2016-10
 */
class DB {
    private final String LOG_TAG = DB.getSimpleName()
    private Connector db
    private String table = ""
    private int last_id = 0
    private Query query = null
	//Setter / Getters
	Map<String, List<String>> priKeys = [:]

    /////////////////////////// Constructors /////////////////////////////
    DB(Connector connector) {
		this.db = connector
		init()
    }

	DBType getType() {
		return db?.getType() ?: DBType.DUMMY
	}

	enum DBType {
		DUMMY, SQLITE, MYSQL, POSGRESQL, JAVADB, ORACLE, DB2, SUN
	}

	enum ColumnType {
		TEXT, INTEGER, DOUBLE, BLOB, DATE, NULL
	}
    ////////////////////////// Interfaces ////////////////////////////////

	interface Connector {
		void open()
		void close()
        boolean isOpen()
		Statement prepare(Query query)
		void onError(Exception ex)
		DBType getType()
        long getLastUsed()
        void setLastUsed(long milliseconds)
	}

	interface Statement {
		boolean next()
		void close()
		int columnCount()
		int firstColumn()
		ColumnType columnType(int index)
		String columnName(int index)
		String columnStr(int index)
		Integer columnInt(int index)
		Double columnDbl(int index)
		Date columnDate(int index)
		byte[] columnBlob(int index)
		boolean isColumnNull(int index)
	}

    ////////////////////////// Public ////////////////////////////////

	/**
	 * Check if its not connectedb.
	 */
	private void init() {
		openIfClosed()
	}

	/**
	 * Reconnect in case it is not connected
	 */
	void openIfClosed() {
		if(!db.isOpen()) {
			Log.d(LOG_TAG, "Connecting...")
			db.open()
		}
	}

    /**
     * Returns all possible records
	 * @return 
     **/
    Data get() {
        getQuery().setAction(Query.Action.SELECT)
        return exec_get()
    }
    /**
     * Returns a single row based in an INT id
	 * @param id
	 * @return 
     **/
    Data get(int id) {
        getQuery().setAction(Query.Action.SELECT).setWhere(id)
        return exec_get()
    }
    /**
     * Returns a single row based in a String id
	 * @param id
	 * @return 
     **/
    Data get(String id) {
        getQuery().setAction(Query.Action.SELECT).setWhere(id)
        return exec_get()
    }
    /**
     * Returns rows which matches specified ids
	 * @param ids
	 * @return 
     **/
    Data get(List ids) {
        getQuery().setAction(Query.Action.SELECT).setWhere(ids)
        return exec_get()
    }
    /**
     * Returns rows based in criteria (key : value)
	 * @param keyvals
	 * @return 
     **/
    Data get(Map keyvals) {
        getQuery().setAction(Query.Action.SELECT).setWhere(keyvals)
        return exec_get()
    }
    /**
     * Update data (Map) where ID is an int with specified value
	 * @param updvals
	 * @param id
	 * @return 
     **/
    boolean update(Map updvals, Integer id) {
        getQuery().setAction(Query.Action.UPDATE).setValues(updvals).setWhere(id)
        return exec_set()
    }
    /**
     * Update data (Map) where ID is a String with specified value
	 * @param updvals
	 * @param id
	 * @return 
     **/
    boolean update(Map updvals, String id) {
        getQuery().setAction(Query.Action.UPDATE).setValues(updvals).setWhere(id)
        return exec_set()
    }

    /**
     * Update data (Map) where IDs is in a list of IDs
     * @param updvals : Key => value
     * @param ids : list of IDs to update
     * @return true on success
     */
    boolean update(Map updvals, List ids) {
        getQuery().setAction(Query.Action.UPDATE).setValues(updvals).setWhere(ids)
        return exec_set()
    }

    /**
     * Update data (Map) where criteria matches.
     * @param updvals : Key => value
     * @param keyvals : Criteria key => value
     * @return true on success
     */
    boolean update(Map updvals, Map keyvals) {
        getQuery().setAction(Query.Action.UPDATE).setValues(updvals).setWhere(keyvals)
        return exec_set()
    }
    /**
     * Inserts row using key => values
	 * @param insvals
	 * @return 
     **/
    boolean insert(Map<String, Object> insvals) {
        getQuery().setAction(Query.Action.INSERT).setValues(insvals)
        return exec_set()
    }
    /**
     * Inserts multiple rows using List(Map).
	 * @param insvals
	 * @return 
     **/
    boolean insert(List<Map<String, Object>> insvals) {
		//TODO prepare..commit
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
     * Delete a single ID
	 * @param id
	 * @return 
     */
    boolean delete(Integer id) {
        getQuery().setAction(Query.Action.DELETE).setWhere(id)
        return exec_set()
    }

    /**
     * Delete rows which IDs are contained in a String array.
     * @param ids
     * @return true on success
     */
    boolean delete(String[] ids) {
        return delete(Arrays.asList(ids))
    }

    /**
     * Performs a data deletion using an array of ids
     * @param ids
     * @return true on success
     */
    boolean delete(List ids) {
        getQuery().setAction(Query.Action.DELETE).setWhere(ids)
        return exec_set()
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
        getQuery().setAction(Query.Action.DELETE).setWhere(keyvals)
        return exec_set()
    }
    /** Drops the current table
	 * @return true on success **/
    boolean drop() {
        Log.w(LOG_TAG, "Dropping table: "+this.table)
        getQuery().setAction(Query.Action.DROP)
        return exec_set()
    }
    //------------------------------ RAW set -------------------------------
    /** Executes a query directly
	 * @param query
	 * @return true on success **/
    boolean set(String query) {
        this.query = new Query(query)
        return exec_set()
    }
    /** Executes a query with arguments directly
	 * @param query
	 * @param args
	 * @return true on success **/
    boolean set(String query, List args) {
        this.query = new Query(query, args)
        return exec_set()
    }
    // -------------------------------- Tools -------------------------------
    /** Get last inserted ID (a INTEGER PRIMARY KEY column must exists)
	 * @return last inserted id **/
    int getLastID() {
        int ilast = this.last_id
        this.last_id = 0 //Prevent it to get it twice
        return ilast
    }
    /** Checks if a table exists or not
	 * @return boolean **/
    boolean exists() {
		Log.d(LOG_TAG, "Checking if table exists...")
		getQuery().setType(getType()).setAction(Query.Action.EXISTS)
        return ! exec_get().isEmpty()
    }
    /** Get Table information
	 * @return  **/
    Data info() {
		Log.d(LOG_TAG, "Getting table information...")
        getQuery().setType(getType()).setAction(Query.Action.INFO)
        return exec_get()
    }

    /** Quit **/
    void close() {
		Log.d(LOG_TAG, "Closing connection...")
    	db.close()
    }

    /** Execute a Query **/
    Data exec(Query q = null) {
        if(q) {
            query = q
        }
        return exec_get()
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
        getQuery().setFields(fields_arr)
        return this
    }

	/**
	 * Sets a custom where with params, form example:
	 * @param query
	 * @param params
	 * @return 
	 * @example : .where("mydate" > ?, somedate.toString())
	 */
	DB where(String query, Object...params) {
		getQuery().setWhere(query, params)
		return this
	}
	/**
	 * Sets a custom where with params, form example:
	 * @param query
	 * @param list
	 * @return 
	 * @example : .where("mydate" > ?, somedate.toString())
	 */
	DB where(String query, List<Object> list) {
		getQuery().setWhere(query, list.toArray())
		return this
	}

    /**
     * Specify the table to be used
	 * @param tbl
	 * @return 
     */
    DB table(String tbl) {
        this.table = tbl
        getQuery().setTable(tbl)
        if(priKeys.isEmpty()) {
            autoSetPriKeys()
        }
        return this
    }

    /**
     * Specify or modify the key to use as PK (for the query)
	 * @param key
	 * @return 
     */
    DB key(String key) {
        List<String> akeys = []
        akeys.add(key)
        return keys(akeys)
    }

    /**
     * Specify keys for the query (multiple keys)
     * //TODO: still not supported
	 * @param keys
	 * @return 
     */
    DB keys(List<String> keys) {
        getQuery().setKeys(keys)
        return this
    }

    /**
     * If set, the result of get(...) will be the count of rows
	 * @return 
     */
    DB count() {
        getQuery().setFieldsType(Query.FieldType.COUNT)
        return this
    }

    /**
     * Count using a column (useful if null are expected)
	 * @param column
	 * @return 
     */
    DB count(String column) {
        getQuery().setFieldsType(Query.FieldType.COUNT)
        return fields(column)
    }
    /**
     * Returns the maximum value in a table
	 * @param column
	 * @return 
     **/
    DB max(String column) {
        getQuery().setFieldsType(Query.FieldType.MAX)
        return fields(column)
    }
    /**
     * Returns the minimum value in a table
	 * @param column
	 * @return 
     **/
    DB min(String column) {
        getQuery().setFieldsType(Query.FieldType.MIN)
        return fields(column)
    }
    /**
     * Returns the average value in a table
	 * @param column
	 * @return 
     **/
    DB avg(String column) {
        getQuery().setFieldsType(Query.FieldType.AVG)
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
        getQuery().setOrder(column, order)
        return this
    }
    /**
     * Set Limit
     * e.g. : .limit(10)
	 * @param limit
	 * @return 
     **/
    DB limit(int limit) {
        getQuery().setLimit(limit)
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
        getQuery().setLimit(limit).setOffset(offset)
        return this
    }
    /**
     * Set Group By
     * e.g. : .group("year")
	 * @param column
	 * @return 
     **/
    DB group(String column) {
        getQuery().setGroupBy(column)
        return this
    }
    ////////////////////////// Private ////////////////////////////////

    /**
     * Starts or recycle a query
     * @return Query
     */
    private Query getQuery() {
        if(query == null) {
            query = new Query()
            Log.d(LOG_TAG,"Initializing Query")
            if(!this.table.isEmpty()) {
                query.setTable(this.table)
            }
        }
        return query
    }

    /**
     * Executes Query and retrieves data
     * Last stop for read queries
     * @return Data (List<Map>)
     */
    private Data exec_get() {
        Data data = null
        openIfClosed()
        if(db.isOpen()) {
            Log.d(LOG_TAG, "GET ::: " + query.toString())
            for (Object o : query.getArgs()) {
                Log.d(LOG_TAG, " --> " + o)
            }
            List<Map> rows = []
            try {
                Statement st = db.prepare(query)
                query = null
                while (st.next()) {
                    Map row = [:]
                    for (int i = st.firstColumn(); i < st.columnCount() + st.firstColumn(); i++) {
                        if (!st.isColumnNull(i)) {
                            String column = st?.columnName(i)
                            ColumnType type = st?.columnType(i)
                            switch (type) {
                                case TEXT:
                                    row.put(column, st.columnStr(i))
                                    break
                                case INTEGER:
                                    row.put(column, st.columnInt(i))
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
                                    Log.e(LOG_TAG, "Type was NULL")
                                    break
                            }
                        }
                    }
                    rows.add(row)
                }
                st?.close()
            } catch (Exception e) {
                db.onError(e)
            }
            data = new Data(rows)
        }
        return data
    }

    /**
     * Executes Query (Final stop for write queries)
     * @return true on success
     */
    private boolean exec_set() {
		boolean ok = false
        openIfClosed()
        if(db.isOpen()) {
			Log.d(LOG_TAG, "SET ::: " + query.toString())
			query.getArgs().each {
				Object it ->
					Log.d(LOG_TAG, " --> " + it)
			}
            Statement st
            try {
                st = db.prepare(query)
            } catch (e) {
                Log.e(LOG_TAG, "Query Syntax error: "+e)
            }
            if(st != null) {
                try {
                    st.next()
                    if (query.getAction() == Query.Action.INSERT) {
                        query = new Query(Query.Action.LASTID)
                        query.setType(getType())
                        this.last_id = exec_get().toInt()
                    }
                    ok = true
                } catch (e) {
                    Log.e(LOG_TAG, "Insert failed. " + (e))
                }
                st.close()
            }
			query = null
        } else {
            Log.e(LOG_TAG, "No changes done: database is not open")
        }
        return ok
    }

    /**
     * Search and set PKs for tables in the database
     */
    private void autoSetPriKeys() {
        boolean ok = false
        openIfClosed()
        if(!this.priKeys.containsKey(table)) {
            Data info = info()
            List<String> foundPks = []
			info.toArrHash().find {
				Map row ->
                    if(row.containsKey("pk") && Double.parseDouble(row.get("pk").toString()) == 1) { //we use double as it may be: "1.0"
                        if(row.containsKey("name")) {
                            ok = true
                            String name = row.get("name").toString()
                            foundPks.add(name)
                            Log.d(LOG_TAG, "PK Found: "+name)
							return true
                        }
                    }
			}
            if(ok) {
                this.priKeys.put(table, foundPks)
            }
        } else {
            ok = true
        }
        if(ok) {
            getQuery().setKeys(priKeys.get(table))
        }
    }

}

