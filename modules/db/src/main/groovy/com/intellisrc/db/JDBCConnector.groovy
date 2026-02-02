package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.sql.*
import java.time.LocalDate
import java.time.LocalDateTime

import static java.sql.Types.NULL

@CompileStatic
/**
 * SQL connector for Java using JDBC
 * It supports multiple implementations and a dummy connection
 * Each type requires an additional library to work (except dummy)
 *
 * @author Alberto Lepe
 */
class JDBCConnector implements Connector {
	protected Connection connection
	protected JDBC jdbc = new Dummy()
	LocalDateTime lastUsed
	LocalDateTime creationTime

	/**
	 * Constructor with local settings
	 * @param conn_url
	 * @param dbname
	 */
	JDBCConnector(final JDBC jdbc = null) {
        if(!jdbc) {
			this.jdbc = JDBC.fromSettings()
        } else {
            this.jdbc = jdbc
        }
		creationTime = SysClock.now
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
	Set<String> getTables() {
		Set<String> list = []
		try {
			ResultSet rs = connection.metaData.getTables(jdbc.catalogSearchName, jdbc.schemaSearchName, "%", ["TABLE", "VIEW"] as String[])
			while (rs.next()) {
				list << (jdbc.convertToLowerCase ? rs.getString("TABLE_NAME")?.toLowerCase() : rs.getString("TABLE_NAME"))
				Log.v("Cat: %s, Sch: %s, Name: %s, Type: %s",
					rs.getString("TABLE_CAT"),
					rs.getString("TABLE_SCHEM"),
					rs.getString("TABLE_NAME"),
					rs.getString("TABLE_TYPE")
				)
			}
			rs.close()
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
		} catch (Exception e) {
			Log.w("Unable to get tables via JDBC")
			onError(e)
		}
		return jdbc.filterTables(list)
	}
	/**
	 * Return tables and views, in which views have "true" as value.
	 * getTables() only returns names, but you may not know if it is a view or not.
	 * @return
	 */
	Map<String, Boolean> getRelationsWithTypes() {
		Map<String, Boolean> types = [:]
		try {
			ResultSet rs = connection.metaData.getTables(jdbc.catalogSearchName, jdbc.schemaSearchName, "%", "TABLE", "VIEW")
			while (rs.next()) {
				String name = (jdbc.convertToLowerCase ? rs.getString("TABLE_NAME")?.toLowerCase() : rs.getString("TABLE_NAME"))
				types[name] = rs.getString("TABLE_TYPE")?.equalsIgnoreCase("view") ?: false
			}
			rs.close()
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
		} catch (Exception e) {
			Log.w("Unable to get tables via JDBC")
			onError(e)
		}
		Set<String> filtered = jdbc.filterTables(types.keySet())
		return types.findAll { k, v -> k in filtered }
	}
	/**
	 * To handle exceptions coming from JDBC driver
	 * @param rs
	 * @param prop
	 * @return
	 */
	protected String getColumnPropertyString(ResultSet rs, String prop) {
		String s = ""
		try {
			s = rs.getString(prop)
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
		} catch (Exception e) {
			Log.w("Unable to get property: ", e)
		}
		return s
	}
	/**
	 * To handle exceptions coming from JDBC driver
	 * @param rs
	 * @param prop
	 * @return
	 */
	protected int getColumnPropertyInt(ResultSet rs, String prop) {
		int i = 0
		try {
			i = rs.getInt(prop)
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
		} catch (Exception e) {
			Log.w("Unable to get property: ", e)
		}
		return i
	}
	/**
	 * Get columns via JDBC
	 * @return Map [ column_name : is_primary ]
	 * https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getColumns
	 */
	List<ColumnInfo> getColumns(String table) {
		List<ColumnInfo> columns = []
		try {
			if(connection) {
				DatabaseMetaData meta = connection.getMetaData()
				List<String> pks = []
				ResultSet rsPk = meta.getPrimaryKeys(jdbc.catalogSearchName, jdbc.schemaSearchName, jdbc.getTableSearchName(table))
				while (rsPk.next()) {
					pks << (jdbc.convertToLowerCase ? rsPk.getString("COLUMN_NAME").toLowerCase() : rsPk.getString("COLUMN_NAME"))
				}
				rsPk.close()
				ResultSet rsCols = meta.getColumns(jdbc.catalogSearchName, jdbc.schemaSearchName, jdbc.getTableSearchName(table), "%")
				while (rsCols.next()) {
					String colName = jdbc.convertToLowerCase ? rsCols.getString("COLUMN_NAME").toLowerCase() : rsCols.getString("COLUMN_NAME")
					int decimals = getColumnPropertyInt(rsCols,"DECIMAL_DIGITS")

					ColumnInfo col = new ColumnInfo(
						name: colName,
						type: ColumnType.fromJavaSQL(getColumnPropertyInt(rsCols,"DATA_TYPE"), decimals),
						position: getColumnPropertyInt(rsCols, "ORDINAL_POSITION"),
						length: getColumnPropertyInt(rsCols,"COLUMN_SIZE"),
						charLength: getColumnPropertyInt(rsCols,"CHAR_OCTET_LENGTH"),
						bufferLength: getColumnPropertyInt(rsCols,"BUFFER_LENGTH"),
						decimalDigits: decimals,
						nullable: getColumnPropertyString(rsCols,"IS_NULLABLE") == "YES",
						defaultValue: getColumnPropertyString(rsCols, "COLUMN_DEF"),
						autoIncrement: getColumnPropertyString(rsCols,"IS_AUTOINCREMENT") == "YES" || (getColumnPropertyString(rsCols,"COLUMN_DEF") ?: "").contains("NEXTVAL"), // For Oracle
						generated: getColumnPropertyString(rsCols,"IS_GENERATEDCOLUMN") == "YES",
						unique: pks.contains(colName), //Through JDBC there is no easy way to identify if column is unique (unique is only used for information at the moment)
						primaryKey: pks.contains(colName)
					)
					//FIXME: autoincrement in Oracle
					columns << col
				}
				rsCols.close()
			} else {
				Log.w("Connection was null")
			}
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
		} catch(Exception e) {
			Log.w("Unable to get columns of table: [%s] via JDBC", table)
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
		return jdbc
	}

	/**
	 * Open connection
	 * @return
	 */
	@Override
	boolean open() throws DatabaseConnectionException {
		boolean connected = false
		String conn = "unset"
		try {
			conn = jdbc.connectionString
			if (!conn.toLowerCase().startsWith("jdbc")) {
				conn = "jdbc:$conn"
			}
			// Be sure that the driver is loaded
			if (jdbc.driver) {
				Class.forName(jdbc.driver)
				DriverManager.setLoginTimeout(DB.connectionTimeout)
				connection = (jdbc.user || jdbc.password) ?
					DriverManager.getConnection(conn, jdbc.user, jdbc.password) :
					DriverManager.getConnection(conn)
				Log.v("Connected to DB: %s (%s)", jdbc.dbname ?: jdbc.toString())
				connected = true
			} else {
				Log.w("Driver was not specified for database (%s)", conn)
			}
		} catch (Exception e) {
			Log.d("Connection failed: Database connection string: %s", conn)
			DatabaseConnectionException dce = new DatabaseConnectionException("Failed to connect to: ${conn}", e)
			if(DB.handleConnectionExceptions) {
				onError(dce)
			} else {
				throw dce
			}
		}
		return connected
	}

	@Override
	void clear(Connection conn) {
		jdbc.clear(conn)
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
            if(connection != null) {
                open = !connection.isClosed()
            }
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
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
			if (connection !== null &&! connection.isClosed()) {
				connection.close()
			}
		} catch (Exception e) {
			Log.w("Unable to close")
			closed = false
			onError(e)
		}
		return closed
	}
	/**
	 * Set values in prepared statement
	 * @param st
	 * @param values
	 */
	protected void setValues(PreparedStatement st, List<Object> values) {
		for (int index = 1; index <= values.size(); index++) {
			Object o = values[index - 1]
			if (o == null) {
				st.setNull(index, NULL)
			} else if (o instanceof Boolean) {
				st.setBoolean(index, (Boolean) o)
			} else if (o instanceof Float) {
				st.setFloat(index, (Float) o)
			} else if (o instanceof Double || o instanceof BigDecimal) {
				st.setDouble(index, (Double) o)
			} else if (o instanceof Short) {
				st.setShort(index, (Short) o)
			} else if (o instanceof Integer) {
				st.setInt(index, (Integer) o)
			} else if (o instanceof Long || o instanceof BigInteger) {
				st.setLong(index, (Long) o)
			} else if (o instanceof byte[]) {
				st.setBytes(index, (byte[]) o)
			} else if (o instanceof Character) {
				st.setString(index, o.toString())
			} else if (o instanceof char[]) {
				st.setString(index, o.toString())
			} else if (o instanceof String) {
				st.setString(index, (String) o)
			} else if (o instanceof LocalDate) {
				if(jdbc.supportsDate) {
					st.setDate(index, Date.valueOf(o.toString()))
				} else {
					st.setString(index, (o as LocalDate).YMD)
				}
			} else if (o instanceof LocalDateTime) {
				if(jdbc.supportsDate) {
					long millis = o.toMillis()
					st.setTimestamp(index, new Timestamp(millis))
				} else {
					st.setString(index, (o as LocalDateTime).YMDHmsS)
				}
			} else {
				st.setNull(index, NULL)
				Log.e( "Wrong data type: " + o)
			}
		}
	}
	/**
	 * Prepare statement using Query
	 * @param query
	 * @return
	 */
	@Override
	ResultStatement execute(Query query, boolean silent) {
		try {
			assert query.toString(): "Query can not be empty"
			final PreparedStatement st = query.isIdentityUpdate ?
				connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS) :
				connection.prepareStatement(query.toString())
			st.setQueryTimeout(DB.queryTimeout)
			setValues(st, query.args)
			boolean updaction = query.isSetQuery
			int countUpdated = 0
			//noinspection GroovyFallthrough
			if (updaction) {
				try {
					countUpdated = st.executeUpdate()
					if (updaction) {
						Log.v("Rows affected: %d", countUpdated)
					}
				} catch (SQLException syntaxError) {
					if (silent) {
						Log.w("SQL Exception: %s", syntaxError)
					} else {
						onError(syntaxError)
					}
					return null
				} catch (Exception e) {
					if (silent) {
						Log.w("Unable to set statement for query [%s]: %s", query.toString(), e)
					} else {
						onError(e)
					}
					return null
				}
			}
			final ResultSet rs = updaction ? (query.isIdentityUpdate ? st.getGeneratedKeys() : null) : st.executeQuery()
			final ResultSetMetaData rm = updaction ? null : rs.getMetaData()
			return new DBStatement(jdbc, this, st, rs, rm, countUpdated)
		} catch(SQLNonTransientConnectionException | ConnectException ce) {
			onError(new DatabaseConnectionException(ce))
		} catch (SQLException ex) {
			if(!silent) {
				Log.w("Statement failed: %s", ex)
			}
			onError(ex)
		} catch (AssertionError ae) {
			Log.w("Invalid query: %s", ae)
			onError(ae)
		} catch (Exception e) {
			Log.w("Unexpected error while processing request: %s", e)
			onError(e)
		}
		clear(connection)
		return null
	}

	@Override
	boolean commit(Collection<Query> queries) {
        boolean commited = false
		connection.autoCommit = false
		Set<String> uniqueQueries = queries.collect { it.toString() }.toSet()
        Map<String, PreparedStatement> statementList = [:]
		try {
			uniqueQueries.each {
				statementList[it] = connection.prepareStatement(it)
			}
			queries.each {
				PreparedStatement ps = statementList[it.toString()]
				setValues(ps, it.args) //List must contain Prepared statements
				ps.executeUpdate()
			}
			connection.commit()
            commited = true
		} catch(Exception e) {
			onError(e)
			connection?.rollback()
		}
		connection.autoCommit = true
		clear(connection)
		return commited
	}

	/**
	 * General error handling
	 * @param ex
	 */
	@Override
	void onError(Throwable ex) {
		if(ex instanceof SQLException) {
			while(ex) {
				jdbc.onError.call(ex)
				ex = (ex as SQLException).nextException
			}
		} else {
			jdbc.onError.call(ex)
		}
	}
}