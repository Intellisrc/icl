package com.intellisrc.core.props

import groovy.transform.CompileStatic

/**
 * @since 2021/07/26.
 */
@CompileStatic
trait PrefixedProperties {
    String prefix = ""
    String prefixSeparator = "."
    /**
     * Return key with prefix if set
     * @param key
     * @return
     */
    String getFullKey(String key) {
        return prefix ? prefix + prefixSeparator + key : key
    }
}
