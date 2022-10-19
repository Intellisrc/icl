package com.intellisrc.db

import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

import java.sql.Connection

/**
 * @since 2022/08/09.
 */
@CompileStatic
interface Connector {
    String getName()
    boolean open()
    void clear(Connection connection) // clear anything that was left on the connection
    boolean close()
    boolean isOpen()
    ResultStatement execute(Query query, boolean silent)
    boolean commit(Collection<Query> query)
    void rollback()
    void onError(Throwable ex)

    JDBC getJdbc()
    long getLastUsed()
    void setLastUsed(long milliseconds)
    List<String> getTables()
    List<ColumnInfo> getColumns(String table)
}
