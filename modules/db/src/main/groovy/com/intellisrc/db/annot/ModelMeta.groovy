package com.intellisrc.db.annot

import groovy.transform.CompileStatic

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * Annotations for Model classes
 */
@Target([TYPE])
@Retention(RUNTIME)
@CompileStatic
@interface ModelMeta {
    int version() default 1
}