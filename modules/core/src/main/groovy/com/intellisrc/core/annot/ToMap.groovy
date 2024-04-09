package com.intellisrc.core.annot

import groovy.transform.CompileStatic

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.TYPE
import static java.lang.annotation.RetentionPolicy.RUNTIME

@CompileStatic
trait ToMap {
    Map<String,Object> toMap() {
        return this.class.declaredFields.findAll {
            ! it.synthetic
        }.collectEntries {
            [(it.name) : this[it.name]]
        }
    }
    Map<String,Object> toSnakeMap(Object self) {
        return toMap().collectEntries {
            [(it.key.toSnakeCase()) : it.value]
        }
    }
}