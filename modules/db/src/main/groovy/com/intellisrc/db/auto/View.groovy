package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.etc.Instanciable
import groovy.transform.CompileStatic
/**
 * A read-only Table or View
 * If the table or view exists (and createSQL is specified) it won't
 * recreate the table or view unless 'recreate' is true (you can additionally
 * specify 'DROP' statements inside the createSQL.
 *
 * NOTE: 'createSQL' is not required to be passed into the constructor,
 * it can be set later, for example:
 *
 * <code>
 * MyView myView = new MyView("CREATE VIEW...", true)
 * </code>
 * @since 2023/05/30.
 */
@CompileStatic
abstract class View<M extends Model> extends Relational<M> implements Instanciable<M> {
    abstract String getCreateSQL()
    final boolean recreate
    // ----------- Flags and other instance properties -------------
    /**
     * Default constructor
     * @param recreate : Remove and Create VIEW each time (recommended)
     */
    View(boolean recreate = true) {
        this("", null, recreate)
    }
    /**
     * Constructor. A Database object can be passed
     * when using multiple databases.
     *
     * @param database
     * @param sql
     */
    View(Database database, boolean recreate = true) {
        this("", database, recreate)
    }
    /**
     * Constructor. A Database object can be passed
     * when using multiple databases.
     *
     * @param name : Alternative way to set table name (besides @TableMeta)
     * @param database
     */
    View(String name, Database database, boolean recreate = true) {
        super(name, database) //Here name will be automatically set if empty
        this.recreate = recreate
        assert create() : "Failed to create view/table: $name"
    }
    /**
     * Execute SQL to create view
     */
    boolean create() {
        boolean ok = false
        if(createSQL != "") {
            switch (jdbc) {
                case AutoJDBC:
                    // Initialize Auto
                    DB conn = connect()
                    jdbc.initialize(conn)
                    boolean exists = conn.exists()
                    if (exists && recreate) {
                        drop()
                        exists = false
                    }
                    if (! exists) {
                        ok = conn.setSQL(createSQL)
                    }
                    conn.close()
                    break
                default:
                    Log.w("CREATE VIEW can not be used with specified database. Please check the documentation to know which databases are supported.", jdbc.toString())
                    ok = false
                    break
            }
        } else {
            ok = true
        }
        return ok
    }

    @Override
    boolean drop() {
        boolean dropped = false
        switch (jdbc) {
            case AutoJDBC:
                // Initialize Auto
                DB conn = connect()
                jdbc.initialize(conn)
                boolean exists = conn.exists()
                if (exists) {
                    dropped = drop(true)
                }
                conn.close()
                break
            default:
                Log.w("DROP VIEW can not be used with specified database. Please check the documentation to know which databases are supported.", jdbc.toString())
                break
        }
        return dropped
    }
}
