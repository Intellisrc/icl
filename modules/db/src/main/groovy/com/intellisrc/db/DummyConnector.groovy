package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.jdbc.Dummy
import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@CompileStatic
/**
 * This class is to simulate and log DB connections
 * It is specially useful for Unit testing
 * it is required to use "dummy" as Connection string
 * @since 17/03/02.
 */
class DummyConnector implements DB.Connector {
    static String datePattern = "yyyy-MM-dd HH:mm:ss"
    private connected = false
    private opened = false
    long lastUsed = 0

    @Override
    boolean isOpen() {
        Log.v( "Is database open? "+(opened ? "YES" : "NO"))
        return opened
    }

    @Override
    String getName() {
        return "dummy"
    }

    @Override
    boolean open() {
        opened = true
        Log.v( "Database opened")
        return opened
    }

    @Override
    boolean close() {
        Log.v( "Database closed")
        opened = false
        connected = false
        return true
    }

    @Override
    DB.Statement prepare(Query query, boolean silent) {
        Log.v( "Query prepared: "+query.toString())
        return new DummyStatement()
    }

    @Override
    void onError(Throwable ex) {
        Log.e( "Error reported: ", ex)
    }

    @Override
    JDBC getJdbc() {
        return new Dummy()
    }

    @Override
    List<String> getTables() {
        return ["dummy"]
    }

    @Override
    List<ColumnInfo> getColumns(String table) {
        return [
            new ColumnInfo(name : "id", type : ColumnType.INTEGER, autoIncrement: true, primaryKey: true),
            new ColumnInfo(name : "name", type : ColumnType.TEXT, charLength: 20)
        ]
    }

    static class DummyStatement implements DB.Statement {
        List<Map<String,Object>> data = []
        private int dataIndex = 0

        @Override
        boolean next() {
            return dataIndex++ < data.size()
        }

        @Override
        void close() {
            data.clear()
        }

        @Override
        int columnCount() {
            return data.first().keySet().size()
        }

        @Override
        int firstColumn() {
            return 0
        }

        @Override
        ColumnType columnType(int index) {
            ColumnType type = ColumnType.NULL
            if(index < columnCount() &! data.isEmpty()) {
                Object value = data.first().get(columnName(index))
                switch(value) {
                    case String : type = ColumnType.TEXT; break
                    case Integer: type = ColumnType.INTEGER; break
                    case Double : type = ColumnType.DOUBLE; break
                    case LocalDateTime : type = ColumnType.DATE; break
                }
            }
            return type
        }

        @Override
        String columnName(int index) {
            Set<String> columns = data.first().keySet()
            return columns[index]
        }

        @Override
        String columnStr(int index) {
            return data[dataIndex].get(columnName(index)).toString()
        }

        @Override
        boolean columnBool(int index) {
            return Boolean.parseBoolean(columnStr(index))
        }

        @Override
        Integer columnInt(int index) {
            return Integer.parseInt(columnStr(index))
        }

        @Override
        Float columnFloat(int index) {
            return Float.parseFloat(columnStr(index))
        }

        @Override
        Double columnDbl(int index) {
            return Double.parseDouble(columnStr(index))
        }

        @Override
        LocalDateTime columnDate(int index) {
            return LocalDateTime.parse(columnStr(index), DateTimeFormatter.ofPattern(datePattern))
        }

        @Override
        byte[] columnBlob(int index) {
            return new byte[0]
        }

        @Override
        boolean isColumnNull(int index) {
            return data[dataIndex].get(columnName(index)) == null
        }

        @Override
        int updatedCount() {
            return 0
        }
    }
}
