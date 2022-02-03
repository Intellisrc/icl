package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

import static com.intellisrc.db.jdbc.Derby.SubProtocol.*

/**
 * Derby Apache Database (JavaDB)
 *
 * Derby has a small footprint -- about 3.5 megabytes for the base engine and embedded JDBC driver.
 * Derby is based on the Java, JDBC, and SQL standards.
 * Derby provides an embedded JDBC driver that lets you embed Derby in any Java-based solution.
 * One advantage over SQLite is that can be run as a server and encryption can be used
 *
 * @since 17/12/14.
 *
 * Additional settings:
 * db.derby.params = [:]
 */
@CompileStatic
class Derby extends JDBCServer {
    static enum SubProtocol {
        SERVER, DIRECTORY, MEMORY, CLASSPATH, JAR
    }
    String dbname = ""
    String user = ""
    String password = ""
    String hostname = "localhost"
    int port = 1527
    String driver = "org.apache.derby.jdbc.EmbeddedDriver"
    boolean create = false
    boolean encrypt = false
    boolean memory = Config.get("db.derby.memory", false)
    SubProtocol subProtocol = DIRECTORY

    // Derby specific parameters:
    // https://db.apache.org/derby/docs/10.0/manuals/reference/sqlj238.html#HDRSII-ATTRIB-24612
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.derby.params", [
            create : create
            // encryptionProvider :
            // encryptionAlgorithm :
            // logDevice :
            // rollForwardRecoveryFrom :
            // createFrom :
            // restoreFrom :
            // shutdown :
        ] + params)
    }

    @Override
    String getConnectionString() {
        String sub = ""
        // Do not set unless is enabled:
        if(encrypt) {
            parameters.dataEncryption = true
        }
        if(memory) {
            subProtocol = MEMORY
        }
        //noinspection GroovyFallthrough
        switch (subProtocol) {
            case SERVER: // Not used
            case DIRECTORY:
                // empty
                break
            case MEMORY:
                sub = "memory:"
                break
            case CLASSPATH:
            case JAR:
                // Must start with /
                // https://db.apache.org/derby/docs/10.8/devguide/cdevdvlp17453.html
                if(! dbname.startsWith("/")) {
                    dbname = "/${dbname}"
                }
                break
        }
        return "derby:" + (subProtocol == SERVER ? "//$hostname:$port/$dbname" : "${sub}$dbname") + ";" +
                parameters.collect {
                    "${it.key}=${it.value}"
                }.join(";")
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String catalogSearchName = "%"
    String schemaSearchName = "%"
    boolean supportsReplace = false

    @Override
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }
}