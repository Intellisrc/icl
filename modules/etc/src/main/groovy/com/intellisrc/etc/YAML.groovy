package com.intellisrc.etc

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.yaml.snakeyaml.Yaml

/**
 * Simple wrapper to be similar to YAML wrapper
 * @since 2021/08/20.
 */
@CompileStatic
class YAML {
    /**
     * Decode a YAML string into an object
     * @param yaml
     * @return
     */
    static <T>T decode(String yaml) {
        T ret = null
        try {
            ret = (T) new Yaml().load(yaml)
        } catch(Exception e) {
            Log.e("Unable to decode YAML", e)
        }
        return ret
    }

    /**
     * Encode an object into a YAML string
     * @param object
     * @return
     */
    static String encode(Object object) {
        return new Yaml().dump(object)
    }
}
