package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.time.LocalDateTime

/**
 * @since 2022/08/09.
 */
@CompileStatic
class DBStatement implements ResultStatement {
    final PreparedStatement preparedStatement
    final ResultSetMetaData resultSetMetaData
    final ResultSet resultSet
    final JDBC jdbc
    final JDBCConnector conn
    final int countUpdated

    DBStatement(JDBC jdbc, JDBCConnector conn, PreparedStatement ps, ResultSet rs, ResultSetMetaData rsmd, int countUpdated) {
        this.jdbc = jdbc
        this.conn = conn
        preparedStatement = ps
        resultSetMetaData = rsmd
        resultSet = rs
        this.countUpdated = countUpdated
    }
    @Override
    boolean next() {
        boolean ok = false
        try {
            if(resultSet != null) {
                ok = resultSet.next()
            }
        } catch (SQLException ex) {
            Log.w( "Step failed")
            conn.onError(ex)
        }
        return ok
    }

    @Override
    void close() {
        try {
            if(resultSet != null) {
                resultSet.close()
            }
            preparedStatement?.close()
        } catch (Exception ex) {
            Log.w("Unable to close Statement")
            if(ex instanceof SQLException) {
                conn.onError(ex)
            }
        }
    }

    @Override
    int columnCount() {
        int count = 0
        try {
            count = resultSetMetaData.getColumnCount()
        } catch (SQLException ex) {
            Log.w( "column count failed")
            conn.onError(ex)
        }
        return count
    }

    @Override
    ColumnType columnType(int index) {
        try {
            return ColumnType.fromJavaSQL(resultSetMetaData.getColumnType(index))
        } catch (SQLException ex) {
            Log.w( "column type failed for index: %d", index)
            conn.onError(ex)
            return null
        }
    }

    @Override
    String columnName(int index) {
        try {
            return resultSetMetaData.getColumnLabel(index)
        } catch (SQLException ex) {
            Log.w( "column name failed for index: %d", index)
            conn.onError(ex)
            return ""
        }
    }

    @Override //NOTE: does not support multibyte
    char columnChar(int index) {
        try {
            return resultSet.getString(index).charAt(0)
        } catch (SQLException ex) {
            Log.w( "column Char failed for index: %d", index)
            conn.onError(ex)
            return 0
        }
    }

    @Override
    char[] columnChars(int index) {
        try {
            return resultSet.getString(index).toCharArray()
        } catch (SQLException ex) {
            Log.w( "column Char Array failed for index: %d", index)
            conn.onError(ex)
            return new char[]{}
        }
    }

    @Override
    String columnStr(int index) {
        try {
            return resultSet.getString(index)
        } catch (SQLException ex) {
            Log.w( "column Str failed for index: %d", index)
            conn.onError(ex)
            return ""
        }
    }

    @Override
    boolean columnBool(int index) {
        try {
            return jdbc.supportsBoolean ? resultSet.getBoolean(index) : (resultSet.getString(index).trim().toLowerCase() == 'true')
        } catch (SQLException ex) {
            Log.w( "column Boolean failed for index: %d", index)
            conn.onError(ex)
            return false
        }
    }

    @Override
    Integer columnInt(int index) {
        try {
            return resultSet.getInt(index)
        } catch (SQLException ex) {
            Log.w( "column Int failed for index: %d", index)
            conn.onError(ex)
            return 0
        }
    }

    @Override
    Float columnFloat(int index) {
        try {
            return resultSet.getFloat(index)
        } catch (SQLException ex) {
            Log.w( "column Float failed for index: %d", index)
            conn.onError(ex)
            return 0f
        }
    }

    @Override
    Double columnDbl(int index) {
        try {
            return resultSet.getDouble(index)
        } catch (SQLException ex) {
            Log.w( "column Dbl failed for index: %d", index)
            conn.onError(ex)
            return 0d
        }
    }

    @Override
    byte[] columnBlob(int index) {
        try {
            return resultSet.getBytes(index)
        } catch (SQLException ex) {
            Log.w( "column Blob failed for index: %d", index)
            conn.onError(ex)
            return null
        }
    }

    @Override
    LocalDateTime columnDate(int index) {
        try {
            return resultSet.getTimestamp(index).toLocalDateTime()
        } catch (SQLException ex) {
            Log.w( "column Date failed for index: %d", index)
            conn.onError(ex)
            return null
        }
    }

    @Override
    boolean isColumnNull(int index) {
        try {
            resultSet.getString(index)
            return resultSet.wasNull()
        } catch (SQLException ex) {
            Log.w( "column isColumnNull failed for index: %d", index)
            conn.onError(ex)
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
