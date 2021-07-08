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
    protected final String name
    protected DB currentTable

    Table(String name = "") {
        if(!Database.initialized) {
            Database.init()
        }
        this.name = name ?: this.class.getAnnotation(TableMeta)?.name() ?: this.class.simpleName.toLowerCase()
        createTable()
    }

    void createTable() {
        boolean exists = table.exists()
        if(exists) {
            boolean drop = Config.getBool("db.table.${tableName}.drop")
            boolean dropAll = Config.getBool("db.tables.drop")
            if (drop || dropAll) {
                table.exec(new Query("DROP TABLE IF EXISTS ${tableName}"))
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
                            if (column.unique()) {
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
            if (!table.exec(new Query(createSQL))) {
                println createSQL
                Log.e("Unable to create table.") //TODO: show error
            }
        }
        table.close()
    }

    List<Field> getFields() {
        int index = 0
        return getParametrizedInstance(index).class.declaredFields.findAll {!it.synthetic }.toList()
    }

    /**
     * Get fields names with values as Map from a Type Object
     * @param type
     * @return
     */
    Map<String, Object> getMap(T type) {
        Map<String, Object> map = fields.collectEntries {
            [(getColumnName(it)) : type[it.name]]
        }
        return convertToDB(map)
    }
    /**
     * Converts fields of a class into db
     * @param map
     * @return
     */
    static Map<String, Object> convertToDB(Map<String, Object> map) {
        map.each {
            key, val ->
                map[key] = toDBValue(val)
        }
        return map
    }
    /**
     * Converts any Object to DB value
     * @param val
     * @return
     */
    static toDBValue(Object val) {
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
        T type = parametrizedInstance
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
                        type[origName] = it.value.toString() == "true"
                        break
                    case Collection:
                        try {
                            type[origName] = new Yaml().load((it.value ?: "").toString()) as List
                        } catch (Exception e) {
                            Log.w("Unable to parse list value in field %s: %s", origName, e.message)
                            type[origName] = []
                        }
                        break
                    case Map:
                        try {
                            type[origName] = new Yaml().load((it.value ?: "").toString()) as Map
                        } catch (Exception e) {
                            Log.w("Unable to parse map value in field %s: %s", origName, e.message)
                            type[origName] = [:]
                        }
                        break
                    case LocalDate:
                        type[origName] = (it.value as LocalDateTime).toLocalDate()
                        break
                    case LocalTime:
                        type[origName] = (it.value as LocalDateTime).toLocalTime()
                        break
                    case URI:
                        type[origName] = new URI(it.value.toString())
                        break
                    case URL:
                        type[origName] = new URL(it.value.toString())
                        break
                    case Enum:
                        type[origName] = Enum.valueOf((Class<Enum>) field.type, it.value.toString())
                        break
                    case Model:
                        Constructor<?> c = field.type.getConstructor()
                        Model refType = (c.newInstance() as Model)
                        type[origName] = refType.table.get(it.value as int)
                        break
                    default:
                        type[origName] = it.value
                }
            } else {
                Log.w("Field not found: %s", origName)
            }
        }
        return type
    }

    /**
     * Return SQL rules related to NULL and DEFAULT
     * @param field
     * @param nullable
     * @return
     */
    String getDefaultValue(Field field, boolean nullable = false) {
        T t = parametrizedInstance
        Object val = field.get(t)
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
        String fname = columnName.toCamelCase()
        return fname
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
        String pk = "id"
        Field field = getFields().find {
            it.getAnnotation(Column)?.primary()
        }
        if(field) {
            pk = getColumnName(field)
        }
        return pk
    }

    T get(int id) {
        Map map = table.key(pk).get(id).toMap()
        T type = setMap(map)
        table.close()
        return type
    }

    List<T> getAll(int id) {
        return getAll([id])
    }

    List<T> findAll(String fieldName, Model type) {
        Field f = getFields().find {
            it.name == fieldName
        }
        List<T> list = []
        if(f) {
            String id = getColumnName(f)
            list = table.get([(id): type.id] as Map<String, Object>).toListMap().collect { setMap(it) }
        } else {
            Log.w("Unable to find field: %s", fieldName)
        }
        return list
    }

    List<T> getAll(List<Integer> ids) {
        List<Map> list = table.key(pk).get(ids).toListMap()
        List<T> all = list.collect {
            Map map ->
                return setMap(map)
        }
        table.close()
        return all
    }

    List<T> getAll(Map<String, Object> options = [:]) {
        DB con = table
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
        table.close()
        return all
    }

    T find(String column, Object value) {
        Map map = table.get([(column): value]).toMap()
        T type = setMap(map)
        table.close()
        return type
    }

    List<T> findAll(String column, Object value) {
        List<Map> list = table.get([(column): value]).toListMap()
        List<T> all = list.collect {
            Map map ->
                return setMap(map)
        }
        table.close()
        return all
    }

    boolean update(T type, List<String> exclude = []) {
        int id = type.id
        Map map = getMap(type)
        exclude.each {
            map.remove(it)
        }
        boolean ok = table.key(pk).update(map, id)
        return table.close() && ok
    }

    boolean delete(int id) {
        boolean ok = table.key(pk).delete(id)
        return table.close() && ok
    }

    boolean delete(Map map) {
        boolean ok = table.key(pk).delete(map)
        return table.close() && ok
    }

    boolean delete(List<Integer> ids) {
        boolean ok = table.key(pk).delete(ids)
        return table.close() && ok
    }

    int insert(T type) {
        int id = type.id
        int lastId = 0
        try {
            Map<String, Object> map = getMap(type)
            boolean ok = table.insert(map)
            lastId = 0
            if (ok) {
                lastId = table.lastID
            } else {
                Log.w("Unable to insert row : %s", map.toSpreadMap())
            }
            table.close()
        } catch(Exception e) {
            Log.e("Unable to insert record", e)
        }
        return lastId ?: id
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
    DB getTable() {
        if(!currentTable || currentTable.closed) {
            currentTable = Database.connect().table(tableName)
        }
        return currentTable
    }

    static void quit() {
        Database.quit()
    }
}
