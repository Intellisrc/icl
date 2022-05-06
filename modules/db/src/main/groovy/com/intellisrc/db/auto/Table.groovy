package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.ModelMeta
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.db.jdbc.MariaDB
import com.intellisrc.db.jdbc.MySQL
import com.intellisrc.etc.Instanciable
import com.intellisrc.etc.YAML
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.regex.Matcher

@CompileStatic
class Table<M extends Model> implements Instanciable<M> {
    static boolean alwaysCheck = false  //Used by updater
    static protected Map<String, Boolean> versionChecked = [:] // it will be set to true after the version has been checked
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
    }
    /**
     * Decide if table needs to be updated or created
     */
    void updateOrCreate() {
        if(alwaysCheck || !versionChecked.containsKey(tableName) || !versionChecked[tableName]) {
            versionChecked[tableName.toString()] = true
            boolean exists = tableConnector.exists()
            //noinspection GroovyFallthrough
            switch (tableConnector.jdbc) {
                case MySQL:
                case MariaDB:
                    // TODO: support others
                    break
                default:
                    Log.w("Create or Update : MySQL/MariaDB is only supported for now.")
                    return
                    break
            }
            if (exists) {
                if(autoUpdate) {
                    if (definedVersion > tableVersion) {
                        Log.i("Table [%s] is going to be updated from version: %d to %d",
                                tableName, tableVersion, definedVersion)
                        if(!TableUpdater.update([this])) {
                            Log.w("Table [%s] was not updated.", tableName)
                        }
                    } else {
                        Log.d("Table [%s] doesn't need to be updated: [Code: %d] vs [DB: %d]",
                                tableName, tableVersion, definedVersion)
                    }
                }
            } else {
                createTable()
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
            String createSQL = "CREATE TABLE IF NOT EXISTS `${tableName}` (\n"
            List<String> columns = []
            List<String> keys = []
            Map<String, List<String>> uniqueGroups = [:]
            getFields().each {
                Field field ->
                    field.setAccessible(true)
                    if (Modifier.isPrivate(field.modifiers)) {
                        Modifier.setPublic(field.modifiers)
                    }
                    String fieldName = getColumnName(field)
                    Column column = field.getAnnotation(Column)
                    List<String> parts = ["`${fieldName}`".toString()]
                    if (!column) {
                        String type = getColumnDefinition(field)
                        assert type: "Unknown type: ${field.type.simpleName} in ${field.name}"
                        parts << type
                        String defaultVal = getDefaultValue(field)
                        if (defaultVal) {
                            parts << defaultVal
                        }
                    } else {
                        if (column.columnDefinition()) {
                            parts << column.columnDefinition()
                        } else {
                            String type = column.type() ?: getColumnDefinition(field, column)
                            parts << type

                            String defaultVal = getDefaultValue(field, column.nullable())
                            if (defaultVal) {
                                parts << defaultVal
                            }

                            List<String> extra = []
                            if (column.unique() || column.uniqueGroup()) {
                                if (column.uniqueGroup()) {
                                    if (!uniqueGroups.containsKey(column.uniqueGroup())) {
                                        uniqueGroups[column.uniqueGroup()] = []
                                    }
                                    uniqueGroups[column.uniqueGroup()] << fieldName
                                } else {
                                    extra << "UNIQUE"
                                }
                            }
                            if (!extra.empty) {
                                parts.addAll(extra)
                            }
                        }
                        if (column.key()) {
                            keys << "KEY `${tableName}_${fieldName}_key_index` (`${fieldName}`)".toString()
                        }
                    }
                    columns << parts.join(' ')
            }
            if (!keys.empty) {
                columns.addAll(keys)
            }
            if (!uniqueGroups.keySet().empty) {
                uniqueGroups.each {
                    columns << "UNIQUE KEY `${tableName}_${it.key}` (`${it.value.join('`, `')}`)".toString()
                }
            }
            String fks = getFields().collect { getForeignKey(it) }.findAll { it }.join(",\n")
            if (fks) {
                columns << fks
            }
            if (engine) {
                engine = "ENGINE=${engine}"
            }
            createSQL += columns.join(",\n") + "\n) ${engine} CHARACTER SET=${charset}\nCOMMENT='v.${definedVersion}'"
            if (!tableConnector.set(new Query(createSQL))) {
                Log.v(createSQL)
                Log.e("Unable to create table.")
            }
        }
        closeConnections()
    }

    /**
     * Returns the table comment
     * @return
     */
    String getComment() {
        DB connection = database.connect()
        Query query = new Query("SELECT table_comment FROM INFORMATION_SCHEMA.TABLES WHERE table_schema=? AND table_name=?",
                [tableConnector.jdbc.dbname, tableName])
        String comment = connection.get(query).toString()
        connection.close()
        return comment
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
     * Get the defined version in the database
     * @return
     */
    int getTableVersion() {
        int version = 1
        String comm = getComment()
        Matcher matcher = (comm =~ /v.(\d+)/)
        if(matcher.find()) {
            version = matcher.group(1) as int
        }
        return version
    }

    List<Field> getFields() {
        int index = 0
        return getParametrizedInstance(index).class.declaredFields.findAll {!it.synthetic }.toList()
    }

    /**
     * Get fields names with values as Map from a Type Object
     * @param model
     * @return
     */
    Map<String, Object> getMap(M model) {
        Map<String, Object> map = fields.collectEntries {
            [(getColumnName(it)) : model[it.name]]
        }
        return convertToDB(map)
    }
    /**
     * Converts fields of a class into db
     * @param map
     * @return
     */
    static Map<String, Object> convertToDB(Map<String, Object> map) {
        Map<String, Object> res = [:]
        map.each {
            key, val ->
                if(val instanceof Model &&! key.endsWith("_id")) {
                    res[key + "_id"] = toDBValue(val)
                } else {
                    res[key] = toDBValue(val)
                }
        }
        return res
    }
    /**
     * Converts any Object to DB value
     * @param val
     * @return
     */
    static Object toDBValue(Object val) {
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
                return YAML.encode(list.empty ? [] : list.collect {
                   toDBValue(it)
                }).trim()
            case Map:
                return YAML.encode(val).trim()
            case URL:
            case URI:
            case Enum:
            case boolean: // bool = ENUM
            case Boolean:
                return val.toString()
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
                //noinspection GroovyFallthrough
                switch (field.type) {
                    case boolean:
                    case Boolean:
                        model[origName] = it.value.toString() == "true"
                        break
                    case Collection:
                        try {
                            model[origName] = YAML.decode((it.value ?: "").toString()) as List
                        } catch (Exception e) {
                            Log.w("Unable to parse list value in field %s: %s", origName, e.message)
                            model[origName] = []
                        }
                        break
                    case Map:
                        try {
                            model[origName] = YAML.decode((it.value ?: "").toString()) as Map
                        } catch (Exception e) {
                            Log.w("Unable to parse map value in field %s: %s", origName, e.message)
                            model[origName] = [:]
                        }
                        break
                    case LocalDate:
                        model[origName] = (it.value as LocalDateTime).toLocalDate()
                        break
                    case LocalTime:
                        model[origName] = (it.value as LocalDateTime).toLocalTime()
                        break
                    case URI:
                        model[origName] = new URI(it.value.toString())
                        break
                    case URL:
                        model[origName] = new URL(it.value.toString())
                        break
                    case Inet4Address:
                        model[origName] = it.value.toString().toInet4Address()
                        break
                    case Inet6Address:
                        model[origName] = it.value.toString().toInet6Address()
                        break
                    case InetAddress:
                        model[origName] = it.value.toString().toInetAddress()
                        break
                    case Enum:
                        if(it.value.toString().isNumber()) {
                            model[origName] = (field.type as Class<Enum>).enumConstants[it.value as int]
                        } else {
                            model[origName] = Enum.valueOf((Class<Enum>) field.type, it.value.toString())
                        }
                        break
                    case Model:
                        Constructor<?> c = field.type.getConstructor()
                        Model refType = (c.newInstance() as Model)
                        model[origName] = refType.table.get(it.value as int)
                        break
                    default:
                        try {
                            // Having a constructor with String
                            model[origName] = field.type.getConstructor(String.class).newInstance(it.value.toString())
                        } catch(Exception ignore) {
                            try {
                                // Having a static method 'fromString'
                                model[origName] = field.type.getDeclaredMethod("fromString", String.class).invoke(null, it.value.toString())
                            } catch (Exception ignored) {
                                try {
                                    model[origName] = it.value
                                } catch(Exception e) {
                                    Log.w("Unable to set Model[%s] field: %s with value: %s (%s)", model.class.simpleName, origName, it.value, e.message)
                                }
                            }
                        }
                }
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
    String getDefaultValue(Field field, boolean nullable = false) {
        M model = parametrizedInstance
        Object val = field.get(model)
        String defaultVal = ""
        Column column = field.getAnnotation(Column)
        boolean primary = column?.primary()
        if(!primary) {
            if (val != null) { // When default value is null, it will be set as nullable
                val = toDBValue(val)
                String dv = val.toString().isNumber() ? val.toString() : "'${val}'".toString()
                defaultVal = (nullable ? "" : "NOT NULL ") + "DEFAULT ${dv}"
            } else if(column &&! column.nullable()) {
                defaultVal = "NOT NULL"
            }
        }
        return defaultVal
    }

    /**
     * Return SQL column definition for a field
     * @param field
     * @param column
     * @return
     */
    static String getColumnDefinition(Field field, Column column = null) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (field.type) {
            case boolean:
            case Boolean:
                type = "ENUM('true','false')"
                break
            case Inet4Address:
                type = "VARCHAR(${column?.length() ?: 15})"
                break
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column?.length() ?: 45})"
                break
            case String:
                type = "VARCHAR(${column?.length() ?: 255})"
                break
            case byte:
                type = type ?: "TINYINT"
            case short:
                type = type ?: "SMALLINT"
            case int:
            case Integer:
            case Model: //Another Model
                type = type ?: "INT"
            case BigInteger:
            case long:
            case Long:
                type = type ?: "BIGINT"
                boolean hasAnnotation = column != null
                int len = hasAnnotation ? column.length() : 0
                String length = len ? "(${len})" : ""
                boolean unsignedDefault = Column.class.getMethod("unsigned").defaultValue
                boolean autoIncDefault = Column.class.getMethod("autoincrement").defaultValue
                boolean primaryDefault = Column.class.getMethod("primary").defaultValue
                List<String> extra = [type, length]
                        extra << ((hasAnnotation ? column.unsigned() : unsignedDefault) ? "UNSIGNED" : "")
                        extra << ((hasAnnotation ? column.primary() && column.autoincrement() : autoIncDefault) ? "AUTO_INCREMENT" : "")
                        extra << ((hasAnnotation ? column.primary() : primaryDefault) ? "PRIMARY KEY" : "")
                type = extra.findAll {it }.join(" ")
                break
            case float:
            case Float:
                type = "FLOAT"
                break
            case double:
            case Double:
            case BigDecimal:
                type = "DOUBLE"
                break
            case LocalDate:
                type = "DATE"
                break
            case LocalDateTime:
                type = "DATETIME"
                break
            case LocalTime:
                type = "TIME"
                break
            case URL:
            case URI:
            case Collection:
            case Map:
                type = column?.key() || column?.unique() || (column?.length() ?: 256) <= 255 ? "VARCHAR(${column?.length() ?: 255})" : "TEXT"
                break
            case Enum:
                type = "ENUM('" + field.type.getEnumConstants().join("','") + "')"
                break
            case byte[]:
                int len = column?.length() ?: 65535
                switch (true) {
                    case len < 256      : type = "TINYBLOB"; break
                    case len < 65536    : type = "BLOB"; break
                    case len < 16777216 : type = "MEDIUMBLOB"; break
                    default             : type = "LONGBLOB"; break
                }
            default:
                // Having a constructor with String or Having a static method 'fromString'
                boolean canImport = false
                try {
                    field.type.getConstructor(String.class)
                    canImport = true
                } catch(Exception ignore) {
                    try {
                        Method method = field.type.getDeclaredMethod("fromString", String.class)
                        canImport = Modifier.isStatic(method.modifiers) && method.returnType == field.type
                    } catch(Exception ignored) {}
                }
                if(canImport) {
                    int len = column?.length() ?: 256
                    type = len < 256 ? "VARCHAR($len)" : "TEXT"
                } else {
                    Log.w("Unknown field type: %s", field.type.simpleName)
                    Log.d("If you want to able to use '%s' type in the database, either set `fromString` as static method or set a constructor which accepts `String`", field.type.simpleName)
                }
            }
            return type
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
     * Get FK
     * @param field
     * @return
     */
    protected String getForeignKey(final Field field) {
        String cname = getColumnName(field)
        String indices = ""
        switch (field.type) {
            case Model:
                Constructor<?> ctor = field.type.getConstructor()
                Model refType = (ctor.newInstance() as Model)
                String joinTable = refType.tableName
                Column column = field.getAnnotation(Column)
                String action = column ? column.ondelete().toString() : Column.class.getMethod("ondelete").defaultValue.toString()
                indices = "CONSTRAINT `${tableName}_${cname}_fk` FOREIGN KEY (`${cname}`) " +
                          "REFERENCES `${joinTable}`(`${getColumnName(refType.pk)}`) ON DELETE ${action}"
                break
        }
        return indices
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
        String pk = null
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
            list = tableConnector.get([(id): model.id]).toListMap().collect { setMap(it) }
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
        int id = model.id
        try {
            Map map = getMap(model)
            exclude.each {
                map.remove(it)
            }
            ok = tableConnector.key(pk).update(map, id)
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
        int id = model.id
        int lastId = 0
        DB db
        try {
            Map<String, Object> map = getMap(model)
            db = tableConnector
            boolean ok = db.insert(map)
            lastId = 0
            if (ok) {
                lastId = db.lastID
            } else {
                Log.w("Unable to insert row : %s", map.toSpreadMap())
            }
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            closeConnections()
        }
        return lastId ?: id
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
        tableConnector.set(new Query("DROP TABLE IF EXISTS ${tableName}"))
    }
}
