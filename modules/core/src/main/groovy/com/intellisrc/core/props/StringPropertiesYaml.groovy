package com.intellisrc.core.props

import groovy.transform.CompileStatic
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import static org.yaml.snakeyaml.DumperOptions.FlowStyle.FLOW

/**
 * String properties in which List and Map are converted from/into YAML objects
 */
@CompileStatic
abstract class StringPropertiesYaml extends StringProperties implements StringToYamlConverter {
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

}
