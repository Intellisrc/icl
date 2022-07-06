package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import groovy.transform.CompileStatic

/**
 * Updates database tables
 * In summary, it will create a backup of a table,
 * drop it, create it and restore the data from
 * the backup. If the data can be inserted without
 * having to change the data or the structure (for
 * example, if an index was added), then you don't
 * need to worry about passing a RecordUpdater,
 * otherwise, you will need to check and change the
 * data before inserting it back using such interface.
 *
 * @since 2021/07/20.
 */
@CompileStatic
class TableUpdater {
    protected static class TableInfo {
        Table table
        String getName() {
            return table.name
        }
        String getBackName() {
            return name + "__back"
        }
    }
    /**
     * Update a list of Table (instances)
     *
     * NOTE: Adding columns or performing minor column changes (like length,
     *      onDelete conditions, unique constrains, indices, auto-increment,
     *      etc) may not need to use `onUpdate`.
     *
     * @param tables
     * @param recordUpdater
     */
    static boolean update(List<Table> tableList) {
        DB db = Database.default.connect()
        Table.alwaysCheck = true
        DB.disableCache = true

        boolean ok = false

        List<TableInfo> tables = []
        tableList.each {
            tables << new TableInfo(
                table: it
            )
        }
        switch (db.jdbc) {
            case AutoJDBC:
                AutoJDBC auto = db.jdbc as AutoJDBC
                auto.autoInit(db)

                if(auto.turnFK(db, false)) {
                    ok = !tables.any {
                        // It will stop if some table fails to create
                        TableInfo info ->
                            if (!db.table(info.backName).exists()) {
                                if (!auto.copyTable(db, info.name, info.backName)) {
                                    db.table(info.backName).drop()
                                    return true //failed
                                }
                            }
                            return !db.table(info.backName).exists()
                    }
                    if (ok) {
                        tables.reverseEach {
                            if (db.table(it.name).exists()) {
                                db.table(it.name).drop()
                            }
                        }
                        ok = !tables.any {
                            it.table.createTable()
                            return !db.table(it.name).exists()
                        }
                        if (ok) {
                            tables.each {
                                TableInfo info ->
                                    int version = getTableVersion(db, info.name)
                                    if (info.table.execOnUpdate(db.table(info.backName), version, info.table.definedVersion)) {
                                        List<Map> newData = info.table.onUpdate(db.table(info.backName).get().toListMap())
                                        ok = db.table(info.name).insert(newData)
                                    } else {
                                        ok = auto.copyTableData(db, info.backName, info.name)
                                        if (!ok) {
                                            // Probably column mismatch (using row by row method):
                                            List<String> columnsOld = db.table(info.backName).info().collect { it.name }
                                            List<String> columnsNew = db.table(info.name).info().collect { it.name }
                                            List<String> columnsAdded = columnsNew - columnsOld
                                            List<String> columnsRemoved = columnsOld - columnsNew
                                            List<Map> newData = db.table(info.backName).get().toListMap().collect {
                                                Map row ->
                                                    if (!columnsAdded.empty) {
                                                        columnsAdded.each {
                                                            row[it] = null
                                                        }
                                                    }
                                                    if (!columnsRemoved.empty) {
                                                        columnsRemoved.each {
                                                            row.remove(it)
                                                        }
                                                    }
                                                    return row
                                            }
                                            Log.i("(Fast import failed) Trying alternative way to import data (it may take some time)...")
                                            ok = db.table(info.name).insert(newData)
                                            if (ok) {
                                                Log.i("Data was successfully imported.")
                                            } else {
                                                Log.w("Unable to import data to the new table structure. Try setting `execOnUpdate()` to true, and handle the data change in `onUpdate()`.")
                                            }
                                        }
                                    }
                            }
                        }
                        if (ok) {
                            tables.each {
                                TableInfo info ->
                                    db.table(info.backName).drop()
                                    // Replace the table version:
                                    auto.setVersion(db, db.jdbc.dbname, info.name, info.table.definedVersion)
                            }
                        } else {
                            Log.w("Update failed!. Rolled back.")
                            tables.each {
                                TableInfo info ->
                                    db.table(info.name).drop()
                                    if (auto.copyTable(db, info.backName, info.name)) {
                                        db.table(info.backName).drop()
                                    } else {
                                        db.table(info.name).drop() // Be sure it doesn't exists
                                        if (!auto.renameTable(db, info.backName, info.name)) {
                                            Log.w("Unable to rollback update. Please check table: [%s] manually.", info.name)
                                            Log.w("    a backup of original table may exists with name: ", info.backName)
                                        }
                                    }
                            }
                        }
                    }
                    auto.turnFK(db, true)
                }
                break
            default:
                Log.w("Database type is not supported yet (it can not be updated automatically). Please check the documentation to see which databases are supported.")
                return ok
        }
        db.close()

        Table.alwaysCheck = false
        DB.disableCache = false
        return ok
    }

    /**
     * Get table version
     * @param db
     * @param auto
     * @param table
     * @return
     */
    static int getTableVersion(DB db, String table) {
        return (db.jdbc as AutoJDBC).getVersion(db, db.jdbc.dbname, table) ?: 1
    }
    /**
     * Close all connections to the default database
     */
    static void quit() {
        Database.default.quit()
    }
}
