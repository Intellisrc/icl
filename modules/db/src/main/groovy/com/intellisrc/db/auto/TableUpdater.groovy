package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
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
        int autoIncrement = 0
        String pkKey
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
    static boolean update(Collection<Table> tableList) {
        DB.clearCache()
        boolean origEnabled = DB.enableCache
        DB.enableCache = false

        boolean ok = false
        List<TableInfo> tables = []
        tableList.each {
            DB db = it.connect(true)
            tables << new TableInfo(
                table: it,
                autoIncrement: it.identityValue,
                pkKey: it.identityField
            )
            switch (db.jdbc) {
                case AutoJDBC:
                    if(db.turnFK(false)) {
                        ok = tables.every {
                            // It will stop if some table fails to create
                            TableInfo info ->
                                boolean ok2 = true
                                int records = 0
                                // Be sure we don't have garbage:
                                if (db.hasTable(info.backName)) {
                                    ok2 = db.table(info.backName).drop()
                                }
                                if(ok2) {
                                    records = db.table(info.name).count().get().toInt()
                                    // We copy the data into our back table (and create)
                                    if (db.cloneTable(info.name, info.backName, info.table.definition, false)) {
                                        // If all went fine, we drop the 'old' version table
                                        ok2 = db.table(info.name).drop()
                                    } else {
                                        // in case it exists, drop the backup table to prevent name collision:
                                        db.table(info.backName).drop()
                                        // If we were unable to copy it, we rename it instead:
                                        ok2 = db.renameTable(info.name, info.backName)
                                    }
                                } else {
                                    Log.w("Failed to drop backup table: %s", info.backName)
                                    // we continue, as it is possible that we can use the back table (otherwise it will fail the record's count below)
                                }
                                if(ok2) {
                                    int recordsAfterBackup = db.table(info.backName).count().get().toInt()
                                    // Verify that both tables have the same number of records:
                                    if(recordsAfterBackup != records) {
                                        Log.w("Data was not successfully backed up (%d vs %d records), aborting", records, recordsAfterBackup)
                                        db.table(info.name).drop()
                                        db.renameTable(info.backName, info.name)
                                        db.setAutoIncrement(info.name, info.pkKey, info.autoIncrement)
                                        return false
                                    } else { // Record cound is the same, create the new table or abort:
                                        if (! info.table.createTable(info.name)) { //Creating new table
                                            Log.w("Unable to copy table. Reverting")
                                            db.table(info.name).drop()
                                            db.renameTable(info.backName, info.name)
                                            db.setAutoIncrement(info.name, info.pkKey, info.autoIncrement)
                                            return false //failed
                                        } // else, keep going...
                                    }
                                } else {
                                    Log.w("Failed to create backup table of: %s", info.name)
                                }
                                return db.table(info.backName).exists() // Be sure that all back tables exists
                        }
                        if (ok) {
                            tables.each {
                                TableInfo info ->
                                    int version = db.getVersion(info.name)
                                    // Execute custom code before updating (it must return true):
                                    if (info.table.execOnUpdate(db.table(info.backName), version, info.table.definedVersion)) {
                                        List<Map> newData = info.table.onUpdate(db.table(info.backName).get().toListMap())
                                        // Insert data and copy auto-increment from back table:
                                        ok = db.table(info.name).insert(newData)
                                        ok &= db.setAutoIncrement(info.name, info.pkKey, info.autoIncrement)
                                    } else {
                                        // Try to copy over the data we have in the back table:
                                        boolean dataCopied = db.copyTableData(info.backName, info.name, info.table.definition)
                                        // Be sure that we have the same number of rows:
                                        boolean countMatch = dataCopied && db.table(info.name).count().get().toInt() == db.table(info.backName).count().get().toInt()
                                        ok = countMatch
                                        if (!ok) { // Probably column mismatch (using row by row method):
                                            Log.i("(Fast import failed) Trying alternative way to import data (it may take some time)...")
                                            List<String> columnsOld = db.table(info.backName).info(false).collect { it.name }
                                            List<String> columnsNew = db.table(info.name).info(false).collect { it.name }
                                            Log.i("Old columns: %d, New columns: %d", columnsOld.size(), columnsNew.size())
                                            columnsNew = db.table(info.name).info(false).collect { it.name }
                                            if(! columnsNew.empty) {
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
                                                ok = newData.empty ?: db.table(info.name).insert(newData) &&
                                                     db.table(info.name).count().get().toInt() == db.table(info.backName).count().get().toInt()
                                                if (ok) {
                                                    Log.i("Data was successfully imported.")
                                                } else {
                                                    Log.w("Unable to import data to the new table structure. Try setting `execOnUpdate()` to true, and handle the data change in `onUpdate()`.")
                                                }
                                            } else {
                                                Log.w("Unable to create new table.")
                                            }
                                        }
                                        // Copy the old auto-increment value to the new table:
                                        ok &= db.setAutoIncrement(info.name, info.pkKey, info.autoIncrement)
                                    }
                            }
                        }
                        // Finally, drop back tables:
                        if (ok) {
                            tables.each {
                                TableInfo info ->
                                    db.table(info.backName).drop()
                                    // Replace the table version:
                                    db.setVersion(info.name, info.table.definedVersion)
                            }
                        } else {
                            Log.w("Update failed!. Rolled back.")
                            tables.each {
                                TableInfo info ->
                                    if(db.table(info.backName).exists()) {
                                        db.table(info.name).drop()
                                        if (!db.renameTable(info.backName, info.name)) {
                                            Log.w("Unable to rollback update. Please check table: [%s] manually.", info.name)
                                            Log.w("    a backup of original table may exists with name: ", info.backName)
                                        }
                                    }
                            }
                        }
                        db.turnFK(true)
                    }
                    break
                default:
                    Log.w("Database type is not supported yet (it can not be updated automatically). Please check the documentation to see which databases are supported.")
                    return ok
            }
            db.close()
        }
        // Revert to original value:
        DB.enableCache = origEnabled
        return ok
    }
}
