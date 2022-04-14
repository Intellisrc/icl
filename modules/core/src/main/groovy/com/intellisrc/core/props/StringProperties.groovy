package com.intellisrc.core.props

import groovy.transform.CompileStatic

/**
 * This class is a partial implementation
 * for those classes which stores values
 * as String (Config, BerkeleyDB, etc)
 *
 */
@CompileStatic
abstract class StringProperties implements StringPropertiesGet, StringPropertiesSet, PrefixedProperties {
    abstract boolean set(String key, String val)

    StringProperties(String keyPrefix = "", String prefixSeparator = ".") {
        this.prefix = keyPrefix
        this.prefixSeparator = prefixSeparator
    }
}
