package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * Oracle Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.oracle.params = [:]
 *
 * NOTE: In Oracle a user is practically the same as a schema (a collection of tables,
 * or as it is known as database in other engines). In other words, tables created
 * by that user are allocated under its tablespace. When you drop a user you can
 * also remove all tables associated with it.
 */
@CompileStatic
class Oracle extends JDBCServer {
    String dbname = ""
    String user = "SYSTEM"
    String password = ""
    String hostname = "localhost"
    int port = 1521 // ssl port: 2484
    String driver = "oracle.jdbc.driver.OracleDriver"

    // Oracle specific parameters:
    // https://docs.oracle.com/cd/E13222_01/wls/docs81/jdbc_drivers/oracle.html#1066413
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.oracle.params", [
            BatchPerformanceWorkaround : false,
            LoginTimeout : 0,
            ConnectionRetryCount : 0,
            ConnectionRetryDelay : 3
        ] + params)
    }

    @Override
    String getConnectionString() {
        return "oracle:thin:@//$hostname:$port/$dbname"
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    boolean supportsReplace = false
    String catalogSearchName = "%"
    @Override
    String getSchemaSearchName() {
        return user.toUpperCase()
    }

    /**
     * Must return:
     *      position, column, type, length, default, notnull, primary
     *
     * FIXME: JDBC is unable to get this information
     *
     * FIXME: 'default':
     *      at.data_default AS "default",
     *      is causing connection to crash for some reason
     *
     * @param table
     * @return
     */
    @Override
    String getInfoQuery(String table) {
        return """SELECT
at.COLUMN_ID AS "position",
LOWER(at.COLUMN_NAME) AS "column",
LOWER(at.data_type) AS "type",
CASE at.nullable
    WHEN 'N' THEN 0
    WHEN 'Y' THEN 1
END AS "nullable",
NVL((SELECT DISTINCT 1
     FROM all_cons_columns cc
     JOIN all_constraints ac
     ON(cc.CONSTRAINT_NAME = ac.CONSTRAINT_NAME)
     WHERE ac.CONSTRAINT_TYPE = 'P'
     AND at.COLUMN_NAME = cc.COLUMN_NAME
     AND at.OWNER = cc.OWNER
), 0) AS "primary"
FROM all_tab_columns at 
WHERE LOWER(at.table_name) = LOWER('${table}')"""
    }

    /**
     * FIXME: JDBC is unable to get last ID (returning some sequence string id instead, like: 'AAATPDAAHAAAALDAAB')
     *   https://community.oracle.com/tech/developers/discussion/338591/why-does-getgeneratedkeys-return-a-rowid-with-no-numeric-value
     *
     * NOTE: this method is not concurrent safe, if an insert happens between the last
     *       insert and this method, it will be incorrect (it is used as last resort)
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table) {
        table = table.replace(fieldsQuotation, "")
        return "SELECT ${table}_seq.currval lastId FROM dual"
    }
    /**
     * This will create a schema (user)
     * FIXME: Probably will fail due to permissions
     * @return
     */
    @Override
    String getCreateDatabaseQuery() {
        return "CREATE USER $user IDENTIFIED BY $password; GRANT ALL PRIVILEGES TO $user"
    }
    /**
     * This will remove the user with all its tables (remember that user is a schema)
     * @return
     */
    @Override
    String getDropDatabaseQuery() {
        return "DROP USER $user CASCADE"
    }
}
