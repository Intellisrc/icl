package jp.sharelock.db

import jp.sharelock.etc.Log
import jp.sharelock.db.DB.DBType
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
    private String query    = ""
    private String table    = ""
    private String groupby  = ""
    private String where    = ""
    private String insvals  = ""
    private String updvals  = ""
    private int limit       = 0
    private int offset      = 0
    private Action action   = RAW //Database action for query, like: SELECT, INSERT...
    private Map<String, SortOrder> sort = [:]
    private List<String> fields = []
    private List<String> keys = []
    private List args = []
    private FieldType fieldtype = FieldType.NOSET

    Query() {
        this.action = RAW
    }
    Query(final Action action) {
        this.action = action
    }
    Query(final String query) {
        this.query = query
        this.action = RAW
    }
    Query(final String query, final List args) {
        this.query = query
        this.action = RAW
        this.args = args
    }

    /////////////////////////////////////// SET //////////////////////////////////
	Query setType(final DBType dbtype) {
		this.dbType = dbtype
		return this
	}
    Query setAction(final Action action) {
        this.action = action
        return this
    }
    Query setTable(final String table) {
        this.table = table
        return this
    }

    Query setFields(final List<String> fields) {
        this.fields = fields
        if(this.fieldtype == FieldType.NOSET) {
            this.fieldtype = FieldType.COLUMN
        }
        return this
    }

    Query setFields(final List<String> fields,final  FieldType type) {
        this.fields = fields
        this.fieldtype = type
        return this
    }

    Query setFieldsType(final FieldType type ) {
        this.fieldtype = type
        return this
    }

    Query setKeys(final List<String> keys) {
        this.keys = keys
        return this
    }

    Query setWhere(final Integer where) {
        String key = getKey() //For the moment no multiple keys allowed
        this.where += (this.where.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
        this.args.add(where)
        return this
    }

    Query setWhere(final String where) {
		if(where.contains("?")) {
			def params = []
			setWhere(where, params)
		} else {
			String key = getKey() //For the moment no multiple keys allowed
			this.where += (this.where.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
			this.args.add(where)
		}
        return this
    }

	Query setWhere(final String where, final ... params) {
		int param_count = where.length() - where.replace("?", "").length()
		if(param_count == params.length) {
			this.where += (this.where.isEmpty() ? "" : " AND ") + cleanSQL(where)
			this.args.addAll(Arrays.asList(params))
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
            this.args.add(it)
        }
        this.where += sqlName(key) + " IN ("+marks+")"
        return this
    }

    Query setWhere(final Map<String,Object> where) {
        where.each {
            String key, Object val ->
                this.where += (this.where.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
                this.args.add(val)
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
            this.args.add(val)
        }
        this.insvals = "("+inspre+") VALUES ("+inspst+")"
        this.updvals = updstr
        return this
    }

    Query setLimit(int limit) {
        this.limit = limit
        return this
    }

    Query setOffset(int offset) {
        this.offset = offset
        return this
    }

    Query setOrder(final Map<String,SortOrder> orderPair) {
        this.sort = orderPair
        return this
    }

    Query setOrder(final String column,final  SortOrder order) {
        this.sort[column] = order //This will also allow chained commands like: .setOrder(x,y).setOrder(v,w)
        return this
    }

    Query setGroupBy(final String column) {
        this.groupby = column
        return this
    }

    /////////////////////////////////////// GET //////////////////////////////////
    Action getAction() {
        return action
    }
    List<String> getKeys() {
        if(keys.isEmpty()) {
            Log.e( "Keys were not set")
            keys.add("id") //Generic ID name
        }
        return keys
    }
    // Returns the first specified key
    String getKey() {
        return getKeys().get(0)
    }
    String getFields() {
        String fieldstr = ""
        if(this.fields.isEmpty()) {
            return "*"
        }
        this.fields.each {
            fieldstr += (fieldstr.isEmpty() ? "" : ',') + sqlName(it)
        }
        return fieldtype.getSQL(fieldstr)
    }
    String getTable() {
        return sqlName(this.table)
    }
    String getWhere() {
        return this.where.isEmpty() ? "" : " WHERE "+this.where
    }
    Object[] getArgs() {
        return this.args.toArray()
    }
	/**
	 * Return args as String array
	 * as Android connector uses only Strings
	 * @return 
	 */
    String[] getArgsStr() {
        String[] array = new String[this.args.size()]
		return this.args.toArray(array)
	}
    private String getInsertValues() {
        return this.insvals
    }
    private String getUpdateValues() {
        return this.updvals
    }
    String getGroupBy() {
        return this.groupby.isEmpty() ? "" : " GROUP BY "+sqlName(this.groupby)
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
        String off = this.offset > 0 ? " OFFSET "+this.offset : ""
        return this.limit > 0 ? " LIMIT "+this.limit+off : ""
    }

	@Override
    String toString() {
        String squery = ""
        switch(this.action) {
            case RAW:    squery = this.query
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
						squery = "SELECT COLUMN_NAME as 'name', DATA_TYPE as 'type', IF(COLUMN_KEY = 'PRI',1,0) as 'pk' FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"+cleanSQL(table)+"'"; break
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
						squery = "SHOW TABLES LIKE \""+cleanSQL(table)+"\""; break
					default:
						Log.e("Type not defined on EXISTS")
				}	
				break
            default :
                //WARN: unknown action
                break
        }
        //this.clear(); //Prevent garbage values
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
