package com.intellisrc.core.ext

import groovy.transform.CompileStatic

@CompileStatic
trait ToMap {
    Map<String,Object> toMap() {
        return this.class.declaredFields.findAll {
            ! it.synthetic
        }.collectEntries {
            Object value = ToMapConverter.convert(this[it.name])
            return [(it.name) : value]
        }
    }
    Map<String,Object> toSnakeMap(Object self) {
        return toMap().collectEntries {
            [(it.key.toSnakeCase()) : it.value]
        }
    }
}