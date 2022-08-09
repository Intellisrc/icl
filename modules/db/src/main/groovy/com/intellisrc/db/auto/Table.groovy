package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.db.jdbc.JDBC
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
    @SuppressWarnings('GrFinalVariableAccess')
    protected final JDBC jdbc
    protected final String name
    protected DB connection
    protected int cache = 0
    protected boolean clearCache = false
    protected List<String> primaryKeys = []
    protected int chunkSize = 100

    /**
     * When using `getAllByChunks` it will use this interface to return as it goes
     */
    interface ChunkReader<M> {
        void call(List<M> rows)
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
     * Return first column marked as Primary Key
     * @return
     */
    String getPk() {
        return pks.empty ? "" : pks.first()
    }
    /**
     * Returns Primary Key column(s)
     * @return
     */
    List<String> getPks() {
        List<String> pk = []
        if(primaryKeys.empty) {
            List<Field> fields = getFields().toList().findAll {
                it.getAnnotation(Column)?.primary()
            }
            if (!fields.empty) {
                pk = fields.collect { getColumnName(it) }
            } else {
                // By default, search for "id"
                if (getFields().find { it.name == "id" }) {
                    pk = ["id"]
                }
            }
            primaryKeys = pk
        } else {
            pk = primaryKeys
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
        DB db = connect()
        if(pk) { db.keys(pks) }
        Map map = db.get(id)?.toMap() ?: [:]
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
        DB db = connect()
        if(pk) { db.keys(pks) }
        List<Map> list = db.get(ids).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        close()
        return all
    }
    /**
     * Get all limiting number of rows to return
     * @param options
     * @return
     */
    List<M> getAll(int limit, int offset = 0) {
        return getAll(
            limit: limit,
            offset: offset
        )
    }
    /**
     * Get all sorting it database-side
     * @param sortBy
     * @param order
     * @return
     */
    List<M> getAll(String sortBy, Query.SortOrder order) {
        return getAll(
            sort: sortBy,
            order: order.toString()
        )
    }
    /**
     * Get all sorting it database-side and getting results by chunks
     * @param sortBy
     * @param order
     * @param chunkReader
     */
    void getAll(String sortBy, Query.SortOrder order, ChunkReader chunkReader) {
        getAll([
            sort: sortBy,
            order: order
        ], chunkReader)
    }
    /**
     * Get all limiting and sorting it database-side
     * @param sortBy
     * @param order
     * @return
     */
    List<M> getAll(String sortBy, Query.SortOrder order, int limit, int offset = 0) {
        return getAll(
            limit: limit,
            offset: offset,
            sort: sortBy,
            order: order.toString()
        )
    }
    /**
     * Sometimes if there are too many records `getAll()` may timeout.
     * For those cases, this method will work faster than `getAll` but
     * it will execute more queries.
     * @param chunkReader
     */
    void getAll(ChunkReader<M> chunkReader) {
        getAll([:], chunkReader)
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
     * Common method to get chunks with options
     * @param options
     * @param chunkReader
     */
    void getAll(Map options, ChunkReader chunkReader) {
        int offset = 0
        int size
        do {
            List<M> buffer = getAll(options + [
                limit : chunkSize,
                offset: offset
            ])
            chunkReader.call(buffer)
            offset += chunkSize
            size = buffer.size()
        } while(size == chunkSize)
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
     * Find all of a kind of model
     * @param fieldName
     * @param type
     * @param options (limit, sort, etc)
     * @return
     */
    List<M> findAll(String fieldName, Model model, Map options = [:]) {
        Field f = getFields().find {
            it.name == fieldName
        }
        List<M> list = []
        DB db = connect()
        if(f) {
            Map map = getMap(model)
            Object id = (pks.empty ? null : (pks.size() == 1) ? map[pk] : pks.collect {map[it] })
            if(pks) { db.keys(pks) }
            if(! options.isEmpty()) {
                if(options.limit) {
                    db.limit(options.limit as int, (options.offset ?: "0") as int)
                }
                if(options.sort) {
                    db.order(options.sort.toString(), (options.order ?: "ASC") as Query.SortOrder)
                }
            }
            list = db.get(id).toListMap().collect { setMap(it) }
        } else {
            Log.w("Unable to find field: %s", fieldName)
        }
        close()
        return list
    }
    void findAll(String fieldName, Model model, ChunkReader chunkReader) {
        int max = table.count().get().toInt()
        (0..(max - 1)).step(chunkSize).each {
            chunkReader.call(findAll(fieldName, model, [
                limit : chunkSize,
                offset: it
            ]))
        }
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
     * Find all items which matches a column and a value and return by chunks
     * @param column
     * @param value
     * @param chunkReader
     */
    void findAll(String column, Object value, ChunkReader chunkReader) {
        findAll([(column): value], chunkReader)
    }
    /**
     * Find all items matching multiple columns
     * @param criteria
     * @return
     */
    List<M> findAll(Map criteria, Map options = [:]) {
        criteria = convertToDB(criteria)
        DB db = connect()
        if(! options.isEmpty()) {
            if(options.limit) {
                db.limit(options.limit as int, (options.offset ?: "0") as int)
            }
            if(options.sort) {
                db.order(options.sort.toString(), (options.order ?: "ASC") as Query.SortOrder)
            }
        }
        List<Map> list = db.get(criteria).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        close()
        return all
    }
    /**
     * Find all items matching multiple columns returning by chunks
     * @param criteria
     * @param chunkReader
     */
    void findAll(Map criteria, ChunkReader chunkReader) {
        int offset = 0
        int size
        do {
            List<M> buffer = findAll(criteria + [
                limit : chunkSize,
                offset: offset
            ])
            chunkReader.call(buffer)
            offset += chunkSize
            size = buffer.size()
        } while(size == chunkSize)
    }
    /**
     * Update a model
     * @param model
     * @param exclude : columns to exclude during update
     * @return
     */
    boolean update(M model, List<String> exclude = []) {
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
            close()
        }
        return ok
    }
    /**
     * Update multiple models
     * @param models
     * @return
     */
    boolean update(List<M> models) {
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
        close()
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
     * Replace multiple models
     * @param models
     * @return
     */
    boolean replace(List<M> models) {
        DB db = connect()
        boolean ok = db.replace(models.collect { it.toMap() })
        close()
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
        DB db = connect()
        if(pk) { db.keys(pks) }
        boolean ok = db.delete(id)
        close()
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
        close()
        return ok
    }
    /**
     * Delete using multiple Models
     * @param ids
     * @return
     */
    boolean delete(List<M> models) {
        DB db = connect()
        if(pk) { db.keys(pks) }
        boolean ok = db.delete(models.collect { getMap(it) })
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
     * Insert multiple models
     * @param models
     * @return
     */
    boolean insert(List<M> models) {
        DB db = connect()
        boolean ok = db.insert(models.collect { it.toMap() })
        close()
        return ok
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
