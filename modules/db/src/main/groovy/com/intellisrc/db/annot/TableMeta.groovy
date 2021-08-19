package com.intellisrc.db.annot

import groovy.transform.CompileStatic

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * Annotation for Table classes
 */
@Target([TYPE])
@Retention(RUNTIME)
@CompileStatic
@interface TableMeta {
    String name() default ""
    String engine() default "auto"
    String charset() default "UTF8"
    int cache() default 0       // seconds to store in memory
    boolean clearCache() default false  // if true, will remove cache keys corresponding to this table (on update)
    boolean autoUpdate() default true // turn off to prevent updating table
}