package com.intellisrc.db.jdbc

import com.intellisrc.core.ext.ToMap
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

/**
 * @since 2024/04/26.
 */
@CompileStatic
@TupleConstructor
class JDBCConfig implements ToMap {
    String type = ""
    String hostname = "localhost"
    String dbname = ""
    int port
    String user = ""
    String password = ""
    boolean memory = false
}
