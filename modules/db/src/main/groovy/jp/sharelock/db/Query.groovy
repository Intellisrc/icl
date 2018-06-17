package jp.sharelock.db

import jp.sharelock.etc.Log
import jp.sharelock.db.DB.DBType

import java.time.LocalDateTime

import static jp.sharelock.db.DB.DBType.*
import static jp.sharelock.db.Query.Action.*

@groovy.transform.CompileStatic
/**
 * Generates a Query based on rules
 * Created by A.Lepe on 16/11/11.
 */
class Query {
	private DBType dbType = DUMMY
    // Action type (RAW is default)
    static enum Action {
        RAW, SELECT, UPDATE, INSERT, DELETE, DROP, INFO, LASTID, EXISTS
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
    private String queryStr     = ""
    private String tableStr     = ""
    private String groupbyStr   = ""
    private String whereStr     = ""
    private String insvalsStr   = ""
    private String updvalsStr   = ""
    private int limitInt        = 0
    private int offsetInt       = 0
    private Action actionType   = RAW //Database action for query, like: SELECT, INSERT...
    private Map<String, SortOrder> sort = [:]
    private List<String> fieldList = []
    private List<String> keyList = []
    private List<Object> argList = []
    private FieldType fieldtype = FieldType.NOSET

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
        whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
        argList << where
        return this
    }

    Query setWhere(final String where) {
		if(where.contains("?")) {
			def params = []
			setWhere(where, params)
		} else {
			String key = getKey() //For the moment no multiple keys allowed
			whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
			argList << where
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
        where.each {
            marks += (marks.isEmpty() ? "" : ",") + '?'
            argList << it
        }
        whereStr += sqlName(key) + " IN ("+marks+")"
        return this
    }

    Query setWhere(final LocalDateTime where) {
        String key = getKey() //For the moment no multiple keys allowed
        whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
        argList << where
        return this
    }

    Query setWhere(final Map<String,Object> where) {
        where.each {
            String key, Object val ->
                whereStr += (whereStr.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
                argList << val
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
            Log.e( "Keys were not set")
            keyList << "id" //Generic ID name
        }
        return keyList
    }
    // Returns the first specified key
    String getKey() {
        return getKeys().get(0)
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
					case MYSQL:
						squery = "SELECT COLUMN_NAME as 'name', DATA_TYPE as 'type', IF(COLUMN_KEY = 'PRI',1,0) as 'pk' FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"+cleanSQL(tableStr)+"'"; break
					default:
						Log.e("Type not defined on INFO")
				}
				break
            case LASTID:
				switch(dbType) {
					case SQLITE:				
						squery = "SELECT last_insert_rowid() as lastid"; break
					case MYSQL:
						squery = "SELECT LAST_INSERT_ID() as lastid"; break
					default:
						Log.e("Type not defined on LASTID")
				}
				break
            case EXISTS: 
				switch(dbType) {
					case SQLITE:				
						squery = "PRAGMA table_info("+getTable()+")"; break
					case MYSQL:
						squery = "SHOW TABLES LIKE \""+cleanSQL(tableStr)+"\""; break
					default:
						Log.e("Type not defined on EXISTS")
				}	
				break
            default :
                //WARN: unknown action
                break
        }
        //clear(); //Prevent garbage values
        return squery
    }

    private static String sqlName(final Object obj) {
        return sqlName(obj.toString())
    }
    // Return column or table name clean and with ``
    private static String sqlName(final String str) {
        return "`" + str.toLowerCase().replaceAll("/[^a-z0-9._]/","") + "`"
    }
	/**
	 * Clean a SQL query removing invalid characters like unicode, comments, semicolon, etc
	 */
	private static String cleanSQL(final String sql) {
		return sql.replaceAll("/[^a-z0-9._()><=?%+*/-`\"']/","")
	}
}
