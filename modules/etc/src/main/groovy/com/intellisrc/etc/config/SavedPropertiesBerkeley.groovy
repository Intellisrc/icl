package com.intellisrc.etc.config

import com.intellisrc.etc.BerkeleyDB
import groovy.transform.CompileStatic
import org.yaml.snakeyaml.Yaml

/**
 * Implementation of SavedProperties for BerkeleyDB
 * @since 2019/12/09.
 */
@CompileStatic
class SavedPropertiesBerkeley extends SavedProperties {
    BerkeleyDB db
    /**
     * Constructor
     * @param root
     * @param databaseFile
     */
    SavedPropertiesBerkeley(String root, File databaseFile = null) {
        super(root)
        db = new BerkeleyDB(databaseFile)
    }

    @Override
    void set(String key, String value) {
        db.set(propKey + "." + key, value)
    }

    @Override
    void set(String key, Collection<String> list) {
        Yaml yaml = new Yaml()
        db.set(propKey + "." + key, yaml.dump(list))
    }

    @Override
    void set(String key, Map<String, String> map) {
        Yaml yaml = new Yaml()
        db.set(propKey + "." + key, yaml.dump(map))
    }

    @Override
    String get(String key, String defVal) {
        return exists(key) ? db.get(key) : defVal
    }

    @Override
    Collection<String> get(String key, Collection<String> defVal) {
        Yaml yaml = new Yaml()
        return exists(key) ? yaml.load(db.get(key)) as List<String> : defVal
    }

    @Override
    Map<String, String> get(String key, Map<String, String> defVal) {
        Yaml yaml = new Yaml()
        return exists(key) ? yaml.load(db.get(key)) as Map<String, String> : defVal
    }

    @Override
    boolean exists(String key) {
        return db.has(key)
    }

    @Override
    Map<String, String> getAll() {
        return db.getMapAsString()
    }

    @Override
    boolean delete(String key) {
        return db.delete(key)
    }

    @Override
    boolean clear() {
        return ! db.keys.any {
            return ! db.delete(it)
        }
    }
}
