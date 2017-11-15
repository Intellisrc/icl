package jp.sharelock.db

import jp.sharelock.etc.Log

@groovy.transform.CompileStatic
/**
 * This class is to simulate and log DB connections
 * It is specially useful for Unit testing
 * it is required to use "dummy" as Connection string
 * @since 17/03/02.
 */
class DummyConnector implements DB.Connector {
    private connected = false
    private opened = false
    long lastUsed = 0

    boolean isOpen() {
        Log.d( "Is database open? "+(opened ? "YES" : "NO"))
        return opened
    }

    void open() {
        opened = true
        Log.d( "Database opened")
    }

    void close() {
        Log.d( "Database closed")
        opened = false
        connected = false
    }

    DB.Statement prepare(Query query) {
        Log.d( "Query prepared: "+query.toString())
        return new DummyStatement()
    }

    void onError(Exception ex) {
        Log.e( "Error reported: "+ex)
    }

    DB.DBType getType() {
        return DB.DBType.DUMMY
    }

    class DummyStatement implements DB.Statement {
        List<Map<String,Object>> data = []
        private int dataIndex = 0

        boolean next() {
            return dataIndex++ < data.size()
        }

        void close() {
            data.clear()
        }

        int columnCount() {
            return data.first().keySet().size()
        }

        int firstColumn() {
            return 0
        }

        DB.ColumnType columnType(int index) {
            DB.ColumnType type = DB.ColumnType.NULL
            if(index < columnCount() &! data.isEmpty()) {
                Object value = data.first().get(columnName(index))
                switch(value) {
                    case String : type = DB.ColumnType.TEXT; break
                    case Integer: type = DB.ColumnType.INTEGER; break
                    case Double : type = DB.ColumnType.DOUBLE; break
                    case Date : type = DB.ColumnType.DATE; break
                }
            }
            return type
        }

        String columnName(int index) {
            Set<String> columns = data.first().keySet()
            return columns[index]
        }

        String columnStr(int index) {
            return data[dataIndex].get(columnName(index)).toString()
        }

        Integer columnInt(int index) {
            return Integer.parseInt(columnStr(index))
        }

        Double columnDbl(int index) {
            return Double.parseDouble(columnStr(index))
        }

        Date columnDate(int index) {
            return columnStr(index).toDateSTD()
        }

        byte[] columnBlob(int index) {
            return new byte[0]
        }

        boolean isColumnNull(int index) {
            return data[dataIndex].get(columnName(index)) == null
        }
    }
}
