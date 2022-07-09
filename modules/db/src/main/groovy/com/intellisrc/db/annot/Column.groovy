package com.intellisrc.db.annot

import groovy.transform.CompileStatic

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static com.intellisrc.db.annot.DeleteActions.RESTRICT
import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.RetentionPolicy.RUNTIME

@Target([FIELD])
@Retention(RUNTIME)
@CompileStatic
@interface Column {
    boolean primary() default false             // Primary Key
    boolean autoincrement() default false       // Set autoincrement column
    boolean unique() default false              // Unique (when using with uniqueGroup will set a constraint)
    boolean nullable() default true             // Nullable otherwise will set default value
    boolean unsigned() default true             // Unsigned number
    boolean key() default false                 // Add KEY INDEX
    String type() default ""                    // Override field type, for example: DECIMAL(5,2)
    String uniqueGroup() default ""             // Used together with "unique" to add multi-column constraints by name
    DeleteActions ondelete() default RESTRICT   // Action to follow when deleting a FK
    int length() default 0                      // Override type length (specially numbers)

    // NOTE: Setting any of the following, may not work correctly if database type changes (use only in extreme cases):
    String columnDefinition() default ""        // Override all column definition (after column name)
    String defaultValue() default ""            // Manually set which should be default value ("" == unset)
}
