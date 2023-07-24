package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.Query.Action.*

@CompileStatic
/**
 * Generates a Query based on rules
 * Created by A.Lepe on 16/11/11.
 */
class Query {
    // Action type (RAW is default)
    static enum Action {
        RAW, SELECT, UPDATE, INSERT, REPLACE, DELETE, TRUNCATE, DROP
    }
    // Field type (NOSET is default)
    static enum FieldType {
        NOSET, COLUMN, MAX, MIN, AVG, COUNT
        def getSQL(String fieldstr) {
            //noinspection GroovyFallthrough
            switch (this) {
                case MAX: fieldstr = "MAX(" + fieldstr + ")"; break
                case MIN: fieldstr = "MIN(" + fieldstr + ")"; break
                case AVG: fieldstr = "AVG(" + fieldstr + ")"; break
                case COUNT: fieldstr = "COUNT(" + fieldstr + ")"; break
                case COLUMN:
                case NOSET:
                default:
                    break
            }
            return fieldstr
        }
    }
    static enum SortOrder {
        ASC, DESC
    }
    static class Part {
        List queries = []
        List data = []
        Part append(String append, List row = []) {
            queries << cleanSQL(append)
            data += row
            return this
        }
        String toString() {
            return queries.join(" AND ")
        }
        boolean isEmpty() {
            return queries.empty
        }
    }

    protected JDBC dbType         = new Dummy()
    protected String queryStr     = ""
    protected String tableStr     = ""
    protected String groupByStr   = ""
    protected Part wherePart      = new Part()
    protected int limitInt        = 0
    protected int offsetInt       = 0
    protected Action actionType   = RAW //Database action for query, like: SELECT, INSERT...
    protected Map<String, SortOrder> sort       = [:]
    protected Map<String, Object> whereValues   = [:]
    protected List<String> fieldList = []
    protected List<String> keyList   = []
    protected List<Object> argList   = []
    protected FieldType fieldType = FieldType.NOSET
    protected boolean isSetQuery        = false // True when we issue a set command in exec_set
    protected boolean isIdentityUpdate  = false
    protected boolean valid             = true // False if there was an invalid request

    Query(final JDBC dbtype, final Action action = RAW) {
        dbType = dbtype
        actionType = action
        setIdentityFlag()
    }
    Query(final String query) {
        queryStr = query
        actionType = RAW
        setIdentityFlag()
    }
    Query(final String query, final Collection args) {
        queryStr = query
        actionType = RAW
        argList = args.toList()
        setIdentityFlag()
    }
    /**
     * Copy a Query object
     * @param query
     * @param action
     * @return
     */
    static Query copyOf(Query query, Action action = null) {
        if(! action) {
            action = query.action
        }
        Query nq = new Query(query.dbType, action)
        nq.with {
            table       = query.table
            whereValues = query.whereValues.collectEntries { [(it.key) : it.value]}
            argList     = query.argList.collect()
            queryStr    = query.queryStr
            tableStr    = query.tableStr
            groupByStr  = query.groupByStr
            limitInt    = query.limitInt
            offsetInt   = query.offsetInt
            sort        = query.sort
            fieldList   = query.fieldList.collect()
            keyList     = query.keyList.collect()
            fieldType   = query.fieldType
            isSetQuery  = query.isSetQuery
            isIdentityUpdate = query.isIdentityUpdate
        }
        return nq
    }

    /**
     * Update identity flag
     */
    protected void setIdentityFlag() {
        boolean isAction = [INSERT, REPLACE].contains(actionType)
        boolean startsWith = queryStr ? ["INSERT", "REPLACE"].any { queryStr.toUpperCase().tokenize(" ").first() == it } : false
        if(isAction || startsWith) {
            isIdentityUpdate = true
        }
    }
    /////////////////////////////////////// SET //////////////////////////////////
    Query setAction(final Action action) {
        actionType = action
        setIdentityFlag()
        return this
    }
    Query setTable(final String table) {
        tableStr = table
        return this
    }

    Query setFields(final Collection<String> fields) {
        fieldList = fields.toList()
        if(fieldType == FieldType.NOSET) {
            fieldType = FieldType.COLUMN
        }
        return this
    }

    Query setFields(final Collection<String> fields, final FieldType type) {
        fieldList = fields.toList()
        fieldType = type
        return this
    }

    Query setFieldsType(final FieldType type) {
        fieldType = type
        return this
    }

    Query setKeys(final Collection<String> keys) {
        keyList = keys.toList()
        return this
    }

    Query setWhere(Object where) {
        List<String> keys = getKeys()
        //noinspection GroovyFallthrough
        switch (where) {
            case null:
                if(keys.size() == 1) {
                    wherePart.append(fieldName(key) + " " + dbType.isNullQuery)
                } else {
                    Log.w("Multiple columns were defined as Primary Key but a single value was passed (null). Query may fail.")
                }
                break
            case String:
                if(where.toString().contains("?")) {
                    def params = []
                    return setWhere(where.toString(), params)
                }
                if(keys.size() == 1) {
                    wherePart.append(fieldName(key) + " = ? ", [where])
                } else {
                    Log.w("Multiple columns were defined as Primary Key but a single value was passed (%s). Query may fail.", where.toString())
                }
                break
            case Collection:
                if(! keys.empty) {
                    if(keys.size() == 1) {
                        if ((where as List).size() == 1) {
                            wherePart.append(fieldName(key) + " = ?", (where as List))
                        } else {
                            if((where as List).empty) {
                                Log.w("Passed an empty collection as argument.")
                                valid = false
                                wherePart.append("IN (<< HERE >>)")
                            } else {
                                String marks = where.collect { '?' }.join(",")
                                wherePart.append(fieldName(key) + " IN (" + marks + ")", (where as List))
                            }
                        }
                    } else {
                        if((where as List).empty) {
                            Log.w("Values used as parameter were not found for key(s): %s in table %s", keys.join(","), table)
                            valid = false
                            wherePart.append("(<< HERE >>)")
                        }  else {
                            if((where as List).first() instanceof Collection) {
                                wherePart.append(
                                    where.collect { "(" + keys.collect { fieldName(it) + " = ? " }.join(" AND ") + ")" }.join(" OR "),
                                    (where as List).flatten() as List
                                )
                            } else {
                                wherePart.append(keys.collect { fieldName(it) + " = ? " }.join(" AND "), (where as List))
                            }
                        }
                    }
                } else {
                    Log.w("Primary key was not set for table: %s", table)
                }
                break
            case Map:
                (where as Map<String, Object>).each {
                    String k, Object v ->
                        if(v == null) {
                            wherePart.append(fieldName(k) + " " + dbType.isNullQuery)
                        } else if(v instanceof Boolean) {
                            if(dbType.supportsBoolean) {
                                wherePart.append(fieldName(k) + " = " + v.toString().toUpperCase())
                            } else {
                                wherePart.append(fieldName(k) + " = ? ", [v.toString()])
                            }
                        } else {
                            wherePart.append(fieldName(k) + " = ? ", [v])
                        }
                }
                break
            case LocalDateTime:
            case LocalDate:
            case LocalTime:
                if(! dbType.supportsDate) {
                    Log.w("Database doesn't supports dates. Converting it to string using default format (results may not be what expected).")
                    switch (where) {
                        case LocalDateTime  : where = (where as LocalDateTime).YMDHms; break
                        case LocalDate      : where = (where as LocalDate).YMD; break
                        case LocalTime      : where = (where as LocalTime).HHmmss; break
                    }
                }
            default:
                if(keys.empty) {
                    Log.w("Primary key was not set for table: %s", table)
                } else {
                    wherePart.append(keys.collect { fieldName(it) + " = ? " }.join(" AND "), where instanceof Collection ? (where as List) : [where])
                }
                break
        }
        return this
    }

	Query setWhere(final String where, final ... params) {
		int param_count = where.length() - where.replace("?", "").length()
		if(param_count == params.length) {
            wherePart.append(where, Arrays.asList(params))
		} else {
			Log.e( "Parameters specified doesn't match arguments count")
		}
		return this
	}

    /* TODO:
    Query setJoin(String table, String... columns) {
        return this
    }
    Query setLeft(String table, String... columns) {
        return this
    }
    Query setRight(String table, String... columns) {
        return this
    }
    Query setJoin(String table, Map relation) {
        return this
    }
    Query setLeft(String table, Map relation) {
        return this
    }
    Query setRight(String table, Map relation) {
        return this
    }
    */
    /**
     * Get the fields part for an INSERT statement
     * @return
     */
    protected Part getInsertFieldsPart() {
        String inspre = whereValues.collect { fieldName(it.key) }.join(",")
        return new Part().append("(" + inspre + ")")
    }
    /**
     * Identifies if value is boolean
     * @param value
     * @return
     */
    protected boolean isBoolean(Object value) {
        return dbType.supportsBoolean && ["true","false"].contains(value.toString().toLowerCase())
    }
    /**
     * Prepare '?' characters for values
     * @param value
     * @return
     */
    protected String getPlaceHolder(Object value) {
        String ph = "?"
        switch (true) {
            case value == null:
                ph = "NULL"
                break
            case isBoolean(value):
                ph = value.toString().toUpperCase()
                break
        }
        return ph
    }
    protected Part getInsertPart() {
        String inspst = whereValues.collect { getPlaceHolder(it.value) }.join(",")
        return new Part().append("VALUES (" + inspst + ")", whereValues.values().toList().findAll {
            it != null &&! isBoolean(it)
        })
    }
    protected Part getUpdatePart() {
        String updstr = whereValues.collect { fieldName(it.key) + " = " + getPlaceHolder(it.value) }.join(",")
        return new Part().append(updstr, whereValues.values().toList().findAll {
            it != null &&! isBoolean(it)
        })
    }

    Query setValues(final Map<String,Object> values) {
        whereValues = values.collectEntries {
            Object val = (it.value instanceof Boolean &&! dbType.supportsBoolean) ? it.value.toString() : it.value
            return [(it.key) : val ]
        }
        return this
    }

    Query setLimit(int limit) {
        limitInt = limit
        return this
    }

    Query setOffset(int offset) {
        offsetInt = offset
        return this
    }

    Query setOrder(final Map<String,SortOrder> orderPair) {
        sort = orderPair.collectEntries {
            [(fieldName(it.key)): it.value]
        } as Map<String, SortOrder>
        return this
    }

    Query setOrder(final String column,final  SortOrder order) {
        sort[fieldName(column)] = order //This will also allow chained commands like: .setOrder(x,y).setOrder(v,w)
        return this
    }

    Query setGroupBy(final String column) {
        groupByStr = column
        return this
    }

    /////////////////////////////////////// GET //////////////////////////////////
    Action getAction() {
        return actionType
    }
    List<String> getKeys() {
        if(keyList.isEmpty()) {
            Log.w( "Keys were not set in table: %s", table)
        }
        return keyList
    }
    // Returns the first specified key
    String getKey() {
        List<String> ks = getKeys()
        return ks &&! ks.empty ? ks.first() : ""
    }
    String getFields() {
        String fieldstr = ""
        if(fieldList.isEmpty()) {
            fieldstr = "*"
        } else {
            fieldList.each {
                fieldstr += (fieldstr.isEmpty() ? "" : ',') + fieldName(it)
            }
        }
        return fieldType.getSQL(fieldstr)
    }
    String getTable() {
        return tableName(tableStr)
    }
    String getWhere() {
        return wherePart.empty ? "" : "WHERE " + wherePart.toString()
    }
    String getGroupBy() {
        return groupByStr.empty ? "" : "GROUP BY "+fieldName(groupByStr)
    }

    List<Object> getArgs() {
        List args = []
        //noinspection GroovyFallthrough
        switch (actionType) {
            case RAW:       args = argList; break

            case SELECT:
            case DELETE:    args = wherePart.data; break

            case INSERT:
            case REPLACE:   args = insertPart.data; break

            case UPDATE:    args = updatePart.data + wherePart.data; break
        }
        if(dbType.supportsBoolean) {
            args = args.collect {
                Object arg = it
                switch (arg) {
                    case String:
                        switch (it.toString().toLowerCase()) {
                            case 'true':
                                arg = true
                                break
                            case 'false':
                                arg = false
                                break
                        }
                        break
                }
                return arg
            }
        }
        return args
    }

    /**
     * Build SQL Query. Here, we get the specific SQL for different databases
     * @return
     */
	@Override
    String toString() {
        String squery = ""
        boolean unknown = false
        switch (actionType) {
            case RAW: squery = queryStr; break
            case SELECT: squery = dbType.getSelectQuery(fields, table, where, groupBy, sort, offsetInt, limitInt); break
            case INSERT: squery = dbType.getInsertQuery(table, insertFieldsPart.toString() + " " + insertPart.toString()); break
            case REPLACE: squery = dbType.getReplaceQuery(table, insertFieldsPart.toString() + " " + insertPart.toString()); break
            case UPDATE: squery = dbType.getUpdateQuery(table, updatePart.toString(), where); break
            case DELETE: squery = dbType.getDeleteQuery(table, where); break
            case TRUNCATE: squery = dbType.getTruncateQuery(table); break
            case DROP: squery = dbType.getDropTableQuery(table); break
            default:
                //WARN: unknown action
                Log.w("Unknown action: %s", actionType)
                unknown = true
                break
        }
        if (!valid) {
            Log.w("Query: [%s] was invalid, not executed.", squery)
            squery = ""
        } else if(!unknown && squery.empty) {
            Log.w("Query was empty for action: %s. Perhaps the implementation doesn't support that action yet?", actionType)
        }
        return squery
    }

    /**
     * Returns the table name with special quotes if needed
     * @param str
     * @return
     */
    private String tableName(final String str) {
        return fieldName(str, true)
    }

    /**
     * Returns column or table name clean and with ``
     */
    private String fieldName(final String str, boolean tableName = false) {
        String result = str.toLowerCase().replaceAll("/[^a-z0-9._]/", "")
        String ch = tableName ? dbType.tablesQuotation : dbType.fieldsQuotation
        return ch + result + ch
    }
	/**
	 * Clean a SQL query removing invalid characters like unicode, comments, semicolon, etc
	 */
	private static String cleanSQL(final String sql) {
		return sql.replaceAll("/[^a-z0-9._()><=?%+*/-`\"']/","")
	}
}
