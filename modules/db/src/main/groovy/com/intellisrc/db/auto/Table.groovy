package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.ColumnInfo
import com.intellisrc.db.ColumnType
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.TableDefinition
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.db.annot.ViewMeta
import com.intellisrc.etc.Instanciable
import groovy.transform.CompileStatic

import java.lang.annotation.Annotation
import java.lang.reflect.Field

import static com.intellisrc.db.jdbc.JDBC.BooleanHandle as BoolType

@CompileStatic
class Table<M extends Model> extends Relational<M> implements Instanciable<M> {
    // Keep relation of tables:
    protected Map<String, Boolean> versionChecked = [:] // it will be set to true after the version has been checked
    // ----------- Flags and other instance properties -------------
    boolean autoUpdate = true // set to false if you don't want the table to update automatically
    /**
     * Constructor. A Database object can be passed
     * when using multiple databases.
     *
     * @param database
     */
    Table(Database database) {
        this("", database)
    }
    /**
     * Constructor. A Database object can be passed
     * when using multiple databases.
     *
     * @param name : Alternative way to set table name (besides @TableMeta)
     * @param database
     */
    Table(String name = "", Database database = null) {
        super(name, database)
        TableMeta meta = this.class.getAnnotation(TableMeta)
        this.autoUpdate = (meta && meta.hasProperty("autoUpdate") ? meta.properties.autoUpdate : this.autoUpdate) as boolean
        updateOrCreate()
    }
    /**
     * Decide if table needs to be updated or created
     */
    void updateOrCreate() {
        if(!versionChecked.containsKey(tableName) || !versionChecked[tableName]) {
            versionChecked[tableName.toString()] = true
            //noinspection GroovyFallthrough
            switch (jdbc) {
                case AutoJDBC:
                    // Initialize Auto
                    DB conn = connect()
                    (jdbc as AutoJDBC).autoInit()
                    boolean exists = conn.exists()
                    if (exists) {
                        if(autoUpdate) {
                            int version = TableUpdater.getTableVersion(conn, tableName.toString())
                            if (definedVersion != version) {
                                updateTable()
                            } else {
                                boolean updated = false
                                // The following only applies if the field @Column type is "boolean":
                                getFields().findAll { [boolean,Boolean].contains(it.type) }.each {
                                    Field field ->
                                        String fname = field.name.toSnakeCase()
                                        // 'ct' is what the database is reporting
                                        ColumnInfo ci = conn.info(fname, true)
                                        if(ci) {
                                            ColumnType ct = ci.type
                                            // booleanHandle is what the column in the database should be (according to Database type):
                                            boolean needUpdate = switch (jdbc.booleanHandle) {
                                                case BoolType.ENUM,
                                                     BoolType.CHAR      -> ct != ColumnType.TEXT
                                                case BoolType.BOOLEAN   -> ct != ColumnType.BOOLEAN
                                                case BoolType.NUMBER    -> ct != ColumnType.INTEGER
                                            }
                                            if (needUpdate) {
                                                updated = true
                                                BoolType from = switch (true) {
                                                    case ct == ColumnType.INTEGER || ct == ColumnType.BOOLEAN -> BoolType.BOOLEAN
                                                    case ColumnType.TEXT && ci.length == 1 -> BoolType.CHAR
                                                    case ColumnType.TEXT && ci.length > 4 -> BoolType.ENUM
                                                }
                                                if (getUpdateBooleanQuery(tableName.toString(), fname, from).every {
                                                    return conn.setSQL(it)
                                                }) {
                                                    Log.i("Table [%s] . [%s] boolean type was updated", tableName, field.name)
                                                } else {
                                                    Log.w("There were problems trying to update boolean field: [%s] . [%s]", tableName, field.name)
                                                }
                                            }
                                        } else {
                                            Log.w("ColumnType of [%s] was null", fname)
                                        }
                                }
                                if (!updated) {
                                    Log.v("Table [%s] doesn't need to be updated: [Code: %d] vs [DB: %d]",
                                        tableName, definedVersion, version)
                                }
                            }
                        }
                    } else {
                        if(!createTable()) {
                            Log.w("Table [%s] was not created.", tableName)
                        }
                    }
                    conn.close()
                    break
                default:
                    Log.w("Create or Update : Database type (%s) can not be updated automatically. Please check the documentation to know which databases are supported.", jdbc.toString())
                    return
                    break
            }
        }
    }
    /**
     * Update database table
     * @return
     */
    boolean updateTable() {
        Log.i("Updating table [%s] to version [%d]", tableName, definedVersion)
        boolean ok = TableUpdater.update([this])
        if(!ok) {
            Log.w("Table [%s] was not updated.", tableName)
        }
        return ok
    }
    /**
     * Create the database table based on @Column and @TableMeta
     * @param copyName : if set, will create a table with another name (as copy)
     */
    boolean createTable(String copyName = "") {
        boolean ok = false
        String tableNameToCreate = copyName ?: tableName
        DB db = connect()
        if (!db.getTables(false).contains(tableNameToCreate)) {
            String charset = "utf8"
            String engine = ""
            if (this.class.isAnnotationPresent(TableMeta) || this.class.isAnnotationPresent(ViewMeta)) {
                Annotation meta = this.class.getAnnotation(ViewMeta) ?: this.class.getAnnotation(TableMeta)
                if (meta.hasProperty("engine") && meta.properties.engine.toString() != "auto") {
                    engine = meta.properties.engine.toString()
                }
                if(meta.hasProperty("charset")) {
                    charset = meta.properties.charset.toString()
                }
            }
            TableDefinition definition = columns.collect { it.normalized } as TableDefinition
            ok = jdbc.createTable(tableNameToCreate, definition, charset, engine, definedVersion)
        }
        db.close()
        return ok
    }
    /**
     * Change Boolean type (store type)
     * @param table
     * @param column
     * @return list of queries to execute to fix column and data
     */
    List<String> getUpdateBooleanQuery(String table, String column, BoolType from) {
        List<String> queries = []
        String boolDef = jdbc.getColumnDefinition(new ColumnInfo(type: ColumnType.BOOLEAN).normalized)
        String varChar = jdbc.getColumnDefinition(new ColumnInfo(type: ColumnType.VARCHAR).normalized)
        column = jdbc.getFieldForQuery(column)
        BoolType to = jdbc.booleanHandle
        // Pre-modification query:
        //noinspection GroovyFallthrough
        switch (true) {
            // We need to update from 'true' -> y, 'false' -> n
            case from == BoolType.ENUM && to == BoolType.CHAR:
                // First we need to change the column to VARCHAR
                queries << "ALTER TABLE ${ jdbc.getTableForQuery(table) } CHANGE COLUMN $column $column $varChar".toString()
                // Then replace the values
                queries << ("UPDATE ${ jdbc.getTableForQuery(table) } " +
                    "SET $column = CASE " +
                    "WHEN $column = 'true' THEN '${ jdbc.getTrueChar() }' " +
                    "WHEN $column = 'false' THEN '${ jdbc.getFalseChar() }' " +
                    "END").toString()
                break

            // We need to update from 'y' -> 1, 'n' -> 0
            case from == BoolType.CHAR && to == BoolType.BOOLEAN:
            case from == BoolType.CHAR && to == BoolType.NUMBER:
                queries << ("UPDATE ${ jdbc.getTableForQuery(table) } " +
                            "SET $column = CASE " +
                                "WHEN $column = '${ jdbc.getTrueChar() }' THEN 1 " +
                                "WHEN $column = '${ jdbc.getFalseChar() }' THEN 0 " +
                            "END").toString()
                break

            // We need to update from 'y' -> 'true', 'n' -> 'false'
            case from == BoolType.CHAR && to == BoolType.ENUM:
                // First we need to change the column to VARCHAR
                queries << "ALTER TABLE ${ jdbc.getTableForQuery(table) } CHANGE COLUMN $column $column $varChar".toString()
                // Then replace the values
                queries << ("UPDATE ${ jdbc.getTableForQuery(table) } " +
                    "SET $column = CASE " +
                    "WHEN $column = '${ jdbc.getTrueChar() }' THEN 'true' " +
                    "WHEN $column = '${ jdbc.getFalseChar() }' THEN 'false' " +
                    "END").toString()
                break
        }
        // Modification query:
        queries << "ALTER TABLE ${ jdbc.getTableForQuery(table) } CHANGE COLUMN $column $column $boolDef".toString()
        // Post-modification query:
        //noinspection GroovyFallthrough
        switch (true) {
            // We need to Update values from 1 -> 0, 2 -> 1 (as number as we already changed the column)
            case from == BoolType.ENUM && to == BoolType.BOOLEAN:
            case from == BoolType.ENUM && to == BoolType.NUMBER:
                queries << ("UPDATE ${ jdbc.getTableForQuery(table) } " +
                    "SET $column = CASE " +
                    "WHEN $column = 1 THEN 0 " +
                    "WHEN $column = 2 THEN 1 " +
                    "END").toString()
                break
            // We need to update from '0' -> 'n', '1' -> 'y' (as char as we already changed the column)
            case from == BoolType.BOOLEAN && to == BoolType.CHAR:
            case from == BoolType.NUMBER && to == BoolType.CHAR:
                queries << ("UPDATE ${ jdbc.getTableForQuery(table) } " +
                    "SET $column = CASE " +
                    "WHEN $column = '0' THEN '${ jdbc.getTrueChar() }' " +
                    "WHEN $column = '1' THEN '${ jdbc.getFalseChar() }' " +
                    "END").toString()

                break
            // We need to update from 1 -> 'TRUE', 0 -> 'FALSE' (as number as we already changed the column)
            case from == BoolType.BOOLEAN && to == BoolType.ENUM:
            case from == BoolType.NUMBER && to == BoolType.ENUM:
                queries << ("UPDATE ${ jdbc.getTableForQuery(table) } " +
                    "SET $column = CASE " +
                    "WHEN $column = 1 THEN 'TRUE' " +
                    "WHEN $column = 0 THEN 'FALSE' " +
                    "END").toString()
                break
        }
        return queries
    }

    /**
     * Get the defined version in code
     * @return
     */
    int getDefinedVersion() {
        int version = 1
        if(parametrizedInstance.class.isAnnotationPresent(ModelMeta)) {
            ModelMeta meta = parametrizedInstance.class.getAnnotation(ModelMeta)
            version = meta.version()
        }
        return version
    }

    /**
     * Returns the autoincrement field
     * @return
     */
    String getAutoIncrement() {
        String ai = ""
        Field field = getFields().find {
            it.getAnnotation(Column)?.autoincrement()
        }
        if(field) {
            ai = getColumnName(field)
        }
        return ai
    }
    /**
     * Update a model
     * @param model
     * @param exclude : columns to exclude during update
     * @return
     */
    boolean update(M model, Collection<String> exclude = []) {
        boolean ok = false
        DB db = connect().keys(primaryKeys)
        try {
            Map map = getMap(model)
            // id can be a List or the value of the field
            Object id = (primaryKeys.empty ? null : (primaryKeys.size() == 1) ? map[primaryKey] : primaryKeys.collect {map[it] })
            primaryKeys.each {
                exclude << it// Exclude pk from map
            }
            exclude.each {
                map.remove(it)
            }
            ok = db.update(map, id)
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            db.close()
        }
        return ok
    }
    /**
     * Update multiple models
     * @param models
     * @return
     */
    boolean update(Collection<M> models) {
        DB db = connect()
        boolean singlePk = false
        boolean multiPk = false
        if(primaryKey) {
            db.keys(primaryKeys)
            singlePk = primaryKeys.size() == 1
            multiPk = primaryKeys.size() > 1
        }
        boolean ok = db.update(models.collect {
            it.toDB()
        }, models.collect {
            return singlePk ? [(primaryKey): it.toDB().get(primaryKey)] :
                (multiPk ? it.toDB().subMap(primaryKeys) : [])
        })
        db.close()
        return ok
    }
    /**
     * Replace a model
     * @param model
     * @return
     */
    boolean replace(M model, Collection<String> exclude = []) {
        boolean ok = false
        DB db = connect()
        try {
            Map<String, Object> map = getMap(model)
            exclude.each {
                map.remove(it)
            }
            db.keys(primaryKeys)
            ok = db.replace(map)
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            db.close()
        }
        return ok
    }
    /**
     * Replace multiple models
     * @param models
     * @return
     */
    boolean replace(Collection<M> models) {
        DB db = connect().keys(primaryKeys)
        boolean ok = db.replace(models.collect { it.toDB() })
        db.close()
        return ok
    }
    /**
     * Delete using model
     * @param model
     * @return
     */
    boolean delete(M model) {
        return model.uniqueId ? delete(model.uniqueId) : delete(getMap(model))
    }
    /**
     * Delete with ID
     * @param id
     * @return
     */
    boolean delete(int id) {
        DB db = connect().keys(primaryKeys)
        boolean ok = db.delete(id)
        db.close()
        return ok
    }
    /**
     * Delete using multiple columns
     * @param map
     * @return
     */
    boolean delete(Map map) {
        DB db = connect().keys(primaryKeys)
        map = convertToDB(map)
        boolean ok = db.delete(map)
        db.close()
        return ok
    }
    /**
     * Delete using PK. Does not support multiple PKs
     * @param ids
     * @return
     */
    boolean deleteByPK(Collection<Integer> ids) {
        DB db = connect()
        boolean ok = false
        if(primaryKey) {
            db.keys(primaryKeys)
            if(primaryKeys.size() == 1) {
                ok = ids.empty || db.delete(ids)
            } else {
                Log.w("Trying to delete using PL in a multiPK table: %s", tableName)
            }
        } else {
            Log.w("Trying to delete using PK. No PK defined for table: %s", tableName)
        }
        db.close()
        return ok
    }
    /**
     * Delete using multiple Models
     * @param ids
     * @return
     */
    boolean delete(Collection<M> models) {
        DB db = connect()

        boolean singlePk = false
        boolean multiPk = false
        if(primaryKey) {
            db.keys(primaryKeys)
            singlePk = primaryKeys.size() == 1
            multiPk = primaryKeys.size() > 1
        }
        boolean ok = db.delete(models.collect {
            return singlePk ? it.toDB().get(primaryKey) :
                (multiPk ? it.toDB().subMap(primaryKeys) : [])
        })
        db.close()
        return ok
    }

    /**
     * Delete all rows which match a criteria. If there is no criteria,
     * it will delete all rows in a table
     * @param criteria
     * @return
     */
    boolean deleteAll(Map<String, Object> criteria = [:]) {
        DB db = connect().keys(primaryKeys)
        boolean ok = criteria.isEmpty() ? (db.truncate() ?: db.clear()) : db.delete(criteria.collectEntries {
            boolean isModel = it.value instanceof Model
            return [(isModel ? it.key + "_id" : it.key) : (isModel ? (it.value as Model).uniqueId : it.value)]
        })
        db.close()
        return ok
    }
    /**
     * Alias for deleteAll without criteria
     * @return
     */
    boolean clear() {
        return deleteAll()
    }
    /**
     * Reset autoincrement
     * @return
     */
    boolean resetAutoIncrement() {
        return jdbc.resetAutoIncrement(name)
    }
    /**
     * Insert a model
     * @param model
     * @return
     */
    int insert(M model) {
        String ai = getAutoIncrement()
        int lastId = 0
        DB db = connect()
        try {
            Map<String, Object> map = getMap(model)
            if(map.containsKey(ai) && map[ai] == 0) {
                map.remove(ai)
            }
            db.keys(primaryKeys)
            boolean ok = db.insert(map)
            lastId = 0
            if (ok) {
                if(ai) {
                    lastId = db.lastID
                    model[ai] = lastId
                }
            } else {
                Log.w("Unable to insert row : %s", map.toSpreadMap())
            }
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            db.close()
        }
        return lastId
    }
    /**
     * Insert multiple models
     * @param models
     * @return
     */
    boolean insert(Collection<M> models) {
        DB db = connect().keys(primaryKeys)
        boolean ok = db.insert(models.collect { it.toDB() })
        db.close()
        return ok
    }
    /**
     * Return table updater
     * Override when needed
     * @return
     */
    @SuppressWarnings('GrMethodMayBeStatic')
    List<Map> onUpdate(Collection<Map> data) {
        return data.toList()
    }
    /**
     * Decide if manual update is required when version changes
     * @param table
     * @param prevVersion
     * @param currVersion
     * @return
     */
    @SuppressWarnings('GrMethodMayBeStatic')
    boolean execOnUpdate(DB table, int prevVersion, int currVersion) {
        return false
    }

    @Override
    void reset() {
        super.reset()
        versionChecked = [:]
    }
}
