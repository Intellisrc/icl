package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
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
	protected static int TIMEOUT = Millis.SECOND
	protected Connection connection
	protected JDBC jdbc = new Dummy()
	long lastUsed = 0

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
			ResultSet rs = connection.metaData.getTables(jdbc.catalogSearchName, jdbc.schemaSearchName, "%", "TABLE", "VIEW")
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
	 * https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getColumns
	 */
	List<ColumnInfo> getColumns(String table) {
		List<ColumnInfo> columns = []
		try {
			DatabaseMetaData meta = connection.getMetaData()
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
					autoIncrement	: rsCols.getString("IS_AUTOINCREMENT") == "YES" || (rsCols.getString("COLUMN_DEF") ?: "").contains("NEXTVAL"), // For Oracle
					generated		: rsCols.getString("IS_GENERATEDCOLUMN") == "YES",
					unique			: pks.contains(colName), //Through JDBC there is no easy way to identify if column is unique (unique is only used for information at the moment)
					primaryKey		: pks.contains(colName)
				)
				columns << col
			}
			rsCols.close()
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
	boolean open() {
		boolean connected = false
		String conn = "unset"
		try {
			conn = jdbc.connectionString
			if(!conn.toLowerCase().startsWith("jdbc")) {
				conn = "jdbc:$conn"
			}
			// Be sure that the driver is loaded
			Class.forName(jdbc.driver)

			connection = DriverManager.getConnection(conn, jdbc.user, jdbc.password)
			Log.v( "Connected to DB: %s (%s)", jdbc.dbname ?: jdbc.toString())
			connected = true
		} catch (SQLException e) {
			Log.w( "Connection failed: %s", conn)
			onError(e)
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
	protected static void setValues(PreparedStatement st, List<Object> values) {
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
	}
	/**
	 * Prepare statement using Query
	 * @param query
	 * @return
	 */
	@Override
	ResultStatement execute(Query query, boolean silent) {
		try {
			assert query.toString() : "Query can not be empty"
			final PreparedStatement st = query.isIdentityUpdate ?
				 connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS) :
				 connection.prepareStatement(query.toString())
			st.setQueryTimeout(TIMEOUT)
			setValues(st, query.args)
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
			return new DBStatement(jdbc, this, st, rs, rm, countUpdated)
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
		}
		connection.autoCommit = true
		clear(connection)
		return commited
	}

	@Override
	void rollback() {
		connection?.rollback()
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