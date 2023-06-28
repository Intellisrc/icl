package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.db.annot.ViewMeta
import com.intellisrc.etc.Instanciable
import groovy.transform.CompileStatic

import java.lang.annotation.Annotation
import java.lang.reflect.Field

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
        this.autoUpdate = (meta && meta.hasProperty("autoUpdate") ? meta.properties.autoUpdate : false) as boolean
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
                    (jdbc as AutoJDBC).autoInit(conn)
                    boolean exists = conn.exists()
                    if (exists) {
                        if(autoUpdate) {
                            int version = TableUpdater.getTableVersion(conn, tableName.toString())
                            if (definedVersion != version) {
                                updateTable()
                            } else {
                                Log.d("Table [%s] doesn't need to be updated: [Code: %d] vs [DB: %d]",
                                    tableName, definedVersion, version)
                            }
                        }
                    } else {
                        if(!createTable(conn)) {
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
    boolean createTable(DB db, String copyName = "") {
        boolean ok = false
        String tableNameToCreate = copyName ?: tableName
        if (!db.tables.contains(tableNameToCreate)) {
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
            AutoJDBC auto = jdbc as AutoJDBC
            ok = auto.createTable(connect(), tableNameToCreate, charset, engine, definedVersion, columns)
        }
        return ok
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
        DB db = connect()
        if(pk) { db.keys(pks) }
        try {
            Map map = getMap(model)
            // id can be a List or the value of the field
            Object id = (pks.empty ? null : (pks.size() == 1) ? map[pk] : pks.collect {map[it] })
            pks.each {
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
        if(pk) {
            db.keys(pks)
            singlePk = pks.size() == 1
            multiPk = pks.size() > 1
        }
        boolean ok = db.update(models.collect {
            it.toMap()
        }, models.collect {
            return singlePk ? [(pk) : it.toMap().get(pk)] :
                (multiPk ? it.toMap().subMap(pks) : [])
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
        DB db = connect()
        boolean ok = db.replace(models.collect { it.toMap() })
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
        DB db = connect()
        if(pk) { db.keys(pks) }
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
        DB db = connect()
        if(pk) { db.keys(pks) }
        map = convertToDB(map)
        boolean ok = db.delete(map)
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
        if(pk) {
            db.keys(pks)
            singlePk = pks.size() == 1
            multiPk = pks.size() > 1
        }
        boolean ok = db.delete(models.collect {
            return singlePk ? it.toMap().get(pk) :
                (multiPk ? it.toMap().subMap(pks) : [])
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
        DB db = connect()
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
        DB db = connect()
        boolean ok = db.insert(models.collect { it.toMap() })
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
