package com.intellisrc.db.auto

import com.intellisrc.core.Log
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.NormalizedColumn
import com.intellisrc.db.annot.Column
import groovy.transform.CompileStatic

/**
 * Information about a Field that will be used as column in a DB
 * @see com.intellisrc.db.annot.Column
 */
@CompileStatic
class ColumnDB implements NormalizedColumn {
    String name
    Class<?> type
    Object defaultVal
    Column annotation

    @Override
    ColumnDefinition getNormalized() {
        boolean hasCustomDef = ! annotation.columnDefinition().empty
        boolean fk = type && Model.isAssignableFrom(type)
        String refTable = ""
        String refColumn = ""
        if (fk) {                                                                                                                                                                                                    
            try {                                                                                                                                                                                                    
                Model refModel = (Model) type.getDeclaredConstructor().newInstance()                                                                                                                                 
                refTable = refModel.tableName                                                                                                                                                                        
                refColumn = Relational.getColumnName(refModel.primaryKey)                                                                                                                                            
            } catch (Exception e) {                                                                                                                                                                                  
                Log.e("Failed to resolve foreign key for field: " + name, e)
            }                                                                                                                                                                                                        
        }  
        return new ColumnDefinition(
            autoIncrement: this.annotation.autoincrement(),
            index: annotation.key(),
            nullable: this.annotation.nullable(),
            primaryKey: annotation.primary(),
            unique: annotation.unique(),
            length: this.annotation.length(),
            name: this.name,
            uniqueGroup: annotation.uniqueGroup(),
            customType: hasCustomDef ? annotation.columnDefinition() : "",
            onDelete: this.annotation.ondelete(),
            onUpdate: this.annotation.onupdate(),
            type: this.type,
            defaultValue: this.defaultVal,
            isForeignKey: fk,
            referenceTable: refTable,
            referenceColumn: refColumn
        )
    }
}
