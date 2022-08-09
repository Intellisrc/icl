package com.intellisrc.db

import com.intellisrc.db.jdbc.JDBC
import groovy.transform.CompileStatic

/**
 * @since 2022/08/09.
 */
@CompileStatic
interface Connector {
    String getName()
    boolean open()
    boolean close()
    boolean isOpen()
    ResultStatement execute(Query query, boolean silent)
    boolean commit(List<Query> query)
    void onError(Throwable ex)

    JDBC getJdbc()
    long getLastUsed()
    void setLastUsed(long milliseconds)
    List<String> getTables()
    List<ColumnInfo> getColumns(String table)
}
