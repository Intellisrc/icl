package com.intellisrc.db

import com.intellisrc.core.Log
import com.intellisrc.db.annot.Column
import com.intellisrc.etc.Instanciable
import groovy.transform.CompileStatic

import java.lang.reflect.Field

@CompileStatic
abstract class Model<T extends Table> implements Instanciable<T> {
    protected Field pkField
    /**
     * Get ID as Int
     * @return
     */
    int getId() {
        int i = 0
        switch (pk?.type) {
            case short:
            case int:
            case long:
            case Integer:
            case Long:
            case BigInteger:
                i = this[pk.name] as int
                break
            default:
                if(pk?.type) {
                    Log.w("Primary Key must be of type INTEGER in table: %s, found: %s", tableName, pk.type.name)
                }
        }
        return i
    }
    /**
     * Get the table name of a Type
     * @return
     */
    String getTableName() {
        return table.tableName
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
     * Get Table instance
     * @return
     */
    T getTable() {
        return getParametrizedInstance()
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
        return table.getMap(this)
    }
}