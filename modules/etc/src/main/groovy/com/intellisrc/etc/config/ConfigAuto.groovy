package com.intellisrc.etc.config

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic
import javassist.Modifier
import org.reflections.Reflections
import org.reflections.scanners.FieldAnnotationsScanner
import org.yaml.snakeyaml.Yaml

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
    final SavedPropertiesRedis props //FIXME: allow other kind of SavedProperties (like BerkeleyDB)
    protected Set<Storage> storedValues = []
    protected String basePkg

    static class Storage {
        final Field field
        final Object initial
        Object previous
        boolean export
        boolean userFriendly

        Storage(Field field) {
            this.field = field
            this.initial = this.previous = field.get(null)
            // Check for class annotation
            Annotation classAnnotation = field.declaringClass.getAnnotation(AutoConfig)
            boolean classExport = classAnnotation ? classAnnotation.export() : true
            boolean classUserFriendly = classAnnotation ? classAnnotation.userFriendly() : false
            this.export = field.getAnnotation(AutoConfig).export() && classExport
            this.userFriendly = field.getAnnotation(AutoConfig).userFriendly() || classUserFriendly
        }

        void update() {
            previous = current
        }

        Object getCurrent() {
            return field.get(null)
        }

        String getKey() {
            return getKey(field)
        }

        boolean isChanged() {
            return previous != current
        }
    }
    /**
     * Constructor. If File (.properties) is passed, it will be compared
     * against configuration
     *
     * @param basePackage
     * @param documentationFile
     */
    ConfigAuto(String basePackage, String rootKey = SavedProperties.defaultRoot) {
        props = new SavedPropertiesRedis(rootKey)
        try {
            basePkg = basePackage
            // Verify current config:
            check()
            storedValues.each {
                try {
                    setConfig(it)
                } catch (Exception ex) {
                    Log.w("Unable to set auto configuration for field: %s (%s)", it.key, ex.message)
                }
            }
        } catch (Exception e) {
            Log.e("Unable to start AutoConfig", e)
        }
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
    List<String> getAllKeys() {
        return allAnnotatedFields.collect { getKey(it) }
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
        String classRoot = field.declaringClass.getAnnotation(AutoConfig)?.root() ?: props.defaultRoot
        String fieldRoot = field.getAnnotation(AutoConfig)?.root() ?: props.defaultRoot
        if(props.propKey != classRoot || props.propKey != fieldRoot) {
            Log.v("Field [%s] was skipped as roots: [%s,%s] don't match current: %s", classRoot, fieldRoot, props.propKey)
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
     * Get Key in configuration
     * @param field
     * @return
     */
    static protected String getKey(final Field field) {
        // Get key from field. If its not present, use field name
        String key = field.getAnnotation(AutoConfig).key() ?: field.name.toLowerCase()
        return getBaseKey(field) + "." + key
    }

    /**
     * Automatic assign props default values to fields
     * @param storage
     */
    void setConfig(final Storage storage) {
        Field field = storage.field
        String key = storage.key
        if (prepare(field)) {
            switch (field.type) {
                case boolean: case Boolean:
                    field.setBoolean(null, props.get(key, field.getBoolean(null)))
                    break
                case short: case Short:
                    field.setShort(null, props.get(key, field.getShort(null)))
                    break
                case int: case Integer:
                    field.setInt(null, props.get(key, field.getInt(null)))
                    break
                case long: case Long: case BigInteger:
                    field.setLong(null, props.get(key, field.getLong(null)))
                    break
                case float: case Float:
                    field.setFloat(null, props.get(key, field.getFloat(null)))
                    break
                case double: case Double: case BigDecimal:
                    field.setDouble(null, props.get(key, field.getDouble(null)))
                    break
                case File:
                    field.set(null, props.get(key, field.get(null) as File))
                    break
                case LocalDateTime:
                    field.set(null, props.get(key, field.get(null) as LocalDateTime))
                    break
                case LocalDate:
                    field.set(null, props.get(key, field.get(null) as LocalDate))
                    break
                case LocalTime:
                    field.set(null, props.get(key, field.get(null) as LocalTime))
                    break
                case Inet4Address:
                    field.set(null, props.get(key, field.get(null) as Inet4Address))
                    break
                case List:
                    field.set(null, props.get(key, field.get(null) as List))
                    break
                case Map:
                    field.set(null, props.get(key, field.get(null) as Map))
                    break
                case String:
                    field.set(null, props.get(key, field.get(null).toString()))
                    break
                default:
                    Log.w("Unable to handle type: %s of field: %s.%s", field.type, field.declaringClass.toString(), field.name)
                    return
            }
            update()
        }
    }

    /**
     * Set field value to a new one
     * @param storage
     */
    void setValueFromString(final Storage storage, String value) {
        Field field = storage.field
        if (prepare(field)) {
            switch (field.type) {
                case boolean: case Boolean:
                    field.setBoolean(null, value == "true")
                    break
                case short: case Short:
                    field.setShort(null, Short.parseShort(value))
                    break
                case int: case Integer:
                    field.setInt(null, Integer.parseInt(value))
                    break
                case long: case Long: case BigInteger:
                    field.setLong(null, Long.parseLong(value))
                    break
                case float: case Float:
                    field.setFloat(null, Float.parseFloat(value))
                    break
                case double: case Double: case BigDecimal:
                    field.setDouble(null, Double.parseDouble(value))
                    break
                case File:
                    field.set(null, SysInfo.getFile(value))
                    break
                case LocalDateTime:
                    field.set(null, value.toDateTime())
                    break
                case LocalDate:
                    field.set(null, value.toDate())
                    break
                case LocalTime:
                    field.set(null, value.toTime())
                    break
                case Inet4Address:
                    field.set(null, value.toInet4Address())
                    break
                case Inet6Address:
                    field.set(null, value.toInet6Address())
                    break
                case Collection:
                    field.set(null, new Yaml().load(value) as List)
                    break
                case Map:
                    field.set(null, new Yaml().load(value) as Map)
                    break
                case String:
                    field.set(null, value)
                    break
                default:
                    Log.w("Unable to handle type: %s of field: %s.%s", field.type, field.declaringClass.toString(), field.name)
                    return
            }
        }
    }

    /**
     * Recreate StoredValues
     */
    void updateStoredValues() {
        if(storedValues.empty) {
            allAnnotatedFields.each {
                Field field ->
                    if (prepare(field)) {
                        // Checks if the key already exists:
                        Storage nameSake = storedValues.find { it.key == getKey(field) }
                        if (nameSake) {
                            Log.w("Duplicated key found in code: %s in field: %s.%s", getKey(field), field.declaringClass.toString(), field.name)
                        } else {
                            storedValues << new Storage(field)
                        }
                    }
            }
        }
    }

    /**
     * Update database with current values
     */
    void update() {
        updateStoredValues()
        storedValues.each {
            Storage storage ->
                save(storage)
        }
    }

    /**
     * Save storage value if changed
     * @param storage
     * @return
     */
    boolean save(Storage storage) {
        boolean updated = true
        Field field = storage.field
        String key = storage.key
        if (prepare(field)) {
            if (storage.changed) {
                Log.d("Config value changed: %s, Prev: %s, Now: %s", key, storage.previous, storage.current)
                switch (field.type) {
                    case boolean: case Boolean:
                        props.set(key, field.getBoolean(null))
                        break
                    case int: case Integer:
                        props.set(key, field.getInt(null))
                        break
                    case long: case Long:
                        props.set(key, field.getLong(null))
                        break
                    case float: case Float:
                        props.set(key, field.getFloat(null))
                        break
                    case double: case Double:
                        props.set(key, field.getDouble(null))
                        break
                    case File:
                        props.set(key, field.get(null) as File)
                        break
                    case LocalDateTime:
                        props.set(key, field.get(null) as LocalDateTime)
                        break
                    case LocalDate:
                        props.set(key, field.get(null) as LocalDate)
                        break
                    case LocalTime:
                        props.set(key, field.get(null) as LocalTime)
                        break
                    case Inet4Address:
                        props.set(key, field.get(null) as Inet4Address)
                        break
                    case List:
                        props.set(key, field.get(null) as List)
                        break
                    case Map:
                        props.set(key, field.get(null) as Map)
                        break
                    case String:
                        props.set(key, field.get(null).toString())
                        break
                    default:
                        Log.w("Unable to handle type: %s of field: %s.%s", field.type, field.declaringClass.toString(), field.name)
                        updated = false
                }
                update()
            } else {
                updated = false
            }
        }
        return updated
    }

    /**
     * Update a single value
     * @param key
     * @param val
     * @return
     */
    boolean update(String key, String val) {
        boolean updated = false
        updateStoredValues()
        Storage storage = storedValues.find { it.key == key }
        if(storage) {
            setValueFromString(storage, val)
            updated = save(storage)
        }
        return updated
    }

    /**
     * Update method for Map
     * @param key
     * @param val
     * @return
     */
    boolean update(String key, Map val) {
        boolean updated = false
        updateStoredValues()
        Storage storage = storedValues.find { it.key == key && it.field.type instanceof Map }
        if(storage) {
            storage.field.set(null, val)
            updated = save(storage)
        }
        return updated
    }

    /**
     * Check if all keys are explained in documentation
     * @param documentationFile (.properties file)
     * @return
     */
    boolean checkDocumentation(File documentationFile) {
        boolean ok = true
        if (documentationFile.exists()) {
            try {
                // Update Stored Values with default ones
                updateStoredValues()
                Config.Props defaults = new Config.Props(documentationFile) //defaults values are String
                List<String> missingKeysInFile = storedValues.sort { it.key }.findAll {
                    Storage storage ->
                        boolean export = storage.export
                        boolean documented = true
                        if(export) {
                            documented = defaults.hasKey(storage.key)
                            if (documented) {
                                boolean same
                                //noinspection GroovyFallthrough
                                switch (storage.field.type) {
                                    case boolean: case Boolean:
                                        same = (storage.initial as boolean) == defaults.getBool(storage.key)
                                        break
                                    case short: case Short:
                                    case int: case Integer:
                                    case long: case Long: case BigInteger:
                                    case float: case Float:
                                    case double: case Double: case BigDecimal:
                                        same = (storage.initial as double) == defaults.getDbl(storage.key)
                                        break
                                    case File:
                                        File keyFile = (storage.initial as File)
                                        File defFile = defaults.getFile(storage.key)
                                        same = keyFile && defFile ? keyFile.name == defFile.name : keyFile == defFile
                                        break
                                    case Inet4Address:
                                        same = (storage.initial as Inet4Address).hostAddress == defaults.get(storage.key).toInet4Address().hostAddress
                                        break
                                    case Inet6Address:
                                        same = (storage.initial as Inet6Address).hostAddress == defaults.get(storage.key).toInet6Address().hostAddress
                                        break
                                    case Collection:
                                        same = (storage.initial as List).join(",") == defaults.getList(storage.key).join(",")
                                        break
                                    case Map:
                                        Log.w("Map is not supported yet in config.properties, it will be compared as string")
                                    default:
                                        same = storage.initial.toString() == defaults.get(storage.key)
                                }
                                if (!same) {
                                    Log.w("Documentation has a wrong default value: %s in %s, it should be: %s", defaults.get(storage.key), storage.key, storage.initial)
                                    ok = false
                                }
                            }
                        }
                        return !documented
                }.collect { it.key }.toList()
                missingKeysInFile.each {
                    Log.w("Missing documentation of key: %s", it)
                    ok = false
                }
                // Now check which keys we have in documentation that are no longer in code
                defaults.props.sort { it.key.toString() }.each {
                    Map.Entry entry ->
                        if(! storedValues.find { it.key == entry.key.toString() }) {
                            // Only report those in which we have other key with same class key:
                            if(storedValues.find { it.key.startsWith(entry.key.toString().tokenize(".").first()) }) {
                                Log.w("Documentation contains key : %s which seems not to be in code", entry.key.toString())
                            } else {
                                Log.d("Documentation contains key : %s which is not AutoConfig", entry.key.toString())
                            }
                        }
                }
            } catch (Exception e) {
                Log.e("Unable to verify documentation:", e)
                ok = false
            }
        } else {
            Log.w("Default config doesn't exist: %s", documentationFile.absolutePath)
        }
        return ok
    }
    /**
     * Check configuration
     */
    boolean check() {
        boolean ok = true
        // If defaults match, check current configuration against code:
        try {
            updateStoredValues()
            Map<String, Object> keys = initialValues
            props.all.each {
                Map.Entry<String, String> entry ->
                    boolean export = storedValues.find { getKey(it.field) == entry.key }.export
                    if(export) {
                        if (!keys.containsKey(entry.key)) {
                            Log.w("Config key: %s='%s' doesn't exists. Removed.", entry.key, entry.value)
                            props.delete(entry.key)
                            ok = false
                        } else if (entry.value == keys[entry.key].toString()) {
                            Log.i("Config key: %s='%s' is the same as default. Removed.", entry.key, entry.value)
                            props.delete(entry.key)
                            ok = false
                        }
                    }
            }
        } catch (Exception e) {
            Log.e("Unable to check configuration", e)
            ok = false
        }
        if (ok) {
            Log.s("Configuration is OK")
        }
        return ok
    }

    /**
     * Import from properties file
     */
    void 'import'(File cfgFile = Config.global.configFile) {
        Log.i("Importing config from %s ...", cfgFile.name)
        Config.Props sysProps = new Config.Props(cfgFile)
        sysProps.props.each {
            update(it.key.toString(), it.value.toString())
        }
        // Keys not present in configuration, will be set to default
        props.all.each {
            Map.Entry<String,String> item ->
            if(!sysProps.hasKey(item.key)) {
                Storage st = storedValues.find { it.key == item.key }
                if(st && st.export) {
                    String defaultVal = st.initial.toString()
                    Log.d("Key removed as it is not present in config: %s = %s (default: %s)", item.key, item.value, defaultVal)
                    props.delete(item.key)
                }
            }
        }
        check()
    }
    /**
     * Export to properties file
     */
    void export(File cfgFile = Config.global.configFile) {
        if (cfgFile.exists()) {
            cfgFile.delete()
        }
        Config.Props sysProps = new Config.Props(cfgFile)
        props.all.each {
            Map.Entry<String,String> prop ->
                boolean export = storedValues.find { it.key == prop.key }.export
                if(export) {
                    sysProps.set(prop.key, prop.value) //FIXME: I guess it won't work for Map and List
                }
        }
        check()
        // Sort it: (as Config.set by nature will not keep order)
        cfgFile.text = cfgFile.readLines().sort().join("\n")
        Log.i("Configuration exported to: %s", cfgFile.absolutePath)
    }
}