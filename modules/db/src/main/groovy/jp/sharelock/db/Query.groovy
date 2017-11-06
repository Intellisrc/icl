package jp.sharelock.db

import jp.sharelock.etc.Log
import jp.sharelock.db.DB.DBType
import static jp.sharelock.db.DB.DBType.*
import static jp.sharelock.db.Query.Action.*
import static jp.sharelock.db.Query.SortOrder.*

@groovy.transform.CompileStatic
/**
 * Generates a Query based on rules
 * Created by A.Lepe on 16/11/11.
 */
class Query {
	private DBType dbType = DUMMY
    private static final String LOG_TAG = Query.getSimpleName()
    // Action type (RAW is default)
    enum Action {
        RAW, SELECT, UPDATE, INSERT, DELETE, DROP, INFO, LASTID, EXISTS
    }
    // Field type (NOSET is default)
    enum FieldType {
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
    enum SortOrder {
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
    private List<String> fields = new ArrayList()
    private List<String> keys = new ArrayList()
    private List args = new ArrayList()
    private FieldType fieldtype = FieldType.NOSET

    Query() {
        this.action = RAW
    }
    Query(Action action) {
        this.action = action
    }
    Query(String query) {
        this.query = query
        this.action = RAW
    }
    Query(String query, List args) {
        this.query = query
        this.action = RAW
        this.args = args
    }

    /////////////////////////////////////// SET //////////////////////////////////
	Query setType(DBType dbtype) {
		this.dbType = dbtype
		return this
	}
    Query setAction(Action action) {
        this.action = action
        return this
    }
    Query setTable(String table) {
        this.table = table
        return this
    }

    Query setFields(List<String> fields) {
        this.fields = fields
        if(this.fieldtype == FieldType.NOSET) {
            this.fieldtype = FieldType.COLUMN
        }
        return this
    }

    Query setFields(List<String> fields, FieldType type) {
        this.fields = fields
        this.fieldtype = type
        return this
    }

    Query setFieldsType(FieldType type ) {
        this.fieldtype = type
        return this
    }

    Query setKeys(List<String> keys) {
        this.keys = keys
        return this
    }

    Query setWhere(Integer where) {
        String key = getKey() //For the moment no multiple keys allowed
        this.where += (this.where.isEmpty() ? "" : " AND ") + sqlName(key) + " = ? "
        this.args.add(where)
        return this
    }

    Query setWhere(String where) {
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

	Query setWhere(String where, Object[] params) {
		int param_count = where.length() - where.replace("?", "").length()
		if(param_count == params.length) {
			this.where += (this.where.isEmpty() ? "" : " AND ") + cleanSQL(where)
			this.args.addAll(Arrays.asList(params))
		} else {
			Log.e(LOG_TAG, "Parameters specified doesn't match arguments count")
		}
		return this
	}

    Query setWhere(List where) {
        String key = getKey() //For the moment no multiple keys allowed
        String marks = ""
        for(Object cond: where) {
            marks += (marks.isEmpty() ? "" : ",") + '?'
            this.args.add(cond)
        }
        this.where += sqlName(key) + " IN ("+marks+")"
        return this
    }

    Query setWhere(HashMap<String,String> where) {
        Iterator it = where.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next()
            this.where += (this.where.isEmpty() ? "" : " AND ") + sqlName(pair.getKey()) + " = ? "
            this.args.add(pair.getValue())
            it.remove() // avoids a ConcurrentModificationException
        }
        return this
    }

    Query setValues(HashMap values) {
        String inspre = ""
        String inspst = ""
        String updstr = ""
        Iterator it = values.entrySet().iterator()
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next()
            inspre += (inspre.isEmpty() ? "" : ',') + sqlName(pair.getKey())
            inspst += (inspst.isEmpty() ? "" : ',') + "?"
            updstr += (updstr.isEmpty() ? "" : ',') + sqlName(pair.getKey()) + " = ?"
            this.args.add(pair.getValue())
            it.remove() // avoids a ConcurrentModificationException
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

    Query setOrder(String column, SortOrder order) {
        this.sort[column] = order //This will also allow chained commands like: .setOrder(x,y).setOrder(v,w)
        return this
    }

    Query setGroupBy(String column) {
        this.groupby = column
        return this
    }

    /////////////////////////////////////// GET //////////////////////////////////
    Action getAction() {
        return action
    }
    List<String> getKeys() {
        if(keys.isEmpty()) {
            Log.e(LOG_TAG, "Keys were not set")
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
        for(String fld: this.fields) {
            fieldstr += (fieldstr.isEmpty() ? "" : ',') + sqlName(fld)
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
        for( int i = 0; i < this.args.size(); i++ ){
            array[i] = this.args.get(i).toString()
        }
		return array
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
						Log.e(LOG_TAG,"Type not defined on INFO")
				}
				break
            case LASTID:
				switch(dbType) {
					case SQLITE:				
						squery = "SELECT last_insert_rowid() as lastid"; break
					case MYSQL:
						squery = "SELECT LAST_INSERT_ID() as lastid"; break
					default:
						Log.e(LOG_TAG,"Type not defined on LASTID")
				}
				break
            case EXISTS: 
				switch(dbType) {
					case SQLITE:				
						squery = "PRAGMA table_info("+getTable()+")"; break
					case MYSQL:
						squery = "SHOW TABLES LIKE \""+cleanSQL(table)+"\""; break
					default:
						Log.e(LOG_TAG,"Type not defined on EXISTS")
				}	
				break
            default :
                //WARN: unknown action
                break
        }
        //this.clear(); //Prevent garbage values
        return squery
    }

    private static String sqlName(Object obj) {
        return sqlName(obj.toString())
    }
    // Return column or table name clean and with ``
    private static String sqlName(String str) {
        return "`" + str.toLowerCase().replaceAll("/[^a-z0-9._]/","") + "`"
    }
	/**
	 * Clean a SQL query removing invalid characters like unicode, comments, semicolon, etc
	 */
	private static String cleanSQL(String sql) {
		return sql.replaceAll("/[^a-z0-9._()><=?%+*/-`\"']/","")
	}
}
