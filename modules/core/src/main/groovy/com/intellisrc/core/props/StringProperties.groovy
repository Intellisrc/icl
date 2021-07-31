package com.intellisrc.core.props

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic

import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * This class is a partial implementation
 * for those classes which stores values
 * as String (Config, BerkeleyDB, etc)
 *
 */
@CompileStatic
abstract class StringProperties implements PrefixedPropertiesRW {
    // Groovy BUG?: IMO, these 'abstract' methods shouldn't be needed here
    // as they are required by PropertiesGet and PropertiesSet. However
    // if they are not specified, this code fails to compile:
    abstract Set<String> getKeys()
    abstract String get(String key, String val)
    abstract boolean set(String key, String val)
    abstract boolean set(String key, Collection list)
    abstract boolean set(String key, Map map)
    abstract boolean exists(String key)
    abstract boolean delete(String key)
    abstract boolean clear()

    StringProperties(String keyPrefix = "", String prefixSeparator = ".") {
        this.prefix = keyPrefix
        this.prefixSeparator = prefixSeparator
    }
    /**
     * Get value as short
     * @param key
     * @param defVal
     * @return
     */
    @Override
    short get(String key, short defVal) {
        return exists(key) ? Short.parseShort(get(key,"0")) : defVal
    }
    /**
     * Get value as int
     * @param key
     * @return
     */
    @Override
    int get(String key, int defVal) {
        return exists(key) ? Integer.parseInt(get(key,"0")) : defVal
    }
    /**
     * Get value as long
     * @param key
     * @param defVal
     * @return
     */
    @Override
    long get(String key, long defVal) {
        return exists(key) ? Long.parseLong(get(key,"0")) : defVal
    }
    /**
     * Get value as double
     * @param key
     * @return
     */
    @Override
    float get(String key, float defVal) {
        return exists(key) ? Float.parseFloat(get(key,"0")) : defVal
    }
    /**
     * Get value as double
     * @param key
     * @return
     */
    @Override
    double get(String key, double defVal) {
        return exists(key) ? Double.parseDouble(get(key,"0")) : defVal
    }
    /**
     * Get value as BigInteger
     * @param key
     * @param defVal
     * @return
     */
    @Override
    BigInteger get(String key, BigInteger defVal) {
        return exists(key) ? (get(key,"0"))  as BigInteger : defVal
    }
    /**
     * Get value as BigDecimal
     * @param key
     * @param defVal
     * @return
     */
    @Override
    BigDecimal get(String key, BigDecimal defVal) {
        return exists(key) ? (get(key,"0")) as BigDecimal : defVal
    }

    @Override
    List get(String key, List defVal) {
        return null
    }

    @Override
    Map get(String key, Map defVal) {
        return null
    }
/**
     * Get value as boolean
     * @param key
     * @return
     */
    @Override
    boolean get(String key, boolean defVal) {
        boolean val = defVal
        if(exists(key)) {
            val = get(key, "false") == "true"
        }
        return val
    }
    /**
     * Get File using default path
     * @param key
     * @return
     */
    File getFile(String key, String defPath) {
        return getFile(key, SysInfo.getFile(defPath))
    }
    /**
     * Get File using default File object
     * @param key
     * @param defFile
     * @return
     */
    File getFile(String key, File defFile) {
        Optional<File> fileOpt = getFile(key)
        return fileOpt.present ? fileOpt.get() : defFile
    }
    /**
     * Get File (optional)
     * @param key
     * @return
     */
    @Override
    Optional<File> getFile(String key) {
        File file = null
        if(exists(key)) {
            try {
                file = SysInfo.getFile(get(key))
            } catch (Exception ignore) {
                Log.w("Unable to parse value as File of key: %s", key)
            }
        }
        return Optional.ofNullable(file)
    }
    /**
     * Get value as URI
     * @param key
     * @return
     */
    @Override
    Optional<URI> getURI(String key) {
        URI uri = null
        try {
            uri = exists(key) ? get(key).toURI() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to URI", key)
        }
        return Optional.ofNullable(uri)
    }
    /**
     * Get value as URL
     * @param key
     * @return
     */
    @Override
    Optional<URL> getURL(String key) {
        URL url = null
        try {
            url = exists(key) ? get(key).toURL() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to URL", key)
        }
        return Optional.ofNullable(url)
    }
    /**
     * Get value as LocalTime
     * @param key
     * @return
     */
    @Override
    Optional<LocalTime> getTime(String key) {
        LocalTime time = null
        try {
            time = exists(key) ? get(key).toTime() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to LocalTime", key)
        }
        return Optional.ofNullable(time)
    }
    /**
     * Get value as LocalDate
     * @param key
     * @return
     */
    @Override
    Optional<LocalDate> getDate(String key) {
        LocalDate date = null
        try {
            date = exists(key) ? get(key).toDate() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to LocalDate", key)
        }
        return Optional.ofNullable(date)
    }
    /**
     * Get value as LocalDateTime
     * @param key
     * @return
     */
    @Override
    Optional<LocalDateTime> getDateTime(String key) {
        LocalDateTime time = null
        try {
            time = exists(key) ? get(key).toDateTime() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to LocalDateTime", key)
        }
        return Optional.ofNullable(time)
    }
    /**
     * Get value as Inet4Address
     * @param key
     * @return
     */
    @Override
    Optional<Inet4Address> getInet4(String key) {
        Inet4Address ip = null
        try {
            ip = exists(key) ? get(key).toInet4Address() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to Inet4Address", key)
        }
        return Optional.ofNullable(ip)
    }
    /**
     * Get value as Inet6Address
     * @param key
     * @return
     */
    @Override
    Optional<Inet6Address> getInet6(String key) {
        Inet6Address ip = null
        try {
            ip = exists(key) ? get(key).toInet6Address() : null
        } catch(Exception ignore) {
            Log.w("value of: %s can not be parsed to Inet6Address", key)
        }
        return Optional.ofNullable(ip)
    }
    /**
     * Get value as byte array
     * @param key
     * @return
     */
    @Override
    Optional<byte[]> getBytes(String key) {
        byte[] bytes = null
        if(exists(key)) {
            try {
                bytes = Base64.decoder.decode(get(key))
            } catch(Exception ignore) {
                Log.w("value of: %s can not be parsed to byte array", key)
            }
        }
        return Optional.ofNullable(bytes)
    }

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

    /**
     * Special method to set value from field automatically
     * @param key
     * @param field
     * @return
     */
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
