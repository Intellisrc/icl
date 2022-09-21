package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.annot.Column
import groovy.transform.CompileStatic

import java.lang.reflect.Field

@CompileStatic
abstract class Model {
    protected Field pkField
    /**
     * Get ID as Int
     * @return
     */
    int getUniqueId() {
        int i = 0
        //noinspection GroovyFallthrough
        switch (pk?.type) {
            case short:
            case int:
            case long:
            case Integer:
            case Long:
            case BigInteger:
                i = this[pk.name] as int
                break
            case Model:
                i = (this[pk.name] as Model).uniqueId as int
                break
            default:
                if(pk?.type) {
                    Log.w("Primary Key must be of type INTEGER or MODEL in table: %s, found: %s", tableName, pk.type.simpleName)
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
        if(Table.relation.containsKey(this.class.name)) {
            name = Table.relation[this.class.name].tableName
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
    Field getPk() {
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
     * Get all fields
     * @return
     */
    List<Field> getFields() {
        return this.class.declaredFields.findAll {!it.synthetic }.toList()
    }
    /**
     * Convert Type to Map
     * @return
     */
    Map<String, Object> toMap() {
        Map<String, Object> map = fields.collectEntries {
            Field field ->
                [(Table.getColumnName(field)) : this[field.name]]
        }
        return Table.convertToDB(map, true)
    }
}
