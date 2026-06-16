package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.core.ext.ToMap
import com.intellisrc.core.ext.ToMapConverter
import com.intellisrc.db.annot.Column
import groovy.transform.CompileStatic

import java.lang.reflect.Field

@CompileStatic
abstract class Model implements ToMap {
    protected Field pkField
    /**
     * Get ID as Int
     * @return
     */
    int getUniqueId() {
        int i = 0
        //noinspection GroovyFallthrough
        switch (primaryKey?.type) {
            case short:
            case int:
            case long:
            case Integer:
            case Long:
            case BigInteger:
                i = this[primaryKey.name] as int
                break
            case Model:
                i = (this[primaryKey.name] as Model).uniqueId as int
                break
            default:
                if(primaryKey?.type) {
                    Log.w("Primary Key must be of type INTEGER or MODEL in table: %s, found: %s", tableName, primaryKey.type.simpleName)
                }
        }
        return i
    }
    /**
     * Get the table name of a Type
     * @return
     */
    String getTableName() {
        String name
        if(relational.tableModelRel.containsValue(this.class)) {
            name = relational.tableName
        } else {
            Log.w("Unable to find table for Model. Be sure that Table class has the generic Model type specified: 'extends Table<%s>'", this.class.simpleName)
            name = (this.class.simpleName + "s").toSnakeCase()
        }
        return name
    }
    /**
     * Get Primary Name field
     * @return
     */
    Field getPrimaryKey() {
        Field primary = pkField ?: fields.find {
            it.getAnnotation(Column)?.primary()
        }
        if(!primary) {
            primary = fields.find { it.name == "id" }
        }
        pkField = primary
        return primary
    }

    /**
     * Get relational (table/view)
     * @return
     */
    Relational getRelational() {
        return Relational.getTableOrView(this)
    }

    /**
     * Get all fields annotated with @Column (ignore any not annotated)
     * @return
     */
    List<Field> getFields() {
        return this.class.declaredFields.findAll {!it.synthetic && it.isAnnotationPresent(Column) }.toList()
    }
    /**
     * Convert Type to Map suitable for database operations
     * Some data types may be replaced (e.g. Map, List, File, etc)
     *
     * NOTE: before it was using toMap(), but as it is commonly overrode it can interfere
     * with the automatic conversion of data. If it is not correctly done, you can
     * always override this method.
     * @return
     */
    Map<String, Object> toDB() {
        return relational.convertToDB(asMap()) // We don't use toMap() here as it may be override
    }
    /**
     * Convert Model fields to Map preserving types
     * NOTE: This is not the same as 'toMap()' because it converts the field names
     * @return
     */
    protected Map<String, Object> asMap() {
        Map<String, Object> map = fields.collectEntries {
            Field field ->
                [(relational.getColumnName(field)): this[field.name]]
        }
        return map
    }
    /**
     * Convert to Map (similar to ToMap trait but modified to work with Auto fields)
     * @return
     */
    @Override
    Map<String,Object> toMap() {
        return toMap(false)
    }
    /**
     * Return map keys as snakeCase or as declared
     * @param snakeCase
     * @return
     */
    Map<String,Object> toMap(boolean snakeCase) {
        return fields.collectEntries {
            Object value = ToMapConverter.convert(this[it.name]) //Here we don't convert name as we need it raw
            String name = relational.getColumnName(it, false, snakeCase)  //<-- here is the difference (we need to use getColumnName)
            return [(name): value]
        }
    }
}
