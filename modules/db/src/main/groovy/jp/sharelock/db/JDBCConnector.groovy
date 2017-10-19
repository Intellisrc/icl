package jp.sharelock.db

import jp.sharelock.etc.Log
import jp.sharelock.db.DB.Connector
import jp.sharelock.db.DB.ColumnType
import jp.sharelock.db.DB.DBType
import static jp.sharelock.db.DB.DBType.*
import static jp.sharelock.db.Query.Action.*

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.PreparedStatement
import static java.sql.Types.*
import java.util.regex.Matcher

@groovy.transform.CompileStatic
/**
 * SQL connector for Java using JDBC
 * It supports mysql, postgres, sqlite and a dummy connection
 * Each type requires an additional library to work (except dummy)
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class JDBCConnector implements Connector {
	
    private final String LOG_TAG = JDBCConnector.getSimpleName()
	private static int TIMEOUT = 1000
	private String dbname = ""
	private String jdbc_url = "" //For sqlite, it will be: 'sqlite' only, for mysql is : mysql://user:pass@host:port/
	private String user = ""
	private String pass = ""
	private static Connection db
	private DBType type = NONE
	long lastUsed = 0

	JDBCConnector(String conn_url, String dbname) {
		this.dbname = dbname
		this.jdbc_url = conn_url
	}

	@Override
	void open() {
		try {
            String connStr = getJDBCStr() //This step will extract user and pass
			db = DriverManager.getConnection(connStr, user, pass)
			Log.d(LOG_TAG, "Connecting to DB")
		} catch (SQLException ex) {
			Log.w(LOG_TAG, "Connection failed: " + ex)
		}
	}

	private String getJDBCStr() {
		String connection_str = "jdbc:"
		if(jdbc_url.contains("://")) {
			//--- Full URL ---
			//CHECK: parsing URL must exists in some common library
							//    protocol      ://    user         :   pass                @host           :port
			String pattern = '^(?:([^\\:]*)\\:\\/\\/)?(?:([^\\:\\@]*)(?:\\:([^\\@]*))?\\@)?(?:([^\\/\\:]*))?(?:\\:([0-9]*))?\\/$'
			Matcher m = jdbc_url =~ /$pattern/
			if(m.find()) {
				String proto = m.group(1)
					   user  = m.group(2)
					   pass  = m.group(3)
				String host  = m.group(4)
				String port  = m.group(5)
				if(user == null) { user = "" }
				if(pass == null) { pass = "" }
				if(port == null) { 
					port = ""
				} else {
					port = ":"+port
				}
				switch(proto) {
					case "mysql": type = MYSQL; break
					case "posgresql": type = POSGRESQL; break
				}
				connection_str += proto + "://" + host + port + "/" + dbname
				//Additional params
				switch(proto) {
					case "mysql":
						try {
							Class.forName("com.mysql.jdbc.Driver")
							connection_str += "?useSSL=false" //Disable SSL warning
							connection_str += "&autoReconnect=true" //Reconnect
							//UTF-8 enable:
							connection_str += "&useUnicode=true"
							connection_str += "&characterEncoding=UTF-8"
							connection_str += "&characterSetResults=utf8"
							connection_str += "&connectionCollation=utf8_general_ci"
						} catch(Exception e) {
							Log.e(LOG_TAG, "Driver not found: "+e)
							connection_str = ""
						}
						break
				}
			} else {
				Log.e(LOG_TAG, "Malformed URL, please specify it as: proto://user:pass@host:port/")
			}
		} else {
			//--- No protocol
			connection_str += jdbc_url + ":"
			switch(jdbc_url) {
				case "sqlite":
					type = SQLITE
					connection_str += dbname + ".db"
					break
				default:
					Log.e(LOG_TAG, "Database type ["+jdbc_url+"] not supported yet")
					break
			}
		}
		return connection_str
	}
			
	/**
	 * NOTE:
	 * isClosed is not reporting correctly (JDBC bug)
     * that is why we check also by 'type'
	 * @return 
	 */
	@Override
	boolean isOpen() {
		boolean open = false
		try {
            if(db != null && type != NONE) {
                open = !db.isClosed()
            }
		} catch (SQLException ex) {
			Log.w(LOG_TAG, "DB was closed ($ex)")
		}
		return open
	}

	@Override
	void close() {
		try {
			db.close()
			Log.d(LOG_TAG, "Disconnecting from DB")
		} catch (SQLException ex) {
			Log.e(LOG_TAG, "Unable to close ($ex)")
		}
	}

	@Override
	DB.Statement prepare(Query query) {
		try {
			final PreparedStatement st = db.prepareStatement(query.toString())
			st.setQueryTimeout(TIMEOUT)
			Object[] values = query.getArgs()
			for (int index = 1; index <= values.length; index++) {
				Object o = values[index - 1]
				if (o == null) {
					st.setNull(index, NULL)
				} else if (o instanceof Double) {
					st.setDouble(index, (Double) o)
				} else if (o instanceof Integer) {
					st.setInt(index, (Integer) o)
				} else if (o instanceof Long) {
					st.setLong(index, (Long) o)
				} else if (o instanceof byte[]) {
					st.setBytes(index, (byte[]) o)
				} else if (o instanceof String) {
					st.setString(index, (String) o)
				} else {
					st.setNull(index, NULL)
					Log.e(LOG_TAG, "Wrong data type")
				}
			}
			boolean updaction = false
			switch(query.getAction()) {
				case INSERT:
				case DELETE:
				case DROP:
				case RAW:
				case UPDATE:
					st.executeUpdate()
					updaction = true
				break
			}
			final ResultSet rs = updaction ? null : st.executeQuery()
			final ResultSetMetaData rm = updaction ? null : rs.getMetaData()
			return new DB.Statement() {
				@Override
				boolean next() {
					boolean ok = false
					try {
						if(rs != null) {
							ok = rs.next()
						}
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "Step failed: "+ex)
					}
					return ok
				}

				@Override
				void close() {
					try {
						if(rs != null) {
							rs.close()
						}
						st.close()
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "Unable to close Statement: "+ex)
					}
				}

				@Override
				int columnCount() {
					int count = 0
					try {
						count = rm.getColumnCount()
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column count failed: "+ex)
					}
					return count
				}

				@Override
				ColumnType columnType(int index) {
					try {
						ColumnType ct = ColumnType.NULL
						switch(rm.getColumnType(index)) {
							case VARCHAR:
							case CHAR:
								ct = ColumnType.TEXT; break
							case INTEGER:
							case SMALLINT:
							case TINYINT:
								ct = ColumnType.INTEGER; break
							case BIGINT:
							case FLOAT:
							case DOUBLE:
							case DECIMAL:
							case NUMERIC:
								ct = ColumnType.DOUBLE; break
							case BLOB:
								ct = ColumnType.BLOB; break
							case TIMESTAMP:
							case DATE:
								ct = ColumnType.DATE; break
							default:
								Log.e(LOG_TAG, "Unknown column type: "+rm.getColumnType(index))
						}
						return ct
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column type failed: "+ex)
						return null
					}
				}

				@Override
				String columnName(int index) {
					try {
						return rm.getColumnName(index)
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column name failed: "+ex)
						return ""
					}
				}

				@Override
				String columnStr(int index) {
					try {
						return rs.getString(index)
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column Str failed: "+ex)
						return ""
					}
				}

				@Override
				Integer columnInt(int index) {
					try {
						return rs.getInt(index)
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column Int failed: "+ex)
						return 0
					}
				}

				@Override
				Double columnDbl(int index) {
					try {
						return rs.getDouble(index)
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column Dbl failed: "+ex)
						return 0d
					}
				}

				@Override
				byte[] columnBlob(int index) {
					try {
						return rs.getBytes(index)
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column Blob failed: "+ex)
						return null
					}
				}

				@Override
				Date columnDate(int index) {
					try {
						return rs.getTimestamp(index)
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column Date failed: "+ex)
						return null
					}
				}

				@Override
				boolean isColumnNull(int index) {
					try {
						rs.getString(index)
						return rs.wasNull()
					} catch (SQLException ex) {
						Log.e(LOG_TAG, "column isColumnNull failed: "+ex)
						return true
					}
				}

				@Override
				int firstColumn() {
					return 1
				}
			}
		} catch (SQLException ex) {
			Log.e(LOG_TAG, "Statement failed: "+ex)
		}
		return null
	}

	@Override
	void onError(Exception ex) {
		Log.e(LOG_TAG, "General error: "+ex)
	}

	@Override
	DBType getType() {
		return type
	}
}