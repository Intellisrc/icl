package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.db.annot.ViewMeta
import com.intellisrc.db.jdbc.JDBC
import com.intellisrc.etc.Instanciable
import com.intellisrc.etc.YAML
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.annotation.Annotation
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

/**
 * @since 2023/05/30.
 */
@CompileStatic
abstract class Relational<M extends Model> implements Instanciable<M> {
    // Keeps the relation between Table/View and Model
    protected final static ConcurrentHashMap<Relational, Class> tableModelRel = new ConcurrentHashMap<>()
    // ----------- Flags and other instance properties -------------
    protected final Database database
    @SuppressWarnings('GrFinalVariableAccess')
    protected final JDBC jdbc
    protected final String name
    protected int cache = 0
    protected boolean clearCache = false
    protected List<String> primaryKey = []
    protected int chunkSize = 100

    /**
     * When using `getAllByChunks` it will use this interface to return as it goes
     */
    interface ChunkReader<M> {
        void call(List<M> rows)
    }

    /**
     * Information about a Field that will be used as column in a DB
     * @see com.intellisrc.db.annot.Column
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
    Relational(Database database) {
        this("", database)
    }
    /**
     * Constructor. A Database object can be passed
     * when using multiple databases.
     *
     * @param name : Alternative way to set table name (besides @TableMeta)
     * @param database
     */
    Relational(String name = "", Database database = null) {
        this.database = database ?: Database.getDefault()
        Annotation meta = this.class.getAnnotation(ViewMeta) ?: this.class.getAnnotation(TableMeta)
        this.name = name ?: (meta && meta.hasProperty("name") ? meta.properties.name : this.class.simpleName.toSnakeCase()).toString()
        this.cache = (meta && meta.hasProperty("cache") ? meta.properties.cache : 0) as int
        this.clearCache = (meta && meta.hasProperty("clearCache") ? meta.properties.clearCache : false) as boolean
        assert this.name : "Table or View name not set"
        Class model = getParametrizedInstance().class
        if(tableModelRel.containsValue(model)) {
            Relational rel = getTableOrView(model)
            if(rel.class != this.class) {
                if (rel instanceof Table && this instanceof Table) {
                    Log.e("Multiple Table classes can not use the same Model class as parametrized type. " +
                        "This may result in unexpected behaviour. " +
                        "[%s] Table class already defined [%s] Model. [%s] Table class is also defining it.",
                        rel.class.simpleName,
                        model.simpleName,
                        this.class.simpleName)
                }
            }
        }
        tableModelRel[this] = model
        jdbc = connect().jdbc
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
     * Convert Map (from database) to Model Object
     * @param map
     * @return
     */
    M setMap(Map map) {
        M model = null
        if(! map.isEmpty()) {
            model = parametrizedInstance
            //noinspection GroovyMissingReturnStatement
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
            case Collection:
                List list = (val as List)
                if(!list.empty && preserve) {
                    if(list.first() instanceof Model) {
                        list = list.collect {(it as Model).uniqueId }
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
                return (val as Model).uniqueId
            case byte[]:
            case int:
            case short:
            case Short:
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
            case Character:
            case char:
            case LocalTime:
            case LocalDate:
            case LocalDateTime:
            case null:
                return val
            default:
                return val.toString()
        }
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
     * Get the view/table name. Override to change it
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
        if(primaryKey.empty) {
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
            primaryKey = pk
        } else {
            pk = primaryKey
        }
        return pk
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
        db.close()
        return model
    }
    /**
     * Get a list of items using ids
     * @param ids
     * @return
     */
    List<M> get(Collection<Integer> ids) {
        DB db = connect()
        if(pk) { db.keys(pks) }
        List<Map> list = db.get(ids).toListMap()
        List<M> all = list.collect {
            Map map ->
                return setMap(map)
        }
        db.close()
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
    void getAll(String sortBy, Query.SortOrder order, ChunkReader<M> chunkReader) {
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
        if(! options.isEmpty() &&! ["limit", "sort"].any { options.containsKey(it) }) {
            Log.e("Incorrect options: (%s) passed to `getAll`, did you mean `findAll` ?", options.toMapString())
            return []
        }
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
        con.close()
        return all
    }
    /**
     * Common method to get chunks with options
     * @param options
     * @param chunkReader
     */
    void getAll(Map options, ChunkReader<M> chunkReader) {
        int offset = 0
        int size
        do {
            List<M> buffer = getAll(options + [
                limit : chunkSize,
                offset: offset
            ])
            chunkReader.call(buffer)
            //noinspection GroovyUnusedAssignment
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
        DB db = connect()
        Map map = db.get(criteria)?.toMap() ?: [:]
        M model = setMap(map)
        db.close()
        return model
    }
    /**
     * Find all of a kind of model id
     * @param fieldName
     * @param type
     * @param options (limit, sort, etc)
     * @return
     */
    List<M> findAll(String fieldName, Model model, Map options = [:]) {
        return findAll([(fieldName) : model.uniqueId], options)
    }
    /**
     * Find all using Model and return by chunks
     * @param fieldName
     * @param model
     * @param chunkReader
     */
    void findAll(String fieldName, Model model, ChunkReader<M> chunkReader) {
        int offset = 0
        int size
        do {
            List<M> buffer = findAll(fieldName, model, [
                limit : chunkSize,
                offset: offset
            ])
            chunkReader.call(buffer)
            //noinspection GroovyUnusedAssignment
            offset += chunkSize
            size = buffer.size()
        } while(size == chunkSize)
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
    void findAll(String column, Object value, ChunkReader<M> chunkReader) {
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
        db.close()
        return all
    }
    /**
     * Find all items matching multiple columns returning by chunks
     * @param criteria
     * @param chunkReader
     */
    void findAll(Map criteria, ChunkReader<M> chunkReader) {
        int offset = 0
        int size
        do {
            List<M> buffer = findAll(criteria, [
                limit : chunkSize,
                offset: offset
            ])
            chunkReader.call(buffer)
            //noinspection GroovyUnusedAssignment
            offset += chunkSize
            size = buffer.size()
        } while(size == chunkSize)
    }
    /**
     * Return records count
     * @return
     */
    int count() {
        DB db = connect()
        int c = db.count().get().toInt()
        db.close()
        return c
    }
    /**
     * if table is empty
     * @return
     */
    boolean isEmpty(){
        return count() == 0
    }
    /**
     * Return records count using a criteria:
     * count(color: 'blue')
     * @return
     */
    int count(Map criteria) {
        DB db = connect()
        int c = db.count().get(convertToDB(criteria)).toInt()
        db.close()
        return c
    }
    /**
     * Return records count using part of SQL query
     * WARNING: This method is not cross-database friendly avoid if possible.
     * count('start_date BETWEEN(? AND ?)', date1, date2)
     * @return
     */
    int count(String where, Object... params) {
        DB db = connect()
        int c = db.count().where(where, params).get().toInt()
        db.close()
        return c
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
     * Get a new connection and set default settings.
     * @return
     */
    protected synchronized DB connect() {
        DB db = database.connect().table(tableName)
        db.cache = cache
        db.clearCache = clearCache
        return db
    }
    /**
     * Exposed tableConnector
     * (NOTE: To use it inside this class, use: `DB db = connect()`
     * instead to prevent opening more connections)
     * @return
     */
    synchronized DB getTable() {
        return connect()
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
    boolean drop(boolean view = false) {
        DB db = connect()
        boolean dropped = view ? db.dropView() : db.drop()
        db.close()
        return dropped
    }
    /**
     * Reset
     */
    void reset() {}

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
                                        Relational rel = getTableOrView(getParameterizedClass(field) as Model)
                                        retVal = rel.get(retVal)
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
                            Log.w("Unable to parse list value in field %s: %s", field.name, e)
                            retVal = []
                        }
                        break
                    case Map:
                        try {
                            retVal = YAML.decode((value ?: "").toString()) as Map
                        } catch (Exception e) {
                            Log.w("Unable to parse map value in field %s: %s", field.name, e)
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
                        Relational rel = getTableOrView(field)
                        retVal = rel.get(value as int)
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
                                    Log.w("Unable to set Model field: %s with value: %s (%s)", field.name, value, e)
                                }
                            }
                        }
                }
            } catch(Exception ex) {
                Log.w("Unable to set value: %s in field: %s (%s)", value, field.name, ex)
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

    /**
     * Return a Model instance from a Field type
     * @param field
     * @return
     */
    static Model getModel(Field field) {
        Model model = null
        try {
            Constructor<?> c = (field.type.getConstructor())
            model = (c.newInstance() as Model)
        } catch(Exception e) {
            Log.e("Trying to get a Model from another object type: %s", field?.type?.simpleName, e)
        }
        return model
    }

    /**
     * Based on a model class, get the table or view
     * @param model
     * @return
     */
    static Relational getTableOrView(Model model) {
        return getTableOrView(model.class)
    }
    /**
     * Based on a field type (of Model), get table or view
     * @param field
     * @return
     */
    static Relational getTableOrView(Field field) {
                        return getTableOrView(getModel(field))
        }

    /**
     * Based on a Class, get the table or view
     * If a model has multiple definitions, it will choose the Table class
     * otherwise, the first one.
     * @param model
     * @return
     */
    static Relational getTableOrView(Class model) {
        List<Relational> list = tableModelRel.findAll { it.value == model }.collect { it.key }
        return list.find { it instanceof Table } ?: list.first()
    }
}
