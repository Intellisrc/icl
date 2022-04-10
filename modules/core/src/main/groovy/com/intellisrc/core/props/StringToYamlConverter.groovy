package com.intellisrc.core.props

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.yaml.snakeyaml.Yaml

/**
 * Yaml conversion from String
 */
@CompileStatic
trait StringToYamlConverter {
    abstract boolean exists(String key)
    abstract String get(String key)

    List get(String key, List defVal) {
        List list = defVal
        if(exists(key)) {
            try {
                Yaml yaml = new Yaml()
                list = yaml.load(get(key)) as List
            } catch(Exception ignore) {
                Log.w("value of: %s can not be parsed to List", key)
            }
        }
        return list
    }

    Map get(String key, Map defVal) {
        Map map = defVal
        if(exists(key)) {
            try {
                Yaml yaml = new Yaml()
                map = yaml.load(get(key)) as Map
            } catch(Exception ignore) {
                Log.w("value of: %s can not be parsed to Map", key)
            }
        }
        return map
    }
}
