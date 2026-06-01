package com.intellisrc.db

import com.intellisrc.db.annot.DeleteActions
import com.intellisrc.db.annot.UpdateActions
import groovy.transform.CompileStatic

/**
 * Structure contract for NormalizedColumn
 * @since 2026/05/27.
 */
@CompileStatic
class ColumnDefinition {
    // Used for unlimited length
    public static final int UNLIMITED = -1

    boolean autoIncrement   = false
    boolean index           = false
    boolean nullable        = true
    boolean primaryKey      = false
    boolean unique          = false
    boolean unsigned        = false
    int length              = 0 // you can use UNLIMITED here
    String name             = ""
    String uniqueGroup      = null   // Can be null
    String customType       = ""
    DeleteActions ondelete  = DeleteActions.RESTRICT
    UpdateActions onupdate  = UpdateActions.NO_ACTION
    Class<?> type           = null
    Object defaultValue     = null
}