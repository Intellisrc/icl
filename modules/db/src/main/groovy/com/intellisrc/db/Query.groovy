package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.DB.DBType
import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime

import static com.intellisrc.db.DB.DBType.*
import static com.intellisrc.db.Query.Action.*

@CompileStatic
/**
 * Generates a Query based on rules
 * Created by A.Lepe on 16/11/11.
 */
class Query {
    // Action type (RAW is default)
    static enum Action {
        RAW, SELECT, UPDATE, INSERT, REPLACE, DELETE, DROP, INFO, LASTID, EXISTS
    }
    // Field type (NOSET is default)
    static enum FieldType {
        NOSET, COLUMN, MAX, MIN, AVG, COUNT
        def getSQL(String fieldstr) {
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
    protected DBType dbType = DUMMY
    protected String queryStr     = ""
    protected String tableStr     = ""
    protected String groupbyStr   = ""
    protected String whereStr     = ""
    protected String insvalsStr   = ""
    protected String updvalsStr   = ""
    protected int limitInt        = 0
    protected int offsetInt       = 0
    protected Action actionType   = RAW //Database action for query, like: SELECT, INSERT...
    protected Map<String, SortOrder> sort = [:]
    protected List<String> fieldList = []
    protected List<String> keyList = []
    protected List<Object> argList = []
    protected FieldType fieldtype = FieldType.NOSET

    Query() {
        actionType = RAW
    }
    Query(final Action action) {
        actionType = action
    }
    Query(final String query) {
        queryStr = query
        actionType = RAW
    }
    Query(final String query, final List args) {
        queryStr = query
        actionType = RAW
        argList = args
    }

    /////////////////////////////////////// SET //////////////////////////////////
	Query setType(final DBType dbtype) {
		dbType = dbtype
		return this
	}
    Query setAction(final Action action) {
        actionType = action
        return this
    }
    Query setTable(final String table) {
        tableStr = table
        return this
    }

    Query setFields(final List<String> fields) {
        fieldList = fields
        if(fieldtype == FieldType.NOSET) {
            fieldtype = FieldType.COLUMN
        }
        return this
    }

    Query setFields(final List<String> fields,final  FieldType type) {
        fieldList = fields
        fieldtype = type
        return this
    }

    Query setFieldsType(final FieldType type ) {
        fieldtype = type
        return this
    }

    Query setKeys(final List<String> keys) {
        keyList = keys
        return this
    }

    Query setWhere(final Integer where) {
        String key = getKey() //For the moment no multiple keys allowed
        if(key &&! whereStr.contains(sqlName(key))) {
            whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
            argList << where
        }
        return this
    }

    Query setWhere(final String where) {
		if(where.contains("?")) {
			def params = []
			setWhere(where, params)
		} else {
			String key = getKey() //For the moment no multiple keys allowed
            if(key &&! whereStr.contains(sqlName(key))) {
                whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
                argList << where
            }
		}
        return this
    }

	Query setWhere(final String where, final ... params) {
		int param_count = where.length() - where.replace("?", "").length()
		if(param_count == params.length) {
			whereStr += (whereStr.isEmpty() ? "" : " AND ") + cleanSQL(where)
			argList.addAll(Arrays.asList(params))
		} else {
			Log.e( "Parameters specified doesn't match arguments count")
		}
		return this
	}

    Query setWhere(final List where) {
        String key = getKey() //For the moment no multiple keys allowed
        String marks = ""
        if(key &&! whereStr.contains(sqlName(key))) {
            where.each {
                marks += (marks.isEmpty() ? "" : ",") + '?'
                argList << it
            }
            whereStr += sqlName(key) + " IN (" + marks + ")"
        } else {
            Log.w("Where for key: %s already existed.", key)
        }
        return this
    }

    Query setWhere(final LocalDate where) {
        String key = getKey() //For the moment no multiple keys allowed
        if(key &&! whereStr.contains(sqlName(key))) {
            whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
            argList << where
        }
        return this
    }

    Query setWhere(final LocalDateTime where) {
        String key = getKey() //For the moment no multiple keys allowed
        if(key &&! whereStr.contains(sqlName(key))) {
            whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
            argList << where
        }
        return this
    }

    Query setWhere(final Map<String,Object> where) {
        where.each {
            String key, Object val ->
                if(! whereStr.contains(sqlName(key))) {
                    whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
                    argList << val
                }
        }
        return this
    }

    Query setValues(final Map<String,Object> values) {
        String inspre = ""
        String inspst = ""
        String updstr = ""
        values.each {
            String key, Object val ->
                inspre += (inspre.isEmpty() ? "" : ',') + sqlName(key)
                inspst += (inspst.isEmpty() ? "" : ',') + "?"
                updstr += (updstr.isEmpty() ? "" : ',') + sqlName(key) + " = ?"
                argList << val
        }
        insvalsStr = "("+inspre+") VALUES ("+inspst+")"
        updvalsStr = updstr
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
        sort = orderPair
        return this
    }

    Query setOrder(final String column,final  SortOrder order) {
        sort[column] = order //This will also allow chained commands like: .setOrder(x,y).setOrder(v,w)
        return this
    }

    Query setGroupBy(final String column) {
        groupbyStr = column
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
        return getKeys().empty ? null : getKeys().get(0)
    }
    String getFields() {
        String fieldstr = ""
        if(fieldList.isEmpty()) {
            return "*"
        }
        fieldList.each {
            fieldstr += (fieldstr.isEmpty() ? "" : ',') + sqlName(it)
        }
        return fieldtype.getSQL(fieldstr)
    }
    String getTable() {
        return sqlName(tableStr)
    }
    String getWhere() {
        return whereStr.isEmpty() ? "" : " WHERE "+whereStr
    }
    Object[] getArgs() {
        return argList.toArray()
    }
    List<Object> getArgsList() {
        return argList.collect()
    }
	/**
	 * Return args as String array
	 * as Android connector uses only Strings
	 * @return 
	 */
    String[] getArgsStr() {
        String[] array = new String[argList.size()]
		return argList.toArray(array)
	}
    private String getInsertValues() {
        return insvalsStr
    }
    private String getUpdateValues() {
        return updvalsStr
    }
    String getGroupBy() {
        return groupbyStr.isEmpty() ? "" : " GROUP BY "+sqlName(groupbyStr)
    }
    String getSort() {
        def query = ""
        if(sort) {
            sort.each {
                String column, SortOrder order ->
                    query += (query ? "," : "") + sqlName(column) + " " + order.toString()
            }
            query = " ORDER BY $query"
        }
        return query
    }
    String getLimit() {
        String off = offsetInt > 0 ? " OFFSET "+offsetInt : ""
        return limitInt > 0 ? " LIMIT "+limitInt+off : ""
    }

	@Override
    String toString() {
        String squery = ""
        switch(actionType) {
            case RAW:    squery = queryStr
				break
            case SELECT: squery = "SELECT "+getFields()+" FROM "+getTable()+getWhere()+getGroupBy()+getSort()+getLimit()
				break
            case INSERT: squery = "INSERT INTO "+getTable()+getInsertValues()
				break
            case REPLACE: squery = "REPLACE INTO "+getTable()+getInsertValues()
                break
            case UPDATE: squery = "UPDATE "+getTable()+" SET "+getUpdateValues()+getWhere()
				break
            case DELETE: squery = "DELETE FROM "+getTable()+getWhere()
				break
            case DROP:   squery = "DROP TABLE "+getTable()
				break
            case INFO:   
				switch(dbType) {
					case SQLITE:
						squery = "PRAGMA table_info("+getTable()+")";  break
                    case MARIADB:
					case MYSQL:
						squery = "SELECT COLUMN_NAME as 'name', DATA_TYPE as 'type', IF(COLUMN_KEY = 'PRI',1,0) as 'pk' FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"+cleanSQL(tableStr)+"'"; break
					default:
						Log.e("Type [%s] not defined on INFO", dbType)
				}
				break
            case LASTID:
				switch(dbType) {
					case SQLITE:				
						squery = "SELECT last_insert_rowid() as lastid"; break
                    case MARIADB:
					case MYSQL:
						squery = "SELECT LAST_INSERT_ID() as lastid"; break
					default:
						Log.e("Type [%s] not defined on LASTID", dbType)
				}
				break
            case EXISTS: 
				switch(dbType) {
					case SQLITE:				
						squery = "PRAGMA table_info("+getTable()+")"; break
                    case MARIADB:
					case MYSQL:
						squery = "SHOW TABLES LIKE \""+cleanSQL(tableStr)+"\""; break
					default:
						Log.e("Type [%s] not defined on EXISTS", dbType)
				}	
				break
            default :
                //WARN: unknown action
                break
        }
        //clear(); //Prevent garbage values
        return squery
    }

    private String sqlName(final Object obj) {
        return sqlName(obj.toString())
    }
    // Return column or table name clean and with ``
    private String sqlName(final String str) {
        String result = str
        switch(dbType) {
            case SQLITE:
            case MARIADB:
            case MYSQL:
                result = "`" + result.toLowerCase().replaceAll("/[^a-z0-9._]/","") + "`"
                break
            case POSTGRESQL: //No special quotation is required in PostgreSQL
                break
        }
        return result
    }
	/**
	 * Clean a SQL query removing invalid characters like unicode, comments, semicolon, etc
	 */
	private static String cleanSQL(final String sql) {
		return sql.replaceAll("/[^a-z0-9._()><=?%+*/-`\"']/","")
	}
}
