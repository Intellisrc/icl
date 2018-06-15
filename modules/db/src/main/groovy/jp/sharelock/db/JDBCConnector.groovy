package jp.sharelock.db

import jp.sharelock.etc.Config
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

@groovy.transform.CompileStatic
/**
 * SQL connector for Java using JDBC
 * It supports mysql, postgres, sqlite and a dummy connection
 * Each type requires an additional library to work (except dummy)
 *
 * In configuration file, you can set:
 * db.type = [mysql,postgresql,sqlite]
 * db.name = mydb
 * db.host = localhost
 * db.user = myuser
 * db.pass = secret
 * db.port = 1234
 *
 * Or set it as URL:
 * db.jdbc.url = mysql://user:pass@host:port/dbname
 *
 * Or set database name and URL separately:
 * db.name = mydb
 * db.jdbc.url = mysql://user:pass@host:port
 *
 * @author Alberto Lepe <lepe@sharelock.jp>
 */
class JDBCConnector implements Connector {
	private static int TIMEOUT = 1000
	private String dbname = ""
	private String user = ""
	private String pass = ""
	private String host = ""
	private int port
	private static Connection db
	private DBType type = JAVADB
	long lastUsed = 0

	/**
	 * Constructor with local settings
	 * @param conn_url
	 * @param dbname
	 */
	JDBCConnector(String conn_url = "") {
        if(conn_url.isEmpty()) {
            if(Config.hasKey("db.name")) {
                this.dbname = Config.get("db.name")  //    database name
            }
            if(!Config.hasKey("db.jdbc.url")) {
                if(Config.hasKey("db.type")) {
                    type = (Config.get("db.type") ?: "mysql").toUpperCase() as DBType
                    host = Config.get("db.host") ?: "localhost"
                    user = Config.get("db.user") ?: "root"
                    pass = Config.get("db.pass") ?: ""
                    port = Config.getInt("db.port") ?: (type == MYSQL ? 3306 : 5432)
                }
            } else {
                parseJDBC(Config.get("db.jdbc.url"))
            }
        } else if(!conn_url.isEmpty()) {
            parseJDBC(conn_url)
        }
		assert dbname
	}

    /**
     * Import connection settings from URL
       proto://user:pass@host:port/db?"
     * @param url
     */
    private void parseJDBC(String sUrl) {
        if(sUrl.isEmpty()) {
            Log.e("JDBC URL is not defined. Either specify it in the constructor or use a configuration file. Alternatively, set all connections settings individually.")
        }
        if(sUrl.contains("://")) {
            try {
                URI url = new URI(sUrl)
                type = url.scheme.toUpperCase() as DBType
                host = url.host
                port = url.port ?: (type == MYSQL ? 3306 : 5432)
                def userpass = url.userInfo?.split(":")
                if(userpass) {
                    user = userpass[0]
                    pass = userpass[1]
                }
                def path = url.path.replaceAll('/','')
                if(path) {
                    dbname = path
                }
            } catch (Exception e) {
                Log.e( "Malformed URL, please specify it as: proto://user:pass@host:port/. Specified: ($sUrl). Error was : ", e)
            }
        } else {
            Log.e( "Specified URL has no protocol: [$sUrl]")
        }
    }

	@Override
	void open() {
		try {
			db = DriverManager.getConnection(getJDBCStr(), user, pass)
			Log.v( "Connecting to DB")
		} catch (SQLException e) {
			Log.w( "Connection failed: ", e)
		}
	}

    /**
     * Return JDBC URL in standard way
     * @return
     */
	String getJDBCStr() {
        def url = "jdbc:"
        def stype = type.toString().toLowerCase()
        switch (type) {
            case SQLITE:
				if(!dbname.endsWith(".db")) {
					dbname += ".db"
				}
                url += "${stype}:${dbname}"
                break
            case POSGRESQL:
            case MYSQL:
            default:
                url += "${stype}://${host}:${port}/${dbname}"
                break
        }
        //Additional params
        switch(type) {
            case MYSQL:
                try {
                    Class.forName("com.mysql.jdbc.Driver")
                    url += "?useSSL=false" //Disable SSL warning
                    url += "&autoReconnect=true" //Reconnect
                    //UTF-8 enable:
                    url += "&useUnicode=true"
                    url += "&characterEncoding=UTF-8"
                    url += "&characterSetResults=utf8"
                    url += "&connectionCollation=utf8_general_ci"
                } catch(Exception e) {
                    Log.e( "Driver not found: ",e)
                    url = ""
                }
                break
        }
        return url
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
            if(db != null) {
                open = !db.isClosed()
            }
		} catch (SQLException e) {
			Log.w( "DB was closed ",e)
		}
		return open
	}

	@Override
	void close() {
		try {
			db.close()
			Log.v( "Disconnecting from DB")
		} catch (SQLException e) {
			Log.e( "Unable to close ",e)
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
					Log.e( "Wrong data type")
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
						Log.e( "Step failed: ",ex)
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
						Log.e( "Unable to close Statement: ",ex)
					}
				}

				@Override
				int columnCount() {
					int count = 0
					try {
						count = rm.getColumnCount()
					} catch (SQLException ex) {
						Log.e( "column count failed: ",ex)
					}
					return count
				}

				@Override
				ColumnType columnType(int index) {
					try {
						ColumnType ct = ColumnType.NULL
						switch(rm.getColumnType(index)) {
                            case NCLOB: //N means: Unicode
                            case CLOB: //stores variable-length character data more than 4GB
                            case LONGNVARCHAR:
                            case LONGVARCHAR:
                            case NVARCHAR:
							case VARCHAR:
                            case NCHAR:
							case CHAR:
								ct = ColumnType.TEXT; break
							case INTEGER:
							case SMALLINT:
							case TINYINT:
                            case BOOLEAN:
                            case BIT:
								ct = ColumnType.INTEGER; break
							case BIGINT:
							case FLOAT:
							case DOUBLE:
							case DECIMAL:
							case NUMERIC:
                            case REAL:
								ct = ColumnType.DOUBLE; break
                            case LONGVARBINARY:
                            case VARBINARY:
                            case BINARY:
                            case BLOB:
								ct = ColumnType.BLOB; break
                            case TIME_WITH_TIMEZONE:
                            case TIMESTAMP_WITH_TIMEZONE:
							case TIMESTAMP:
                            case TIME:
							case DATE:
								ct = ColumnType.DATE; break
							default:
								Log.e( "Unknown column type: "+rm.getColumnType(index))
						}
						return ct
					} catch (SQLException ex) {
						Log.e( "column type failed: ",ex)
						return null
					}
				}

				@Override
				String columnName(int index) {
					try {
						return rm.getColumnName(index)
					} catch (SQLException ex) {
						Log.e( "column name failed: ",ex)
						return ""
					}
				}

				@Override
				String columnStr(int index) {
					try {
						return rs.getString(index)
					} catch (SQLException ex) {
						Log.e( "column Str failed: ",ex)
						return ""
					}
				}

				@Override
				Integer columnInt(int index) {
					try {
						return rs.getInt(index)
					} catch (SQLException ex) {
						Log.e( "column Int failed: ",ex)
						return 0
					}
				}

				@Override
				Double columnDbl(int index) {
					try {
						return rs.getDouble(index)
					} catch (SQLException ex) {
						Log.e( "column Dbl failed: ",ex)
						return 0d
					}
				}

				@Override
				byte[] columnBlob(int index) {
					try {
						return rs.getBytes(index)
					} catch (SQLException ex) {
						Log.e( "column Blob failed: ",ex)
						return null
					}
				}

				@Override
				Date columnDate(int index) {
					try {
						return rs.getTimestamp(index)
					} catch (SQLException ex) {
						Log.e( "column Date failed: ",ex)
						return null
					}
				}

				@Override
				boolean isColumnNull(int index) {
					try {
						rs.getString(index)
						return rs.wasNull()
					} catch (SQLException ex) {
						Log.e( "column isColumnNull failed: ",ex)
						return true
					}
				}

				@Override
				int firstColumn() {
					return 1
				}
			}
		} catch (SQLException ex) {
			Log.e( "Statement failed: ",ex)
		}
		return null
	}

	@Override
	void onError(Exception ex) {
		Log.e( "General error: ",ex)
	}

	@Override
	DBType getType() {
		return type
	}
}