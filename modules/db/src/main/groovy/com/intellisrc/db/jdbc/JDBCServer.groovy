package com.intellisrc.db.jdbc

import groovy.transform.CompileStatic

/**
 * This class is for JDBC database servers
 * which usually are running in a specific port (and host)
 *
 * @since 2022/01/18.
 */
@CompileStatic
abstract class JDBCServer extends JDBC {
    abstract String getHostname()
    abstract void setHostname(String host)
    abstract int getPort()
    abstract void setPort(int port)

    // Aliases
    final void setHost(String name) { hostname = name }
    final String getHost() { return hostname }
}
