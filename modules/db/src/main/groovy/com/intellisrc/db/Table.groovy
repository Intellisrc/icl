package com.intellisrc.db

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.annot.Column
import com.intellisrc.db.annot.TableMeta
import com.intellisrc.etc.Instanciable
import groovy.transform.CompileStatic
import javassist.Modifier
import org.yaml.snakeyaml.Yaml

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@CompileStatic
class Table<T extends Model> implements Instanciable<T> {
    protected Database database
    protected final String name
    protected Set<DB> activeConnections = []

    Table(String name = "", Database database = null) {
        this.database = database ?: Database.getDefault()
        this.name = name ?: this.class.getAnnotation(TableMeta)?.name() ?: this.class.simpleName.toSnakeCase()
        assert this.name : "Table name not set"
        createTable()
    }

    void createTable() {
        boolean exists = tableConnect.exists()
        if(exists) {
            boolean drop = Config.getBool("db.table.${tableName}.drop")
            boolean dropAll = Config.getBool("db.tables.drop")
            if (drop || dropAll) {
                tableConnect.exec(new Query("DROP TABLE IF EXISTS ${tableName}"))
                exists = false
            }
        }
        if(!exists) {
            String charset = "utf8"
            String engine = ""
            if(this.class.isAnnotationPresent(TableMeta)) {
                TableMeta meta = this.class.getAnnotation(TableMeta)
                if(meta.engine() != "auto") {
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
                    String fname = getColumnName(field)
                    Column column = field.getAnnotation(Column)
                    List<String> parts = ["`${fname}`".toString()]
                    if (! column) {
                        String type = getColumnDefinition(field)
                        assert type: "Unknown type: ${field.type.simpleName} in ${field.name}"
                        parts << type
                        String defaultVal = getDefaultValue(field)
                        if(defaultVal) {
                            parts << defaultVal
                        }
                    } else {
                        if (column.columnDefinition()) {
                            parts << column.columnDefinition()
                        } else {
                            String type = column.type() ?: getColumnDefinition(field, column)
                            parts << type

                            String defaultVal = getDefaultValue(field, column.nullable())
                            if(defaultVal) {
                                parts << defaultVal
                            }

                            List<String> extra = []
                            if (column.unique() || column.uniqueGroup()) {
                                if(column.uniqueGroup()) {
                                    if(! uniqueGroups.containsKey(column.uniqueGroup())) {
                                        uniqueGroups[column.uniqueGroup()] = []
                                    }
                                    uniqueGroups[column.uniqueGroup()] << fname
                                } else {
                                    extra << "UNIQUE"
                                }
                            }
                            if(!extra.empty) {
                                parts.addAll(extra)
                            }
                        }
                        if(column.key()) {
                            keys << "KEY `${tableName}_${fname}_key_index` (`${fname}`)".toString()
                        }
                    }
                    columns << parts.join(' ')
            }
            if(!keys.empty) {
                columns.addAll(keys)
            }
            if(! uniqueGroups.keySet().empty) {
                uniqueGroups.each {
                    columns << "UNIQUE KEY `${tableName}_${it.key}` (`${it.value.join('`, `')}`)".toString()
                }
            }
            String fks = getFields().collect { getForeignKey(it) }.findAll { it }.join(",\n")
            if(fks) {
                columns << fks
            }
            if(engine) {
                engine = "ENGINE=${engine}"
            }
            createSQL += columns.join(",\n") + "\n) ${engine} CHARACTER SET=${charset}"
            println createSQL
            if (!tableConnect.exec(new Query(createSQL))) {
                println createSQL
                Log.e("Unable to create table.")
            }
        }
        closeConnections()
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
    Map<String, Object> getMap(T model) {
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
        switch (val) {
            case LocalTime:
                return  (val as LocalTime).HHmmss
            case LocalDate:
                return (val as LocalDate).YMD
            case LocalDateTime:
                return (val as LocalDateTime).YMDHms
            case Collection:
                List list = (val as List)
                return new Yaml().dump(list.empty ? [] : list.collect {
                   toDBValue(it)
                })
            case Map:
                return new Yaml().dump(val)
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
     * Convert Map to Object
     * @param map
     * @return
     */
    T setMap(Map<String, Object> map) {
        T model = parametrizedInstance
        map.each {
            String origName = it.key.toCamelCase()
            Field field = getFields().find { it.name == origName }
            // Look for Type ID
            if(!field && it.key.endsWith("_id")) {
                origName = (it.key.replaceAll(/_id$/,'')).toCamelCase()
                field = getFields().find { it.name == origName }
            }
            if(field) {
                switch (field.type) {
                    case boolean:
                    case Boolean:
                        model[origName] = it.value.toString() == "true"
                        break
                    case Collection:
                        try {
                            model[origName] = new Yaml().load((it.value ?: "").toString()) as List
                        } catch (Exception e) {
                            Log.w("Unable to parse list value in field %s: %s", origName, e.message)
                            model[origName] = []
                        }
                        break
                    case Map:
                        try {
                            model[origName] = new Yaml().load((it.value ?: "").toString()) as Map
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
                    case Enum:
                        model[origName] = Enum.valueOf((Class<Enum>) field.type, it.value.toString())
                        break
                    case Model:
                        Constructor<?> c = field.type.getConstructor()
                        Model refType = (c.newInstance() as Model)
                        model[origName] = refType.table.get(it.value as int)
                        break
                    default:
                        model[origName] = it.value
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
        T model = parametrizedInstance
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
            default:
                Log.w("Unknown field type: %s", field.type.class.simpleName)
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
    T get(int id) {
        Map map = tableConnect.key(pk).get(id)?.toMap() ?: [:]
        T model = setMap(map)
        closeConnections()
        return model
    }
    /**
     * Get a list of items using ids
     * @param ids
     * @return
     */
    List<T> get(List<Integer> ids) {
        List<Map> list = tableConnect.key(pk).get(ids).toListMap()
        List<T> all = list.collect {
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
    List<T> getAll(Map options = [:]) {
        DB con = tableConnect
        if(options.limit) {
            con = con.limit(options.limit as int, (options.offset ?: 0) as int)
        }
        if(options.sort) {
            con = con.order(options.sort.toString(), (options.order ?: "ASC") as Query.SortOrder)
        }
        List<Map> list = con.get().toListMap()
        List<T> all = list.collect {
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
    List<T> findAll(String fieldName, Model model) {
        Field f = getFields().find {
            it.name == fieldName
        }
        List<T> list = []
        if(f) {
            String id = getColumnName(f)
            list = tableConnect.get([(id): model.id]).toListMap().collect { setMap(it) }
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
    T find(String column, Object value) {
        return find([(column): value])
    }
    /**
     * Find a single item using multiple columns
     * @param criteria
     * @return
     */
    T find(Map criteria) {
        criteria = convertToDB(criteria)
        Map map = tableConnect.get(criteria)?.toMap() ?: [:]
        T model = setMap(map)
        closeConnections()
        return model
    }
    /**
     * Find all items which matches a column and a value
     * @param column
     * @param value
     * @return
     */
    List<T> findAll(String column, Object value) {
        return findAll([(column): value])
    }
    /**
     * Find all items matching multiple columns
     * @param criteria
     * @return
     */
    List<T> findAll(Map criteria) {
        criteria = convertToDB(criteria)
        List<Map> list = tableConnect.get(criteria).toListMap()
        List<T> all = list.collect {
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
    boolean update(T model, List<String> exclude = []) {
        int id = model.id
        Map map = getMap(model)
        exclude.each {
            map.remove(it)
        }
        boolean ok = tableConnect.key(pk).update(map, id)
        closeConnections()
        return ok
    }
    /**
     * Delete using model
     * @param model
     * @return
     */
    boolean delete(T model) {
        return model.id ? delete(model.id) : delete(getMap(model))
    }
    /**
     * Delete with ID
     * @param id
     * @return
     */
    boolean delete(int id) {
        boolean ok = tableConnect.key(pk).delete(id)
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
        boolean ok = tableConnect.key(pk).delete(map)
        closeConnections()
        return ok
    }
    /**
     * Delete using multiple IDs
     * @param ids
     * @return
     */
    boolean delete(List<Integer> ids) {
        boolean ok = tableConnect.key(pk).delete(ids)
        closeConnections()
        return ok
    }
    /**
     * Insert a model
     * @param model
     * @return
     */
    int insert(T model) {
        int id = model.id
        int lastId = 0
        DB db
        try {
            Map<String, Object> map = getMap(model)
            db = tableConnect
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
     * Replace a model
     * @param model
     * @return
     */
    boolean replace(T model) {
        int id = model.id
        boolean ok = false
        try {
            Map<String, Object> map = getMap(model)
            ok = tableConnect.replace(map)
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        } finally {
            closeConnections()
        }
        return ok
    }
    /**
     * Returns a new instance of this class's generic type
     * @return
     */
    T getNew() {
        return getParametrizedInstance()
    }
    /**
     * Returns a new query with the table already set
     * @return
     */
    Query getQuery() {
        return new Query().setTable(tableName)
    }
    /**
     * Get a connection. If its already opened, reuse
     * @return
     */
    synchronized DB getTableConnect() {
        DB db = database.connect().table(tableName)
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
}
