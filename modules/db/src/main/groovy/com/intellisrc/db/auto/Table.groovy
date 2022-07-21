package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.*
import com.intellisrc.db.annot.*
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.etc.Instanciable
import com.intellisrc.etc.YAML
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.*
import java.time.*

@CompileStatic
class Table<M extends Model> implements Instanciable<M> {
    // Keep relation of tables:
    static protected Map<String, Boolean> versionChecked = [:] // it will be set to true after the version has been checked
    static protected Map<String, Table> relation = [:]
    static void reset() {
        versionChecked = [:]
        relation = [:]
    }

    // ----------- Flags and other instance properties -------------
    boolean autoUpdate = true // set to false if you don't want the table to update automatically
    protected final Database database
    protected final JDBC jdbc
    protected final String name
    protected DB connection
    protected int cache = 0
    protected boolean clearCache = false
    String charset = "utf8"

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
        boolean multipleKey = false // Filled automatically
    }
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
        this.database = database ?: Database.getDefault()
        TableMeta meta = this.class.getAnnotation(TableMeta)
        this.name = name ?: meta?.name() ?: this.class.simpleName.toSnakeCase()
        if(meta) {
            this.cache = meta.cache()
            this.clearCache = meta.clearCache()
            this.autoUpdate = meta.autoUpdate()
        }
        assert this.name : "Table name not set"
        jdbc = connect().jdbc
        updateOrCreate()
        relation[parametrizedInstance.class.name] = this
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
                            if (definedVersion > version) {
                                updateTable()
                            } else {
                                Log.d("Table [%s] doesn't need to be updated: [Code: %d] vs [DB: %d]",
                                    tableName, version, definedVersion)
                            }
                        }
                    } else {
                        if(!createTable(conn)) {
                            Log.w("Table [%s] was not created.", tableName)
                        }
                    }
                    break
                default:
                    Log.w("Create or Update : Database type can not be updated automatically. Please check the documentation to know which databases are supported.")
                    return
                    break
            }
        }
        close()
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
            if (this.class.isAnnotationPresent(TableMeta)) {
                TableMeta meta = this.class.getAnnotation(TableMeta)
                if (meta.engine() != "auto") {
                    engine = meta.engine()
                }
                charset = meta.charset()
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
    /**
     * Get all fields as ColumnDB list
     * @return
     */
    List<ColumnDB> getColumns() {
        return fields.collect { getColumnDB(it) }
    }
    /**
     * Convert Field to ColumnDB
     * @param field
     * @return
     */
    ColumnDB getColumnDB(final Field field) {
        Column column = field.getAnnotation(Column)
        return new ColumnDB(
            name : getColumnName(field),
            type : field.type,
            defaultVal: getDefaultValue(field),
            annotation: column
        )
    }

    /**
     * Get fields names with values as Map from a Type Object
     * @param model
     * @return
     */
    Map<String, Object> getMap(Model model) {
        Map<String, Object> map = fields.collectEntries {
            Object val = model[it.name]
            [(getColumnName(it)) : val]
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
        M model = null
        if(! map.isEmpty()) {
            model = parametrizedInstance
            map.each {
                String origName = it.key.toString().toCamelCase()
                Field field = getFields().find { it.name == origName }
                // Look for Type ID
                if (!field && it.key.toString().endsWith("_id")) {
                    origName = (it.key.toString().replaceAll(/_id$/, '')).toCamelCase()
                    field = getFields().find { it.name == origName }
                }
                if (field) {
                    model[origName] = fromDB(field, it.value)
                } else {
                    Log.w("Field not found: %s", origName)
                }
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
        Map map = connect().key(pk).get(id)?.toMap() ?: [:]
        M model = setMap(map)
        close()
        return model
    }
    /**
     * Get a list of items using ids
     * @param ids
     * @return
     */
    List<M> get(List<Integer> ids) {
        List<Map> list = connect().key(pk).get(ids).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        close()
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
        DB con = connect()
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
        close()
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
            list = connect().get([(main): model[main]]).toListMap().collect { setMap(it) }
        } else {
            Log.w("Unable to find field: %s", fieldName)
        }
        close()
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
        Map map = connect().get(criteria)?.toMap() ?: [:]
        M model = setMap(map)
        close()
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
        List<Map> list = connect().get(criteria).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        close()
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
                ok = connect().key(primary).update(map, id)
            } else {
                Log.w("Trying to update a row without key. Please specify 'key()' or a primary key")
            }
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            close()
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
            ok = connect().replace(map)
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            close()
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
        boolean ok = connect().key(pk).delete(id)
        close()
        return ok
    }
    /**
     * Delete using multiple columns
     * @param map
     * @return
     */
    boolean delete(Map map) {
        map = convertToDB(map)
        boolean ok = connect().key(pk).delete(map)
        close()
        return ok
    }
    /**
     * Delete using multiple IDs
     * @param ids
     * @return
     */
    boolean delete(List<Integer> ids) {
        boolean ok = connect().key(pk).delete(ids)
        close()
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
            db = connect()
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
            close()
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
        return new Query(jdbc).setTable(tableName)
    }
    /**
     * Get a connection. If its already opened, reuse
     * @return
     */
    protected synchronized DB connect() {
        DB db
        if(connection?.opened) {
            db = connection
        } else {
            db = database.connect().table(tableName)
            db.cache = cache
            db.clearCache = clearCache
            connection = db
        }
        return db
    }
    /**
     * Exposed tableConnector
     * @return
     */
    synchronized DB getTable() {
        return connect()
    }
    /**
     * Close current connection
     * @param db
     */
    synchronized void close() {
        if(connection?.opened) {
            connection.close()
        }
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
        Query qry = new Query(jdbc, Query.Action.DROP)
        qry.table = tableName
        connect().set(qry)
    }

    /**
     * Import value from database
     * @param field
     * @param value
     * @return
     */
    @SuppressWarnings('GroovyUnusedAssignment')
    Object fromDB(Field field, Object value, boolean convertModels = true) {
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
                                        Column annotation = field.getAnnotation(Column)
                                        Table table = relation[getParameterizedClass(field).class.name]
                                        retVal = table.get(retVal)
                                        switch (annotation.ondelete()) {
                                            case DeleteActions.NULL:
                                                // Do nothing (must be null already)
                                                break
                                            default:
                                                boolean warn = annotation.ondelete() == DeleteActions.RESTRICT
                                                if(warn && retVal.find { it == null }) {
                                                    Log.w("Table: [%s], field: %s, contains NULL values",
                                                        this.name, field.name)
                                                }
                                                retVal = retVal.findAll {
                                                    return it != null
                                                }
                                                break
                                        }
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
