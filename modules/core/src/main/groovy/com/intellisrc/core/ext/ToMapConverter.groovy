package com.intellisrc.core.ext

import groovy.transform.CompileStatic

/**
 * @since 2024/06/13.
 */
@CompileStatic
class ToMapConverter {
    static Object convert(Object obj) {
        if(obj !== null) {
            if (obj instanceof ToMap) {
                obj = obj.toMap()
            } else if(obj instanceof Collection) {
                obj = (obj as Collection).collect { convert(it) }
            } else if(obj instanceof Map) {
                obj = (obj as Map).collectEntries { [(it.key) : convert(it.value) ]}
            } else {
                try {
                    obj = obj.class.getMethod("toMap", null)
                } catch (NoSuchMethodException | SecurityException ignore) {}
            }
        }
        return obj
    }
}
