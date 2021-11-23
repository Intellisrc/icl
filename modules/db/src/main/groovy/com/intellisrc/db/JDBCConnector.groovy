package com.intellisrc.db

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB.ColumnType
import com.intellisrc.db.DB.Connector
import com.intellisrc.db.DB.DBType
import groovy.transform.CompileStatic

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime

import static com.intellisrc.db.DB.DBType.*
import static com.intellisrc.db.Query.Action.*
import static java.sql.Types.*

@CompileStatic
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
 * @author Alberto Lepe <lepe@intellisrc.com>
 */
class JDBCConnector implements Connector {
	protected static int TIMEOUT = 1000
	protected String name = ""
	protected String user = ""
	protected String pass = ""
	protected String host = ""
	protected int port
	protected Connection db
	protected DBType type = DUMMY
	long lastUsed = 0

	/**
	 * Constructor with local settings
	 * @param conn_url
	 * @param dbname
	 */
	JDBCConnector(String conn_url = "") {
        if(conn_url.isEmpty()) {
            this.name = Config.get("db.name")  //    database name
            if(!Config.exists("db.jdbc.url")) {
				try {
					type = Config.get("db.type", "mysql").toUpperCase() as DBType
				} catch(Exception ignore) {
					Log.w("Specified database type is not supported: %s", Config.get("db.type"))
				}
				host = Config.get("db.host", "localhost")
				user = Config.get("db.user", "root")
				pass = Config.get("db.pass")
				port = Config.get("db.port", type.port)
            } else {
                parseJDBC(Config.get("db.jdbc.url"))
            }
        } else if(!conn_url.isEmpty()) {
            parseJDBC(conn_url)
        }
	}
	/**
	 * Returns database name
	 * @return
	 */
	@Override
	String getName() {
		return name
	}
	/**
	 * Returns database type
	 * @return
	 */
	@Override
	DBType getType() {
		return type
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
                port = url.port ?: type.port
                def userpass = url.userInfo?.split(":")
                if(userpass) {
                    user = userpass[0]
                    pass = userpass[1]
                }
                def path = url.path.replaceAll('/','')
                if(path) {
                    name = path
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
				if(!name.endsWith(".db")) {
					name += ".db"
				}
                url += "${stype}:${this.name}"
                break
            //case POSTGRESQL:
            //case MARIADB:
			//case MYSQL:
			default:
                url += "${stype}://${host}:${port}/${this.name}"
                break
        }
        //Additional params
        switch(type) {
			case SQLITE:
                    try {
						Class.forName("org.sqlite.JDBC")
					} catch (ClassNotFoundException e) {
						Log.e("SQLite Driver not found", e)
					}
				break
			case POSTGRESQL:
				try {
					Class.forName("org.postgresql.Driver")
				} catch (ClassNotFoundException e) {
					Log.e("PostgreSQL Driver not found", e)
				}
				break
			case MARIADB:
			case MYSQL:
				try {
					Class.forName(type == MYSQL ? "com.mysql.cj.jdbc.Driver" : "org.mariadb.jdbc.Driver")
					url += "?useSSL=false" //Disable SSL warning
					url += "&autoReconnect=true" //Reconnect
                    //UTF-8 enable:
                    url += "&useUnicode=true"
                    url += "&characterEncoding=UTF-8"
                    url += "&characterSetResults=utf8"
                    url += "&connectionCollation=utf8_general_ci"
                } catch(Exception e) {
                    Log.e( "%s Driver not found: ", type.toString(), e)
                    url = ""
                }
                break
			default:
				Log.w("Database type: [%s] doesn't include the driver. You will need to add it separately.", type)
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
		} catch (Exception e) {
			Log.w( "DB was closed ",e)
		}
		return open
	}

	@Override
	boolean close() {
		boolean closed = true
		try {
			if (!db.isClosed()) {
				db.close()
			}
		} catch (Exception e) {
			Log.w("Unable to close ", e)
			closed = false
		}
		return closed
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
				} else if (o instanceof Float) {
					st.setFloat(index, (Float) o)
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
				} else if (o instanceof LocalDate) {
					st.setDate(index, java.sql.Date.valueOf(o.toString()))
				} else if (o instanceof LocalDateTime) {
					long millis = o.toMillis()
					st.setTimestamp(index, new Timestamp(millis))
				} else {
					st.setNull(index, NULL)
					Log.e( "Wrong data type: " + o)
				}
			}
			boolean updaction = false
			//noinspection GroovyFallthrough
			switch(query.action) {
				case RAW:
					if(["SELECT", "SHOW", "GET"].any {
						query.toString().toUpperCase().startsWith(it + " ")
					}) {
						break
					}
				case INSERT:
				case REPLACE:
				case DELETE:
				case DROP:
				case UPDATE:
					try {
						st.executeUpdate()
						updaction = true
					} catch(Exception e) {
						if(query.action == RAW) {
							Log.w("An update action was inferred but it seems it is not correct." +
								  " To remove this error, set action to 'SELECT'")
							Log.w("Query is: %s", query.toString())
						}
						Log.e("Unable to set update statement. ", e)
					}
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
							case FLOAT:
								ct = ColumnType.FLOAT; break
							case BIGINT:
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
				Float columnFloat(int index) {
					try {
						return rs.getFloat(index)
					} catch (SQLException ex) {
						Log.e( "column Float failed: ",ex)
						return 0f
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
				LocalDateTime columnDate(int index) {
					try {
						return rs.getTimestamp(index).toLocalDateTime()
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
}