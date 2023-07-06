package com.intellisrc.db.annot

import groovy.transform.AnnotationCollector
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
@AnnotationCollector([ViewMeta, UpdaterMeta])
@interface TableMeta {}