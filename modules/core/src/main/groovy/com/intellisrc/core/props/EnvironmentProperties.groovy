package com.intellisrc.core.props

import groovy.transform.CompileStatic

/**
 * Environment Properties accessor
 *
 * It allows to have GET access to the Environment variables
 */
@CompileStatic
class EnvironmentProperties implements StringPropertiesGet, StringToYamlConverter {
    protected final Map<String, String> values = [:]
    EnvironmentProperties() {
        // Fill Environment variables into env
        System.getenv().each {
            values[(it.key.toLowerCase().replaceAll("_","."))] = values[it.key] = it.value
        }
    }
    @Override
    String get(String key, String defVal) {
        return exists(key) ? values.get(key) : defVal
    }

    @Override
    boolean exists(String key) {
        return values.containsKey(key)
    }

    @Override
    Set<String> getKeys() {
        return values.keySet()
    }
}
