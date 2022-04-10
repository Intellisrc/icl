package com.intellisrc.core.props

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic

import java.lang.reflect.Field
import java.time.*

/**
 * This class is a partial implementation
 * for those classes which stores values
 * as String (Config, BerkeleyDB, etc)
 *
 */
@CompileStatic
trait StringPropertiesSet implements PropertiesSet {
    /**
     * Set value in properties for Objects other
     * than the supported ones.
     *
     * NOTE: this method should not be overrode nor
     * called from set(String, String)
     *
     * @param key
     * @param value
     */
    final boolean setObj(String key, Object value) {
        boolean ok
        if(value == null) {
            ok = delete(key)
        } else {
            //noinspection GroovyFallthrough
            switch (value) {
                case String:
                case Number:
                case URI:
                                    ok = set(key, value.toString());        break
                case InetAddress:   ok = set(key, value as InetAddress);    break
                case File:          ok = set(key, value as File);           break
                case URL:           ok = set(key, value as URL);            break
                case LocalTime:     ok = set(key, value as LocalTime);      break
                case LocalDate:     ok = set(key, value as LocalDate);      break
                case LocalDateTime: ok = set(key, value as LocalDateTime);  break
                case Collection:    ok = set(key, value as Collection);     break
                case Map:           ok = set(key, value as Map);            break
                case Enum:          ok = set(key, value as Enum);           break
                case byte[]:        ok = set(key, value as byte[]);         break
                default:
                    ok = set(key, value.toString())
            }
        }
        return ok
    }

    // Default implementation for several types:
    @Override
    boolean set(String key, boolean val)        { setObj(key, val) }
    @Override
    boolean set(String key, Number val)         { setObj(key, val) }
    @Override
    boolean set(String key, InetAddress val)    { setObj(key, val.hostAddress) }
    @Override
    boolean set(String key, File val)           {
        return setObj(key, val.absolutePath.replace(SysInfo.userDir.absolutePath + File.separator, "") +
                (val.directory ? File.separator : ""))
    }
    @Override
    boolean set(String key, URI val)            { setObj(key, val) }
    @Override
    boolean set(String key, URL val)            { setObj(key, val.toExternalForm()) }
    @Override
    boolean set(String key, LocalTime val)      { setObj(key, val.HHmmss) }
    @Override
    boolean set(String key, LocalDate val)      { setObj(key, val.YMD) }
    @Override
    boolean set(String key, LocalDateTime val)  { setObj(key, val.YMDHmsS) }
    @Override
    boolean set(String key, byte[] val)         { setObj(key, Base64.encoder.encodeToString(val)) }
    @Override
    boolean set(String key, Enum val)           { setObj(key, val.toString()) }

    /**
     * Special method to set value from field automatically
     * @param key
     * @param field
     * @return
     */
    @SuppressWarnings('GroovyFallthrough')
    boolean set(String key, Field field) {
        boolean updated = false
        switch (field.type) {
            case boolean: case Boolean:
                updated = set(key, field.getBoolean(null))
                break
            case int: case Integer:
                updated = set(key, field.getInt(null))
                break
            case long: case Long:
                updated = set(key, field.getLong(null))
                break
            case float: case Float:
                updated = set(key, field.getFloat(null))
                break
            case double: case Double:
                updated = set(key, field.getDouble(null))
                break
            case BigDecimal:
                updated = set(key, field.get(null) as BigDecimal)
                break
            case File:
                updated = set(key, field.get(null) as File)
                break
            case LocalDateTime:
                updated = set(key, field.get(null) as LocalDateTime)
                break
            case LocalDate:
                updated = set(key, field.get(null) as LocalDate)
                break
            case LocalTime:
                updated = set(key, field.get(null) as LocalTime)
                break
            case Inet4Address:
                updated = set(key, field.get(null) as Inet4Address)
                break
            case Inet6Address:
                updated = set(key, field.get(null) as Inet6Address)
                break
            case URI:
                updated = set(key, field.get(null) as URI)
                break
            case URL:
                updated = set(key, field.get(null) as URL)
                break
            case List:
                updated = set(key, field.get(null) as List)
                break
            case Set:
                updated = set(key, field.get(null) as Set)
                break
            case Map:
                updated = set(key, field.get(null) as Map)
                break
            case byte[]:
                set(key, field.get(null) as byte[])
                break
            case Enum:
                set(key, field.get(null) as Enum)
                break
            case String:
                updated = set(key, field.get(null).toString())
                break
            default:
                Log.w("Unable to handle type: %s of field: %s.%s", field.type, field.declaringClass.toString(), field.name)
                updated = false
        }
        return updated
    }
}
