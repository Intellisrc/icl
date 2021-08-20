package com.intellisrc.db

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import static com.intellisrc.db.DB.DBType.*

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
    static interface BaseUpdater {}
    /**
     * Interface used to update values before inserting them.
     * Useful specially when columns were removed or require data conversion.
     * If a row fails, all fails
     */
    static interface RecordsUpdater extends BaseUpdater {
        List<Map> fix(List<Map> records)
    }
    /**
     * Interface used to update each row that is going to be inserted
     * Useful specially when columns were removed or require data conversion.
     * If a row fails, it will ignore and continue (just warn)
     */
    static interface RecordUpdater extends BaseUpdater {
        Map fix(Map record)
    }
    /**
     * Update a list of Table (instances)
     * If a table has `onUpdate` interface set,
     *
     * NOTE: Adding columns or performing minor column changes (like length,
     *      onDelete conditions, unique constrains, indices, auto-increment,
     *      etc) may not need to use a onUpdate.
     *
     * @param tables
     * @param recordUpdater
     */
    static void update(List<Table> tableList) {
        DB db = Database.default.connect()
        Table.alwaysCheck = true
        DB.disableCache = true

        if(db.type != MYSQL && db.type != MARIADB) {
            Log.w("Only MySQL/MariaDB is supported for now.")
            return
        }
        db.exec(new Query("SET FOREIGN_KEY_CHECKS=0"))
        List<TableInfo> tables = []
        tableList.each {
            tables << new TableInfo(
                    table : it
            )
        }
        boolean ok = ! tables.any { // It will stop if some table fails to create
            TableInfo info ->
                if(! db.table(info.backName).exists()) {
                    db.exec(new Query("CREATE TABLE `${info.backName}` LIKE `${info.name}`"))
                    db.exec(new Query("INSERT INTO `${info.backName}` SELECT * FROM `${info.name}`"))
                }
                return ! db.table(info.backName).exists()
        }
        if(ok) {
            tables.reverseEach {
                if(db.table(it.name).exists()) {
                    db.table(it.name).drop()
                }
            }
            ok = ! tables.any {
                it.table.createTable()
                return ! db.table(it.name).exists()
            }
            if(ok) {
                tables.each {
                    TableInfo info ->
                        if (info.table.manualUpdate(db.table(info.backName))) {
                            switch (info.table.onUpdate) {
                                case RecordsUpdater:
                                    List<Map> newData = (info.table.onUpdate as RecordsUpdater).fix(db.table(info.backName).get().toListMap())
                                    ok = db.table(info.name).insert(newData)
                                    break
                                case RecordUpdater:
                                    db.table(info.backName).get().toListMap().each {
                                        Map newRow = (info.table.onUpdate as RecordUpdater).fix(it)
                                        if(! db.table(info.name).insert(newRow)) {
                                            Log.w("Inserting row failed: %s", newRow)
                                        }
                                    }
                                    ok = true
                                    break
                                default:
                                    Log.w("You must choose between `RecordsUpdater` or `RecordUpdater` interfaces (see documentation)")
                            }
                        } else {
                            db.exec(new Query("INSERT IGNORE INTO `${info.name}` SELECT * FROM `${info.backName}`"))
                        }
                }
            }
            if(ok) {
                tables.each {
                    TableInfo info ->
                        db.table(info.backName).drop()
                }
            } else {
                Log.w("Update failed!. Rolled back.")
                tables.each {
                    TableInfo info ->
                        db.table(info.name).drop()
                        db.exec(new Query("CREATE TABLE `${info.name}` LIKE `${info.backName}`"))
                        db.exec(new Query("INSERT INTO `${info.name}` SELECT * FROM `${info.backName}`"))
                        db.table(info.backName).drop()
                        // Replace the table version:
                        String comment = info.table.comment
                        String version = "v." + info.table.definedVersion
                        if(comment =~ /v.(\d+)/) {
                            comment = comment.replaceAll(/v.(\d+)/, version)
                        } else {
                            comment = (comment.empty ? "" : " ") + "${version}"
                        }
                        db.exec(new Query("ALTER TABLE `${info.name}` COMMENT = '${comment}'"))
                }
            }
        }
        db.exec(new Query("SET FOREIGN_KEY_CHECKS=1"))
        db.close()

        Table.alwaysCheck = false
        DB.disableCache = false
    }
    /**
     * Close all connections to the default database
     */
    static void quit() {
        Database.default.quit()
    }
}
