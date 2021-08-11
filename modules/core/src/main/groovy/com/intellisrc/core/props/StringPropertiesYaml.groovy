package com.intellisrc.core.props

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import static org.yaml.snakeyaml.DumperOptions.FlowStyle.FLOW

@CompileStatic
abstract class StringPropertiesYaml extends StringProperties {
    StringPropertiesYaml(String keyPrefix = "", String prefixSeparator = ".") {
        super(keyPrefix, prefixSeparator)
    }

    @Override
    boolean set(String key, Collection collection) {
        Yaml yaml = new Yaml(new DumperOptions(defaultFlowStyle : FLOW))
        return set(key, yaml.dump(collection.toList()).trim().replaceAll("\\r|\\n", ""))
    }

    @Override
    boolean set(String key, Map map) {
        Yaml yaml = new Yaml(new DumperOptions(defaultFlowStyle : FLOW))
        return set(key, yaml.dump(map).trim().replaceAll("\\r|\\n", ""))
    }

    @Override
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

    @Override
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
