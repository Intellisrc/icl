package com.intellisrc.db.annot

import groovy.transform.CompileStatic

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

@Target([TYPE])
@Retention(RUNTIME)
@CompileStatic
@interface TableMeta {
    String name() default ""
    String engine() default "auto"
    String charset() default "UTF8"
}