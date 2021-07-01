package com.intellisrc.etc.config

import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @since 2019/12/09.
 */
@CompileStatic
abstract class SavedProperties implements PropertiesGet, PropertiesSet {
    static final String defaultRoot = AutoConfig.getMethod("root").defaultValue.toString() //Same as AutoConfig default
    final String propKey
    SavedProperties(String propertiesKey = defaultRoot) {
        propKey = propertiesKey
    }
    abstract String get(String key, String defVal)
    abstract void set(String key, String value)
    abstract void set(String key, Collection<String> list)
    abstract void set(String key, Map<String, String> map)
    abstract Collection<String> get(String key, Collection<String> defVal)
    abstract Map<String, String> get(String key, Map<String, String> defVal)
    abstract boolean exists(String key)
    abstract Map<String, String> getAll()
    abstract boolean delete(String key)
    abstract boolean clear()

    //---------------------------------------- SET ----------------------------------------
    void set(String key, short val) {
        set(key, val.toString())
    }
    void set(String key, int val) {
        set(key, val.toString())
    }
    void set(String key, float val) {
        set(key, val.toString())
    }
    void set(String key, double val) {
        set(key, val.toString())
    }
    void set(String key, boolean val) {
        set(key, val ? "true" : "false")
    }
    void set(String key, File val) {
        set(key, val.absolutePath)
    }
    void set(String key, LocalDate time) {
        set(key, time.YMD)
    }
    void set(String key, LocalTime time) {
        set(key, time.HHmmss)
    }
    void set(String key, LocalDateTime time) {
        set(key, time.YMDHms)
    }
    void set(String key, Inet4Address ip) {
        set(key, ip.hostAddress)
    }
    void set(String key, Inet6Address ip) {
        set(key, ip.hostAddress)
    }
    //---------------------------------------- GET ----------------------------------------
    boolean get(String key, boolean defVal) {
        return get(key, defVal ? "true" : "false") == "true"
    }
    short get(String key, short defVal) {
        return get(key, defVal.toString()) as short
    }
    int get(String key, int defVal) {
        return get(key, defVal.toString()) as int
    }
    long get(String key, long defVal) {
        return get(key, defVal.toString()) as long
    }
    float get(String key, float defVal) {
        return get(key, defVal.toString()) as float
    }
    double get(String key, double defVal) {
        return get(key, defVal.toString()) as double
    }
    File get(String key, File defVal) {
        return SysInfo.getFile(get(key, defVal.absolutePath))
    }
    LocalDate get(String key, LocalDate defVal) {
        return get(key, defVal.YMD)?.toString()?.toDate()
    }
    LocalTime get(String key, LocalTime defVal) {
        return get(key, defVal.HHmmss).toString()?.toTime()
    }
    LocalDateTime get(String key, LocalDateTime defVal) {
        return get(key, defVal.YMDHmsS).toString()?.toDateTime()
    }
    Inet4Address get(String key, Inet4Address defVal) {
        return get(key, defVal.hostAddress).toString()?.toInet4Address()
    }
    Inet4Address get(String key, Inet6Address defVal) {
        return get(key, defVal.hostAddress).toString()?.toInet4Address()
    }
}
