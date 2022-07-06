package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.etc.Instanciable
import com.intellisrc.etc.YAML
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@CompileStatic
class Table<M extends Model> implements Instanciable<M> {
    static boolean alwaysCheck = false  //Used by updater
    static protected Map<String, Boolean> versionChecked = [:] // it will be set to true after the version has been checked
    static protected Map<String, Table> relation = [:]
    boolean autoUpdate = true // set to false if you don't want the table to update automatically
    protected Database database
    protected final String name
    protected Set<DB> activeConnections = []
    protected int cache = 0
    protected boolean clearCache = false

    /**
     * Interface used to update values before inserting them.
     * Useful specially when columns were removed or require data conversion.
     * If a row fails, all fails
     */
    static interface RecordsUpdater {
        List<Map> fix(List<Map> records)
    }
    /**
     * Information about a Field that will be used as column in a DB
     * @see Column
     */
    static class ColumnDB {
        String name
        Class<?> type
        Object defaultVal
        Column annotation
    }
    /**
     * Constructor. A Database object can be passed
     * when using multiple databases.
     *
     * @param name
     * @param database
     */
    Table(String name = "", Database database = null) {
        this.database = database ?: Database.getDefault()
        TableMeta meta = this.class.getAnnotation(TableMeta)
        this.name = name ?: meta?.name() ?: this.class.simpleName.toSnakeCase()
        if(meta) {
            this.cache = meta.cache()
            this.clearCache = meta.clearCache()
            this.autoUpdate = meta.autoUpdate()
        }
        assert this.name : "Table name not set"
        updateOrCreate()
        relation[parametrizedInstance.class.name] = this
    }
    /**
     * Decide if table needs to be updated or created
     */
    void updateOrCreate() {
        if(alwaysCheck || !versionChecked.containsKey(tableName) || !versionChecked[tableName]) {
            versionChecked[tableName.toString()] = true
            //noinspection GroovyFallthrough
            switch (tableConnector.jdbc) {
                case AutoJDBC:
                    // Initialize Auto
                    (tableConnector.jdbc as AutoJDBC).autoInit(tableConnector)
                    boolean exists = tableConnector.exists()
                    if (exists) {
                        if(autoUpdate) {
                            int version = TableUpdater.getTableVersion(tableConnector, tableName.toString())
                            if (definedVersion > version) {
                                Log.i("Table [%s] is going to be updated from version: %d to %d",
                                    tableName, version, definedVersion)
                                if(!TableUpdater.update([this])) {
                                    Log.w("Table [%s] was not updated.", tableName)
                                }
                            } else {
                                Log.d("Table [%s] doesn't need to be updated: [Code: %d] vs [DB: %d]",
                                    tableName, version, definedVersion)
                            }
                        }
                    } else {
                        createTable()
                    }
                    break
                default:
                    Log.w("Create or Update : Database type can not be updated automatically. Please check the documentation to know which databases are supported.")
                    return
                    break
            }
        }
    }
    /**
     * Create the database based on @Column and @TableMeta
     */
    void createTable() {
        if (!tableConnector.exists()) {
            String charset = "utf8"
            String engine = ""
            if (this.class.isAnnotationPresent(TableMeta)) {
                TableMeta meta = this.class.getAnnotation(TableMeta)
                if (meta.engine() != "auto") {
                    engine = meta.engine()
                }
                charset = meta.charset()
            }
            AutoJDBC auto = tableConnector.jdbc as AutoJDBC
            auto.createTable(tableConnector, tableName, charset, engine, definedVersion, columns)
        }
        closeConnections()
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
     * Return the list of fields annotated with Column from the Model class
     * @return
     */
    List<Field> getFields() {
        int index = 0
        return getParametrizedInstance(index).class.declaredFields.findAll {
            boolean inc = false
            if(!it.synthetic && it.isAnnotationPresent(Column)) {
                inc = true
                it.setAccessible(true)
                if (Modifier.isPrivate(it.modifiers)) {
                    Modifier.setPublic(it.modifiers)
                }
            }
            return inc
        }.toList()
    }

    List<ColumnDB> getColumns() {
        return fields.collect {
            Column column = it.getAnnotation(Column)
            new ColumnDB(
                name : getColumnName(it),
                type : it.type,
                defaultVal: getDefaultValue(it),
                annotation: column
            )
        }
    }

    /**
     * Get fields names with values as Map from a Type Object
     * @param model
     * @return
     */
    Map<String, Object> getMap(Model model) {
        Map<String, Object> map = fields.collectEntries {
            [(getColumnName(it)) : model[it.name]]
        }
        return convertToDB(map)
    }
    /**
     * Converts fields of a class into db
     * @param map
     * @param preserve : Preserve some types to be exported into json/yaml
     * @return
     */
    static Map<String, Object> convertToDB(Map<String, Object> map, boolean preserve = false) {
        Map<String, Object> res = [:]
        map.each {
            key, val ->
                if(val instanceof Model &&! key.endsWith("_id")) {
                    res[key + "_id"] = toDBValue(val, preserve)
                } else {
                    res[key] = toDBValue(val, preserve)
                }
        }
        return res
    }
    /**
     * Converts any Object to DB value
     * @param val
     * @param preserve : preserve some types to be exported into json/yaml
     * @return
     */
    static Object toDBValue(Object val, boolean preserve = false) {
        //noinspection GroovyFallthrough
        switch (val) {
            case LocalTime:
                return  (val as LocalTime).HHmmss
            case LocalDate:
                return (val as LocalDate).YMD
            case LocalDateTime:
                return (val as LocalDateTime).YMDHms
            case Collection:
                List list = (val as List)
                if(!list.empty && preserve) {
                    if(list.first() instanceof Model) {
                        list = list.collect {(it as Model).id }
                    }
                }
                return preserve ? list : YAML.encode(list.empty ? [] : list.collect {
                   toDBValue(it)
                }).trim()
            case Map:
                return preserve ? val : YAML.encode(val).trim()
            case URL:
                return (val as URL).toExternalForm()
            case URI:
                return val.toString()
            case Enum:
                return preserve ? (val as Enum).ordinal() : val.toString()
            case boolean: // bool = ENUM
            case Boolean:
                return preserve ? val : val.toString()
            case InetAddress:
                return (val as InetAddress).hostAddress
            case Model:
                return (val as Model).id
            case byte[]:
            case int:
            case short:
            case Integer:
            case BigInteger:
            case long:
            case Long:
            case float:
            case Float:
            case double:
            case Double:
            case BigDecimal:
            case String:
            case char:
            case null:
                return val
            default:
                return val.toString()
        }
    }
    /**
     * Convert Map (from database) to Model Object
     * @param map
     * @return
     */
    M setMap(Map map) {
        M model = parametrizedInstance
        map.each {
            String origName = it.key.toString().toCamelCase()
            Field field = getFields().find { it.name == origName }
            // Look for Type ID
            if(!field && it.key.toString().endsWith("_id")) {
                origName = (it.key.toString().replaceAll(/_id$/,'')).toCamelCase()
                field = getFields().find { it.name == origName }
            }
            if(field) {
                model[origName] = fromDB(field, it.value)
            } else {
                Log.w("Field not found: %s", origName)
            }
        }
        return model
    }

    /**
     * Return SQL rules related to NULL and DEFAULT
     * @param field
     * @param nullable
     * @return
     */
    Object getDefaultValue(Field field) {
        M model = parametrizedInstance
        Object val = field.get(model)
        Object defaultVal = null
        Column column = field.getAnnotation(Column)
        boolean primary = column?.primary()
        if(!primary && val != null) { // When default value is null, it will be set as nullable
            defaultVal = toDBValue(val)
        }
        return defaultVal
    }

    /**
     * Returns the column name in the database
     * @param field
     * @return
     */
    static String getColumnName(final Field field) {
        String fname = field.name.toSnakeCase()
        switch (field.type) {
            case Model:
                fname += "_id"
                break
        }
        return fname
    }
    /**
     * Returns field name from DB column name
     * @param columnName
     * @return
     */
    static String getFieldName(final String columnName) {
        return columnName.toCamelCase()
    }

    /**
     * Get the table name. Override to change it
     * @return
     */
    String getTableName() {
        return this.name
    }
    /**
     * Returns Primary Key
     * @return
     */
    String getPk() {
        String pk = ""
        Field field = getFields().find {
            it.getAnnotation(Column)?.primary()
        }
        if(field) {
            pk = getColumnName(field)
        } else {
            // By default, search for "id"
            if(getFields().find { it.name == "id"} ) {
                pk = "id"
            }
        }
        return pk
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
     * Get one item using ID
     * @param id
     * @return
     */
    M get(int id) {
        Map map = tableConnector.key(pk).get(id)?.toMap() ?: [:]
        M model = setMap(map)
        closeConnections()
        return model
    }
    /**
     * Get a list of items using ids
     * @param ids
     * @return
     */
    List<M> get(List<Integer> ids) {
        List<Map> list = tableConnector.key(pk).get(ids).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        closeConnections()
        return all
    }
    /**
     * Get all with options:
     * limit : Total of items to get
     * offset : Starting from...
     * sort : Sort by
     * order : ASC or DESC
     *
     * @param options
     * @return
     */
    List<M> getAll(Map options = [:]) {
        DB con = tableConnector
        if(options.limit) {
            con = con.limit(options.limit as int, (options.offset ?: 0) as int)
        }
        if(options.sort) {
            con = con.order(options.sort.toString(), (options.order ?: "ASC") as Query.SortOrder)
        }
        List<Map> list = con.get().toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        closeConnections()
        return all
    }

    /**
     * Find all of a kind of model
     * @param fieldName
     * @param type
     * @return
     */
    List<M> findAll(String fieldName, Model model) {
        Field f = getFields().find {
            it.name == fieldName
        }
        List<M> list = []
        if(f) {
            String id = getColumnName(f)
            String main = autoIncrement ?: pk
            list = tableConnector.get([(main): model[main]]).toListMap().collect { setMap(it) }
        } else {
            Log.w("Unable to find field: %s", fieldName)
        }
        closeConnections()
        return list
    }
    /**
     * Find a single item which matches some column an some value
     * @param column
     * @param value
     * @return
     */
    M find(String column, Object value) {
        return find([(column): value])
    }
    /**
     * Find a single item using multiple columns
     * @param criteria
     * @return
     */
    M find(Map criteria) {
        criteria = convertToDB(criteria)
        Map map = tableConnector.get(criteria)?.toMap() ?: [:]
        M model = setMap(map)
        closeConnections()
        return model
    }
    /**
     * Find all items which matches a column and a value
     * @param column
     * @param value
     * @return
     */
    List<M> findAll(String column, Object value) {
        return findAll([(column): value])
    }
    /**
     * Find all items matching multiple columns
     * @param criteria
     * @return
     */
    List<M> findAll(Map criteria) {
        criteria = convertToDB(criteria)
        List<Map> list = tableConnector.get(criteria).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        closeConnections()
        return all
    }
    /**
     * Update a model
     * @param model
     * @param exclude : columns to exclude during update
     * @return
     */
    boolean update(M model, List<String> exclude = []) {
        boolean ok = false
        String primary = getPk()
        Object id = primary ? model[primary] : null
        try {
            Map map = getMap(model)
            exclude << primary // Exclude pk from map
            exclude.each {
                map.remove(it)
            }
            if(primary) {
                ok = tableConnector.key(primary).update(map, id)
            } else {
                Log.w("Trying to update a row without key. Please specify 'key()' or a primary key")
            }
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            closeConnections()
        }
        return ok
    }
    /**
     * Replace a model
     * @param model
     * @return
     */
    boolean replace(M model, List<String> exclude = []) {
        boolean ok = false
        try {
            Map<String, Object> map = getMap(model)
            exclude.each {
                map.remove(it)
            }
            ok = tableConnector.replace(map)
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            closeConnections()
        }
        return ok
    }
    /**
     * Delete using model
     * @param model
     * @return
     */
    boolean delete(M model) {
        return model.id ? delete(model.id) : delete(getMap(model))
    }
    /**
     * Delete with ID
     * @param id
     * @return
     */
    boolean delete(int id) {
        boolean ok = tableConnector.key(pk).delete(id)
        closeConnections()
        return ok
    }
    /**
     * Delete using multiple columns
     * @param map
     * @return
     */
    boolean delete(Map map) {
        map = convertToDB(map)
        boolean ok = tableConnector.key(pk).delete(map)
        closeConnections()
        return ok
    }
    /**
     * Delete using multiple IDs
     * @param ids
     * @return
     */
    boolean delete(List<Integer> ids) {
        boolean ok = tableConnector.key(pk).delete(ids)
        closeConnections()
        return ok
    }
    /**
     * Insert a model
     * @param model
     * @return
     */
    int insert(M model) {
        String ai = getAutoIncrement()
        int lastId = 0
        DB db
        try {
            Map<String, Object> map = getMap(model)
            if(map.containsKey(ai) && map[ai] == 0) {
                map.remove(ai)
            }
            db = tableConnector
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
            closeConnections()
        }
        return lastId
    }
    /**
     * Return table updater
     * Override when needed
     * @return
     */
    @SuppressWarnings('GrMethodMayBeStatic')
    List<Map> onUpdate(List<Map> data) {
        return data
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
    /**
     * Returns a new instance of this class's generic type
     * @return
     */
    M getNew() {
        return getParametrizedInstance()
    }
    /**
     * Returns a new query with the table already set
     * @return
     */
    Query getQuery() {
        return new Query(tableConnector.jdbc).setTable(tableName)
    }
    /**
     * Get a connection. If its already opened, reuse
     * @return
     */
    synchronized DB getTableConnector() {
        DB db = database.connect().table(tableName)
        db.cache = cache
        db.clearCache = clearCache
        activeConnections.add(db)
        return db
    }
    /**
     * Close active connections
     */
    synchronized void closeConnections() {
        activeConnections.each {
            it.close()
        }
        activeConnections.clear()
    }
    /**
     * Close all connections (in all threads) to the database
     */
    void quit() {
        database.quit()
    }
    /**
     * Drops the table
     */
    void drop() {
        tableConnector.set(new Query(tableConnector.jdbc, Query.Action.DROP))
    }

    /**
     * Import value from database
     * @param field
     * @param value
     * @return
     */
    @SuppressWarnings('GroovyUnusedAssignment')
    static Object fromDB(Field field, Object value, boolean convertModels = true) {
        Object retVal = null
        if(value != null) {
            try {
                //noinspection GroovyFallthrough
                switch (field.type) {
                    case short:
                        retVal = value as short
                        break
                    case int:
                    case Integer:
                        retVal = value as int
                        break
                    case BigInteger:
                        retVal = value as BigInteger
                        break
                    case long:
                    case Long:
                        retVal = value as long
                        break
                    case float:
                    case Float:
                        retVal = value as float
                        break
                    case double:
                    case Double:
                        retVal = value as double
                        break
                    case BigDecimal:
                        retVal = value as BigDecimal
                        break
                    case String:
                        retVal = value.toString()
                        break
                    case char:
                        retVal = value as char
                        break
                    case boolean:
                    case Boolean:
                        retVal = value.toString() == "true"
                        break
                    case Collection:
                        try {
                            retVal = YAML.decode((value ?: "").toString()) as List
                            if (retVal) {
                                if (!(retVal as List).empty && convertModels) {
                                    if (retVal.first() instanceof Integer && genericIsModel(field)) {
                                        retVal = retVal.collect { relation[getParameterizedClass(field).class.name].get(it as int) }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.w("Unable to parse list value in field %s: %s", field.name, e.message)
                            retVal = []
                        }
                        break
                    case Map:
                        try {
                            retVal = YAML.decode((value ?: "").toString()) as Map
                        } catch (Exception e) {
                            Log.w("Unable to parse map value in field %s: %s", field.name, e.message)
                            retVal = [:]
                        }
                        break
                    case LocalDate:
                        try {
                            retVal = value.toString().toDate()
                        } catch(Exception ignore) {
                            retVal = (value.toString().toDateTime()).toLocalDate()
                        }
                        break
                    case LocalTime:
                        try {
                            retVal = value.toString().toTime()
                        } catch(Exception ignore) {
                            retVal = (value.toString().toDateTime()).toLocalTime()
                        }
                        break
                    case URI:
                        retVal = new URI(value.toString())
                        break
                    case URL:
                        retVal = new URL(value.toString())
                        break
                    case Inet4Address:
                        retVal = value.toString().toInet4Address()
                        break
                    case Inet6Address:
                        retVal = value.toString().toInet6Address()
                        break
                    case InetAddress:
                        retVal = value.toString().toInetAddress()
                        break
                    case Enum:
                        if (value.toString().isNumber()) {
                            retVal = (field.type as Class<Enum>).enumConstants[value as int]
                        } else {
                            retVal = Enum.valueOf((Class<Enum>) field.type, value.toString().toUpperCase())
                        }
                        break
                    case Model:
                        Constructor<?> c = field.type.getConstructor()
                        Model refType = (c.newInstance() as Model)
                        retVal = relation[refType.class.name].get(value as int)
                        break
                    default:
                        try {
                            // Having a constructor with String
                            retVal = field.type.getConstructor(String.class).newInstance(value.toString())
                        } catch (Exception ignore) {
                            try {
                                // Having a static method 'fromString'
                                retVal = field.type.getDeclaredMethod("fromString", String.class).invoke(null, value.toString())
                            } catch (Exception ignored) {
                                try {
                                    retVal = value
                                } catch (Exception e) {
                                    Log.w("Unable to set Model field: %s with value: %s (%s)", field.name, value, e.message)
                                }
                            }
                        }
                }
            } catch(Exception ex) {
                Log.w("Unable to set value: %s in field: %s (%s)", value, field.name, ex.message)
            }
        }
        return retVal
    }
    /**
     * Get the parameterized class of a List (using field)
     * @param field
     * @return
     */
    static Object getParameterizedClass(Field field) {
        ParameterizedType type = (ParameterizedType) field.getGenericType()
        Class listClass = (Class) type.getActualTypeArguments()[0]
        Object obj = listClass.getDeclaredConstructor().newInstance()
        return obj
    }
    /**
     * True if generic type is Model, example : List<Model>
     * @param field
     * @return
     */
    static boolean genericIsModel(Field field) {
        return getParameterizedClass(field) instanceof Model
    }
}
