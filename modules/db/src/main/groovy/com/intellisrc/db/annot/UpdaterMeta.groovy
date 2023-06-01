package com.intellisrc.db.annot

import groovy.transform.CompileStatic

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * Annotation for Table classes (auxiliary class to reduce code)
 */
@Target([TYPE])
@Retention(RUNTIME)
@CompileStatic
@interface UpdaterMeta {
    boolean autoUpdate() default true // turn off to prevent updating table
}