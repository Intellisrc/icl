package com.intellisrc.etc.config

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.intellisrc.core.props.PropertiesGet
import com.intellisrc.core.props.StringProperties
import com.intellisrc.core.props.StringPropertiesYaml
import com.intellisrc.etc.BerkeleyDB
import groovy.transform.CompileStatic
import javassist.Modifier
import org.reflections.Reflections
import org.reflections.scanners.FieldAnnotationsScanner

import java.lang.annotation.Annotation
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Manages @AutoConfig annotations
 * Usage:
 *
 * ConfigAuto config = new ConfigAuto(Main.packageName)
 *
 * We use the packageName to improve performance and keep the scanning scope to a minimum
 *
 * TODO: currently there is no way to automatically update database when a static field has changed
 *       so, we use an updater class which will be looking for changes in those values at intervals
 *       ConfigAutoTask (from thread module) can be used for this task.
 *
 * @since 2021/03/12.
 */
@CompileStatic
class ConfigAuto {
    static int columnDocWrap = Config.get("config.auto.width", 75)
    // If true, it will remove missing keys
    static boolean removeMissing = Config.get("config.auto.remove", true)
    static List<String> removeIgnore = Config.getList("config.auto.ignore")
    // Where to export configuration:
    static File defaultCfgFile = Config.getFile("config.auto.file", "system.properties")

    final File cfgFile
    final StringProperties props
    boolean importOnStart = Config.get("config.auto.import", true)
    // If `exportOnSave` is true, each time a value is updated in the db, it will also update
    // the config file (by default will save on exit)
    boolean exportOnSave = Config.get("config.auto.export", false)
    protected Set<Storage> storedValues = []
    protected String basePkg
    Closer onClose = null

    interface Closer {
        void call()
    }

    /**
     * Dummy implementation of StringPropertiesYaml
     * used to convert objects into string
     */
    static class ConfigProps extends StringPropertiesYaml {
        Map<String, String> cache = [:]
        @Override
        Set<String> getKeys() {
            cache.keySet()
        }
        @Override
        String get(String key, String val) {
            return cache.containsKey(key) ? cache.get(key) : val
        }
        @Override
        boolean set(String key, String val) {
            cache[key] = val
            return true
        }
        @Override
        boolean exists(String key) {
            return cache.containsKey(key)
        }
        @Override
        boolean delete(String key) {
            return cache.remove(key)
        }
        @Override
        boolean clear() {
            cache.clear()
            return cache.keySet().empty
        }
    }

    /**
     * Basic Storage object with no instance reference
     * which can be easily tested
     */
    static class BasicStorage {
        final Class parent
        final Field field
        final Object initial
        final String description
        Object previous
        boolean export
        boolean userFriendly

        BasicStorage(Field field) {
            this.field = field
            this.initial = this.previous = getEncoded(field.get(null))
            parent = field.declaringClass
            // Check for class annotation
            Annotation classAnnotation = field.declaringClass.getAnnotation(AutoConfig)
            boolean classExport = classAnnotation ? classAnnotation.export() : true
            boolean classUserFriendly = classAnnotation ? classAnnotation.userFriendly() : false
            Annotation fieldAnnotation = field.getAnnotation(AutoConfig)
            this.export = fieldAnnotation?.export() && classExport
            this.userFriendly = fieldAnnotation?.userFriendly() || classUserFriendly
            this.description = fieldAnnotation?.description()
        }

        static String getEncoded(Object obj) {
            ConfigProps yamlProps = new ConfigProps()
            yamlProps.setObj("tmp", obj)
            return yamlProps.get("tmp")
        }

        void resetChange() {
            previous = getEncoded(current)
        }

        Object getCurrent() {
            return field.get(null)
        }

        boolean isChanged() {
            return previous != getEncoded(current)
        }

        boolean isSameAsDefault() {
            return initial == getEncoded(current)
        }
    }
    /**
     * Main class used to store and manage field values
     */
    class Storage extends BasicStorage {
        Storage(Field field) {
            super(field)
        }

        String getKey() {
            return getKey(field)
        }

        boolean save() {
            boolean updated = true
            if (changed) {
                updated = props.set(key, field)
                Log.v("Value changed: %s, Prev: %s, Now: %s", props.getFullKey(key), previous.toString(), current.toString())
                resetChange()
                if(exportOnSave) {
                    exportValues()
                }
            }
            return updated
        }

        boolean removeFromStorage() {
            return props.delete(key)
        }

        boolean isStored() {
            return props.exists(key)
        }

        void updateFieldValueAndSave(Object object) {
            updateFieldValue(object)
            if(stored && sameAsDefault) {
                removeFromStorage()
            } else {
                save() // Save as well in database
            }
        }

        void updateFieldValue(Object object) {
            setFieldValue(field, object, key)
        }
    }
    /**
     * Constructor
     *
     * @param basePackage : package name in which we will search for annotations (e.g: com.example.project)
     * @param configFile : where to store the configuration
     * @param storage : Storage used to save properties (Berkeley by default)
     */
    ConfigAuto(String basePackage, File configFile, StringProperties storage = new BerkeleyDB("main", "config")) {
        props = storage
        cfgFile = configFile
        try {
            assert basePackage : "Package was not specified."
            assert basePackage != "java.lang" : "Package was not correctly specified."
            if(storage instanceof BerkeleyDB &&! onClose) {
                onClose = { (storage as BerkeleyDB).close() } as Closer
            }
            basePkg = basePackage
            // Initialize stored values
            updateStoredValues()
            // Verify current config:
            cleanseStoredValues()
            // Import on Start
            if(importOnStart) {
                importValues()
            }
        } catch (Exception e) {
            Log.e("Unable to start AutoConfig", e)
        }
    }
    ConfigAuto(String basePackage, StringProperties storage = new BerkeleyDB("main", "config")) {
        this(basePackage, defaultCfgFile, storage)
    }

    /**
     * Get all annotated fields
     * @return
     */
    protected List<Field> getAllAnnotatedFields() {
        // Process all AutoConfig in fields:
        Reflections refFields = new Reflections(basePkg, new FieldAnnotationsScanner())
        return refFields.getFieldsAnnotatedWith(AutoConfig).collect().findAll {
            prepare(it)
        }
    }

    /**
     *  Return all keys in config
     * @return
     */
    Set<String> getAllKeys() {
        return allAnnotatedFields.collect { getKey(it) }.toSet()
    }

    /**
     * Return all defined configuration as Map
     * @return
     */
    Map<String, Object> getInitialValues() {
        updateStoredValues()
        return storedValues.collectEntries {
            [(it.key) : it.initial]
        }
    }

    /**
     * Return all defined configuration as Map
     * @return
     */
    Map<String, Object> getCurrentValues(boolean userFriendlyOnly = false) {
        updateStoredValues()
        return storedValues.findAll {
            boolean include = it.export
            if(include && userFriendlyOnly) {
                include = it.userFriendly
            }
            return include
        }.collectEntries {  //No need to sort it as it is a Map
            Object val = it.current
            // Handle not base types:
            switch (val) {
                case File :
                    val = (val as File).name
                    break
                case Inet4Address :
                    val = (val as Inet4Address).hostAddress
                    break
                case LocalTime :
                    val = (val as LocalTime).HHmmss
                    break
                case LocalDate:
                    val = (val as LocalDate).YMD
                    break
                case LocalDateTime :
                    val = (val as LocalDateTime).YMDHms
                    break
            }
            [(it.key) : val]
        }
    }

    /**
     * Set Modifier flag and warn if its not static or if its final
     * @param field
     * @return
     */
    boolean prepare(Field field) {
        boolean ok = true
        // Filter by root (class and field roots must match to be included)
        String fieldRoot = field.getAnnotation(AutoConfig)?.prefix()
        if(!fieldRoot) {
            fieldRoot = field.declaringClass.getAnnotation(AutoConfig)?.prefix() ?: props.prefix
        }
        if(props.prefix != fieldRoot) {
            Log.v("Field [%s] was skipped as roots: [%s] don't match current: %s", fieldRoot, props.prefix)
            ok = false
        }
        if(ok) {
            if (!Modifier.isStatic(field.modifiers)) {
                Log.w("Field [%s] must be static in order to use @AutoConfig", getKey(field))
                ok = false
            }
            if (Modifier.isFinal(field.modifiers)) {
                Log.w("Field [%s] must not be final in order to use @AutoConfig", getKey(field))
                ok = false
            }
        }
        if (ok) {
            field.setAccessible(true)
            if (Modifier.isPrivate(field.modifiers)) {
                Modifier.setPublic(field.modifiers)
            }
        }
        return ok
    }

    /**
     * Get Key for Class
     * @param field
     * @return
     */
    static protected String getBaseKey(final Field field) {
        return field.declaringClass.getAnnotation(AutoConfig)?.key() ?: field.declaringClass.simpleName.toLowerCase()
    }
    /**
     * Get Key in configuration, for example:
     *
     * my_class.my_field
     *
     * @param field
     * @return
     */
    protected String getKey(final Field field) {
        // Get key from field. If its not present, use field name
        String key = field.getAnnotation(AutoConfig).key() ?: field.name.toLowerCase()
        return getBaseKey(field) + props.prefixSeparator + key
    }

    /**
     * Set field value
     * @param field
     * @param obj : Can be PropertiesGet (from where we are going to read the value) or Object (value)
     * @param key
     */
    static void setFieldValue(Field field, Object obj, String key) {
        PropertiesGet getter = null
        if(obj instanceof PropertiesGet) {
            getter = obj
        }
        // Only set field values when we have such key in getter or we are passing the value
        if (getter ? getter.exists(key) : obj != null) {
            //noinspection GroovyFallthrough
            switch (field.type) {
                case boolean: case Boolean:
                    field.setBoolean(null, getter ? getter.getBool(key) : getBoolean(obj))
                    break
                case short: case Short:
                    field.setShort(null, getter ? getter.getShort(key) : obj as short)
                    break
                case int: case Integer:
                    field.setInt(null, getter ? getter.getInt(key) : obj as int)
                    break
                case long: case Long: case BigInteger:
                    field.setLong(null, getter ? getter.getLong(key) : obj as long)
                    break
                case float: case Float:
                    field.setFloat(null, getter ? getter.getFloat(key) : obj as float)
                    break
                case double: case Double:
                    field.setDouble(null, getter ? getter.getDbl(key) : obj as double)
                    break
                case BigDecimal:
                    field.set(null, getter ? getter.getBigDec(key) : obj as BigDecimal)
                    break
                case File:
                    field.set(null, getter ? getter.getFile(key)?.get() : (obj instanceof File ? obj : File.get(obj.toString())))
                    break
                case LocalDateTime:
                    field.set(null, getter ? getter.getDateTime(key)?.get() : (obj instanceof LocalDateTime ? obj : obj.toString().toDateTime()))
                    break
                case LocalDate:
                    field.set(null, getter ? getter.getDate(key)?.get() : (obj instanceof LocalDate ? obj : obj.toString().toDate()))
                    break
                case LocalTime:
                    field.set(null, getter ? getter.getTime(key)?.get() : (obj instanceof LocalTime ? obj : obj.toString().toTime()))
                    break
                case Inet4Address:
                    field.set(null, getter ? getter.getInet4(key)?.get() : (obj instanceof Inet4Address ? obj : obj.toString().toInet4Address()))
                    break
                case Inet6Address:
                    field.set(null, getter ? getter.getInet6(key)?.get() : (obj instanceof Inet6Address ? obj : obj.toString().toInet6Address()))
                    break
                case URI:
                    field.set(null, getter ? getter.getURI(key)?.get() : (obj instanceof URI ? obj : obj.toString().toURI()))
                    break
                case URL:
                    field.set(null, getter ? getter.getURL(key)?.get() : (obj instanceof URL ? obj : obj.toString().toURL()))
                    break
                case List:
                    field.set(null, getter ? getter.getList(key) : obj as List)
                    break
                case Set:
                    field.set(null, getter ? getter.getSet(key) : obj as Set)
                    break
                case Map:
                    field.set(null, getter ? getter.getMap(key) : obj as Map)
                    break
                case byte[]:
                    field.set(null, getter ? getter.getBytes(key)?.get() : obj as byte[])
                    break
                case String:
                    field.set(null, getter ? getter.get(key) : obj.toString())
                    break
                default:
                    Log.w("Unable to handle type: %s of field: %s.%s", field.type, field.declaringClass.toString(), field.name)
                    return
            }
        }
    }

    /**
     * Recreate StoredValues
     * 1. import values from annotated fields
     * 2. update values from storage (db)
     */
    void updateStoredValues() {
        if(storedValues.empty) {
            allAnnotatedFields.each {
                Field field ->
                    // Checks if the key already exists:
                    Storage storedField = storedValues.find { it.key == getKey(field) }
                    if (storedField) {
                        Log.w("Duplicated key found in code: %s in field: %s.%s", getKey(field), field.declaringClass.toString(), field.name)
                    } else {
                        storedValues << new Storage(field)
                    }
            }
            props.keys.each {
                String key ->
                    Storage storedField = storedValues.find {
                        it.key == key
                    }
                    if(storedField) {
                        storedField.updateFieldValue(props)
                        storedField.resetChange() //Initial state: without change
                    } else if(! removeIgnore.contains(props.getFullKey(key))) {
                        Log.w("Key not found in stored values: %s", props.getFullKey(key))
                    }
            }
        }
    }

    /**
     * Update database with current values
     * This method is used to monitor changes on values
     * Used in @ConfigAutoTask (thread module)
     * //TODO: Observe changes and update immediately
     */
    void update() {
        storedValues.each {
            Storage storage ->
                storage.save()
        }
    }

    /**
     * Update a single value using PropertiesGet
     * @param key
     * @param val
     * @return
     */
    boolean update(String key, Object object) {
        boolean updated = false
        updateStoredValues()
        Storage storage = storedValues.find { it.key == key }
        if(storage) {
            storage.updateFieldValueAndSave(object)
            updated = true
        } else {
            Log.w("Unable to find key: %s in storage", key)
        }
        return updated
    }

    /**
     * Check configuration for no-longer existing keys and default values stored.
     * The goal of this method is to keep the storage clean
     */
    boolean cleanseStoredValues() {
        boolean ok = true
        // If defaults match, check current configuration against code:
        try {
            Map<String, Object> keys = initialValues
            props.keys.each {
                String key ->
                    String fullKey = props.getFullKey(key)
                    Storage storage = storedValues.find { getKey(it.field) == key }
                    if(storage) {
                        if (storage.export) {
                            String value = props.get(key)
                            if (value == keys[key].toString()) {
                                Log.i("Config key: %s='%s' is the same as default. Removed.", fullKey, value)
                                props.delete(key)
                                ok = false
                            }
                        }
                    } else if(removeMissing) {
                        if(! removeIgnore.contains(fullKey)) {
                            String value = props.get(key)
                            Log.w("Config key: %s='%s' doesn't exists. Removed.", fullKey, value)
                            props.delete(key)
                            ok = false
                        }
                    } else {
                        Log.w("Key [%s] found saved in configuration which no longer exists.", fullKey)
                    }
            }
        } catch (Exception e) {
            Log.e("Unable to cleanse configuration", e)
            ok = false
        }
        return ok
    }

    /**
     * Import from properties file
     */
    void importValues(File propertiesFile = cfgFile) {
        if(propertiesFile.exists()) {
            Log.i("Importing config from %s ...", propertiesFile.name)
            Config.Props appProps = new Config.Props(propertiesFile)

            // Keys not present in configuration, will be set to default
            storedValues.each {
                Storage storage ->
                    if (appProps.exists(storage.key) && storage.export) {
                        storage.updateFieldValueAndSave(appProps)
                    }
            }
        } else {
            Log.w("Properties file doesn't exists: %s", propertiesFile.absolutePath)
        }
    }
    /**
     * Export to properties file
     */
    boolean exportValues(File propertiesFile = cfgFile) {
        if(!propertiesFile.parentFile.exists()) {
            propertiesFile.parentFile.mkdirs()
        }
        List<String> buffer = ["# suppress inspection \"UnusedProperty\" for whole file"]
        // Group all fields by class
        String currentClass = null
        storedValues.sort { it.parent.simpleName + "_" + it.key }.each {
            Storage storage ->
                if(currentClass != storage.parent.simpleName) {
                    currentClass = storage.parent.simpleName
                    Annotation classAnnotation = storage.parent.getAnnotation(AutoConfig) as AutoConfig
                    String desc = classAnnotation?.description() ?: ""
                    boolean hasExport = storedValues.find { it.parent == storage.parent && it.export }
                    if(hasExport) {
                        buffer << "##############################################################################"
                        buffer << wordWrap("# [${getBaseKey(storage.field)}] ${desc} (${currentClass})".toString())
                        buffer << "##############################################################################"
                    }
                }
                if(storage.export) {
                    ConfigProps configProps = new ConfigProps()
                    configProps.setObj("initial", storage.initial)
                    configProps.setObj("current", storage.current)

                    buffer << wordWrap("# (${storage.field.type.simpleName}) ${storage.description}".toString())
                    if(! storage.sameAsDefault) {
                        buffer << "#${storage.key}=${configProps.get("initial")}".toString()
                        buffer << "${storage.key}=${configProps.get("current")}".toString()
                    } else {
                        buffer << "#${storage.key}=${configProps.get("current")}".toString()
                    }
                    buffer << ""
                    configProps.clear()
                }
        }
        propertiesFile.text = buffer.join(SysInfo.newLine)
        Log.v("Settings file updated: %s", propertiesFile.absolutePath)
        return propertiesFile.exists()
    }
    /**
     * Split string based on width
     * @param input
     * @return
     */
    static String wordWrap(String input) {
        LinkedList<String> words = input.tokenize(" ")
                .collect { it.trim() }
                .findAll {it } as LinkedList
        List<String> lines = []
        while(! words.empty) {
            List<String> buffer = []
            while (! words.empty && buffer.join(" ").length() < columnDocWrap) {
                buffer << words.poll()
            }
            if(! words.empty && words.join(" ").length() < 10) {
                buffer.addAll(words)
                words.clear()
            }
            lines << buffer.join(" ")
        }
        return lines.join(SysInfo.newLine + "# ")
    }
    /**
     * Return changed values in configuration
     * 'changed' means values which differ from default
     * @return
     */
    Map<String, String> getChanged(boolean exportOnly = false) {
        return storedValues.findAll {
            return exportOnly ? it.export &&! it.sameAsDefault : it.changed
        }.collectEntries {
            Storage storage ->
                return [(storage.key) : storage.current.toString() ]
        }
    }

    /**
     * Drop storage
     */
    void clear() {
        props.clear()
    }
    /**
     * Call closing interface
     */
    void close() {
        exportValues()
        if(onClose) {
            onClose.call()
        }
    }
    /**
     * Convert object to boolean
     * @param obj
     * @return
     */
    static boolean getBoolean(Object obj) {
        boolean res
        switch (obj) {
            case String: res = Boolean.parseBoolean(obj.toString()); break
            default: res = obj as boolean
        }
        return res
    }
}