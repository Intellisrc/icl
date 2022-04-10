package com.intellisrc.core.props

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * These properties will try to find any property either
 * in `Environment variables`, `config.properties` and `System.properties` (in that order)
 *
 * Be careful when using this class as it may pose a security risk
 * if other users are allowed to modify other properties.
 *
 * This class may be particularly useful when working with docker as Environment variables
 * are searched first and then in the configuration file
 *
 */
@CompileStatic
class AnyProperties implements StringPropertiesGet {
    @Override
    Set<String> getKeys() {
        return Config.system.keys + Config.keys + Config.env.keys
    }

    @Override
    String get(String key, String val) {
        return Config.env.get(key, Config.get(key, Config.system.get(key, val)))
    }

    @Override
    List get(String key, List defVal) {
        return Config.env.get(key, Config.get(key, Config.system.get(key, defVal)))
    }

    @Override
    Map get(String key, Map defVal) {
        return Config.env.get(key, Config.get(key, Config.system.get(key, defVal)))
    }

    @Override
    boolean exists(String key) {
        return Config.exists(key) || Config.system.exists(key) || Config.env.exists(key)
    }
}
