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
    String fieldsQuotation = '"'
    String catalogSearchName = "%"
    @Override
    String getSchemaSearchName() {
        return user.toUpperCase()
    }
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }

    /*
     * Must return:
     *      position, column, type, length, default, notnull, primary
     *
     * JDBC is unable to get this information
     *
     * NOTE: We did not include constraint_type 'C' (NOT NULL) as they are not needed here.
     * NOTE: Returning at.data_default won't work as it is LONG inside Oracle and its really
     *       complicated to get that from the DB.
     *
     * @param table
     * @return
     *
    @Override
    String getInfoQuery(String table) {
        return """
            SELECT 
                at.column_id AS "position",
                LOWER(at.column_name) as "column",
                LOWER(at.data_type) as "type",
                '' as "default",
            CASE at.nullable
                WHEN 'N' THEN 0
                WHEN 'Y' THEN 1
            END AS "nullable",
            CASE
                WHEN cc.constraint_type = 'P' THEN 1
                ELSE 0
            END AS "autoinc",
            CASE
                WHEN cc.constraint_type = 'P' THEN 1
                ELSE 0
            END AS "primary",
            CASE
                WHEN cc.constraint_type = 'U' THEN 1
                ELSE 0
            END AS "unique"
            FROM all_tab_columns at
            LEFT JOIN all_cons_columns ac 
              ON (at.owner = ac.owner
                AND at.table_name = ac.table_name 
                AND at.column_name = ac.column_name)
            LEFT JOIN all_constraints cc 
              ON (ac.constraint_name = cc.constraint_name)    
            WHERE cc.constraint_type != 'C' AND LOWER(at.table_name) = LOWER('${table}')"""
    }*/

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
