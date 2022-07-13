package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.db.DB.Connector
import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime

import static com.intellisrc.db.Query.Action.*
import static java.sql.Types.*

@CompileStatic
/**
 * SQL connector for Java using JDBC
 * It supports multiple implementations and a dummy connection
 * Each type requires an additional library to work (except dummy)
 *
 * @author Alberto Lepe
 */
class JDBCConnector implements Connector {
	protected static int TIMEOUT = Millis.SECOND
	protected Connection db
	protected JDBC type = new Dummy()
	long lastUsed = 0

	/**
	 * Constructor with local settings
	 * @param conn_url
	 * @param dbname
	 */
	JDBCConnector(final JDBC jdbc = null) {
        if(!jdbc) {
			type = JDBC.fromSettings()
        } else {
            type = jdbc
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
	 * Get tables via JDBC
	 * @return
	 */
	List<String> getTables() {
		List<String> list = []
		try {

			ResultSet rs = db.getMetaData().getTables(jdbc.catalogSearchName, jdbc.schemaSearchName, "%", "TABLE", "VIEW")
			while (rs.next()) {
				list << (jdbc.convertToLowerCase ? rs.getString("TABLE_NAME")?.toLowerCase() : rs.getString("TABLE_NAME"))
				/*Log.v("Cat: %s, Sch: %s, Name: %s, Type: %s",
					rs.getString("TABLE_CAT"),
					rs.getString("TABLE_SCHEM"),
					rs.getString("TABLE_NAME"),
					rs.getString("TABLE_TYPE")
				)*/
			}
			rs.close()
		} catch (Exception e) {
			Log.w("Unable to get tables via JDBC")
			onError(e)
		}
		return jdbc.filterTables(list)
	}
	/**
	 * Get columns via JDBC
	 * @return Map [ column_name : is_primary ]
	 */
	List<ColumnInfo> getColumns(String table) {
		List<ColumnInfo> columns = []
		try {
			DatabaseMetaData meta = db.getMetaData()
			List<String> pks = []
			ResultSet rsPk = meta.getPrimaryKeys(jdbc.catalogSearchName,jdbc.schemaSearchName, jdbc.getTableSearchName(table))
			while (rsPk.next()) {
				pks << (jdbc.convertToLowerCase ? rsPk.getString("COLUMN_NAME").toLowerCase() : rsPk.getString("COLUMN_NAME"))
			}
			rsPk.close()
			ResultSet rsCols = meta.getColumns(jdbc.catalogSearchName, jdbc.schemaSearchName, jdbc.getTableSearchName(table), "%")
			while(rsCols.next()) {
				String colName = jdbc.convertToLowerCase ? rsCols.getString("COLUMN_NAME").toLowerCase() : rsCols.getString("COLUMN_NAME")
				ColumnInfo col = new ColumnInfo(
					name 			: colName,
					type 			: ColumnType.fromJavaSQL(rsCols.getInt("DATA_TYPE")),
					position		: rsCols.getInt("ORDINAL_POSITION"),
					length			: rsCols.getInt("COLUMN_SIZE"),
					charLength		: rsCols.getInt("CHAR_OCTET_LENGTH"),
					bufferLength	: rsCols.getInt("BUFFER_LENGTH"),
					decimalDigits	: rsCols.getInt("DECIMAL_DIGITS"),
					nullable		: rsCols.getString("IS_NULLABLE") == "YES",
					defaultValue	: rsCols.getString("COLUMN_DEF"),
					autoIncrement	: rsCols.getString("IS_AUTOINCREMENT") == "YES",
					generated		: rsCols.getString("IS_GENERATEDCOLUMN") == "YES",
					unique			: false, //TODO: not available through jdbc ??
					primaryKey		: pks.contains(colName)
				)
				columns << col
			}
			rsCols.close()
		} catch(Exception e) {
			Log.w("Unable to get tables via JDBC")
			onError(e)
		}
		return columns
	}

	/**
	 * Returns database type
	 * @return
	 */
	@Override
	JDBC getJdbc() {
		return type
	}

	/**
	 * Open connection
	 * @return
	 */
	@Override
	boolean open() {
		boolean connected = false
		try {
			String conn = type.connectionString
			if(!conn.toLowerCase().startsWith("jdbc")) {
				conn = "jdbc:$conn"
			}
			Log.v( "Connecting to DB: %s", conn)
			db = DriverManager.getConnection(conn, type.user, type.password)
			Log.d( "Connected to DB: %s", type.dbname ?: type.toString())
			connected = true
		} catch (SQLException e) {
			Log.w( "Connection failed")
			onError(e)
		}
		return connected
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
			Log.w( "DB was closed")
			onError(e)
		}
		return open
	}

	/**
	 * Close connection
	 * @return
	 */
	@Override
	boolean close() {
		boolean closed = true
		try {
			if (db !== null &&! db.isClosed()) {
				db.close()
			}
		} catch (Exception e) {
			Log.w("Unable to close")
			closed = false
			onError(e)
		}
		return closed
	}

	/**
	 * Prepare statement using Query
	 * @param query
	 * @return
	 */
	@Override
	DB.Statement prepare(Query query, boolean silent) {
		try {
			assert query.toString() : "Query can not be empty"
			final PreparedStatement st = query.isIdentityUpdate ?
				 db.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS) :
				 db.prepareStatement(query.toString())
			st.setQueryTimeout(TIMEOUT)
			Object[] values = query.getArgs()
			for (int index = 1; index <= values.length; index++) {
				Object o = values[index - 1]
				if (o == null) {
					st.setNull(index, NULL)
				} else if (o instanceof Float) {
					st.setFloat(index, (Float) o)
				} else if (o instanceof Double || o instanceof BigDecimal) {
					st.setDouble(index, (Double) o)
				} else if (o instanceof Integer) {
					st.setInt(index, (Integer) o)
				} else if (o instanceof Long || o instanceof BigInteger) {
					st.setLong(index, (Long) o)
				} else if (o instanceof byte[]) {
					st.setBytes(index, (byte[]) o)
				} else if (o instanceof String) {
					st.setString(index, (String) o)
				} else if (o instanceof LocalDate) {
					st.setDate(index, Date.valueOf(o.toString()))
				} else if (o instanceof LocalDateTime) {
					long millis = o.toMillis()
					st.setTimestamp(index, new Timestamp(millis))
				} else {
					st.setNull(index, NULL)
					Log.e( "Wrong data type: " + o)
				}
			}
			boolean updaction = query.isSetQuery
			int countUpdated = 0
			//noinspection GroovyFallthrough
			if(updaction) {
				try {
					countUpdated = st.executeUpdate()
					if(updaction) {
						Log.v("Rows affected: %d", countUpdated)
					}
				} catch(SQLException syntaxError) {
					if(!silent) {
						Log.w("Query was mistaken: %s", syntaxError.message)
					}
					onError(syntaxError)
					return null
				} catch(Exception e) {
					if(!silent) {
						Log.w("Unable to set statement for query [%s]: %s", query.toString(), e.message)
					}
					onError(e)
					return null
				}
			}
			final ResultSet rs = updaction ? (query.isIdentityUpdate ? st.getGeneratedKeys() : null) : st.executeQuery()
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
						Log.w( "Step failed")
						onError(ex)
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
						Log.w( "Unable to close Statement")
						onError(ex)
					}
				}

				@Override
				int columnCount() {
					int count = 0
					try {
						count = rm.getColumnCount()
					} catch (SQLException ex) {
						Log.w( "column count failed")
						onError(ex)
					}
					return count
				}

				@Override
				ColumnType columnType(int index) {
					try {
						return ColumnType.fromJavaSQL(rm.getColumnType(index))
					} catch (SQLException ex) {
						Log.w( "column type failed for index: %d", index)
						onError(ex)
						return null
					}
				}

				@Override
				String columnName(int index) {
					try {
						return rm.getColumnLabel(index)
					} catch (SQLException ex) {
						Log.w( "column name failed for index: %d", index)
						onError(ex)
						return ""
					}
				}

				@Override
				String columnStr(int index) {
					try {
						return rs.getString(index)
					} catch (SQLException ex) {
						Log.w( "column Str failed for index: %d", index)
						onError(ex)
						return ""
					}
				}

				@Override
				Integer columnInt(int index) {
					try {
						return rs.getInt(index)
					} catch (SQLException ex) {
						Log.w( "column Int failed for index: %d", index)
						onError(ex)
						return 0
					}
				}

				@Override
				Float columnFloat(int index) {
					try {
						return rs.getFloat(index)
					} catch (SQLException ex) {
						Log.w( "column Float failed for index: %d", index)
						onError(ex)
						return 0f
					}
				}

				@Override
				Double columnDbl(int index) {
					try {
						return rs.getDouble(index)
					} catch (SQLException ex) {
						Log.w( "column Dbl failed for index: %d", index)
						onError(ex)
						return 0d
					}
				}

				@Override
				byte[] columnBlob(int index) {
					try {
						return rs.getBytes(index)
					} catch (SQLException ex) {
						Log.w( "column Blob failed for index: %d", index)
						onError(ex)
						return null
					}
				}

				@Override
				LocalDateTime columnDate(int index) {
					try {
						return rs.getTimestamp(index).toLocalDateTime()
					} catch (SQLException ex) {
						Log.w( "column Date failed for index: %d", index)
						onError(ex)
						return null
					}
				}

				@Override
				boolean isColumnNull(int index) {
					try {
						rs.getString(index)
						return rs.wasNull()
					} catch (SQLException ex) {
						Log.w( "column isColumnNull failed for index: %d", index)
						onError(ex)
						return true
					}
				}

				@Override
				int firstColumn() {
					return 1
				}

				@Override
				int updatedCount() {
					return countUpdated
				}
			}
		} catch (SQLException ex) {
			Log.w("Statement failed")
			onError(ex)
		} catch (AssertionError ae) {
			Log.w("Invalid query")
			onError(ae)
		} catch (Exception e) {
			Log.w("Unexpected error while processing request")
			onError(e)
		}
		return null
	}

	/**
	 * General error handling
	 * @param ex
	 */
	@Override
	void onError(Throwable ex) {
		if(ex instanceof SQLException) {
			while(ex) {
				type.onError.call(ex)
				ex = (ex as SQLException).nextException
			}
		} else {
			type.onError.call(ex)
		}
	}
}