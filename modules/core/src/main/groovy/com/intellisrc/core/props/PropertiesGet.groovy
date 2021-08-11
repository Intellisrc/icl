package com.intellisrc.core.props

import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * This interface is to standardize the way
 * to get most common types from any source
 * and protect against NULL values
 * @see PropertiesSet for the contrapart
 *
 * @since 2021/03/10.
 */
@CompileStatic
trait PropertiesGet {
    abstract boolean get(String key, boolean defVal)
    abstract String get(String key, String defVal)
    abstract short get(String key, short defVal)
    abstract int get(String key, int defVal)
    abstract long get(String key, long defVal)
    abstract float get(String key, float defVal)
    abstract double get(String key, double defVal)
    abstract BigInteger get(String key, BigInteger defVal)
    abstract BigDecimal get(String key, BigDecimal defVal)
    abstract List get(String key, List defVal)
    abstract Map get(String key, Map defVal)
    abstract File getFile(String key, String defVal)
    abstract File getFile(String key, File defVal)
    abstract File get(String key, File defVal)
    abstract Enum get(String key, Enum defVal)

    // Simple implementation to return Set instead of List
    Set get(String key, Set defVal) {
        return get(key, defVal.toList()).toSet()
    }

    // The following are aliases implementation without default values:
    boolean getBool(String key)     { get(key, false) }
    String get(String key)          { get(key, "") }
    short getShort(String key)      { get(key, 0 as short) }
    int getInt(String key)          { get(key, 0) }
    long getLong(String key)        { get(key, 0L) }
    float getFloat(String key)      { get(key, 0f) }
    double getDbl(String key)       { get(key, 0d) }
    BigInteger getBigInt(String key){ get(key, 0 as BigInteger) }
    BigDecimal getBigDec(String key){ get(key, 0 as BigDecimal) }
    List getList(String key)        { get(key, [] as List) }
    Set getSet(String key)          { get(key, [] as Set) }
    Map getMap(String key)          { get(key, [:]) }

    // The following objects have no default value:
    abstract Optional<byte[]> getBytes(String key)
    abstract Optional<Inet4Address> getInet4(String key)
    abstract Optional<Inet6Address> getInet6(String key)
    abstract Optional<File> getFile(String key)
    abstract Optional<URI> getURI(String key)
    abstract Optional<URL> getURL(String key)
    abstract Optional<LocalTime> getTime(String key)
    abstract Optional<LocalDate> getDate(String key)
    abstract Optional<LocalDateTime> getDateTime(String key)
    abstract Optional<Enum> getEnum(String key, Class<Enum> type)

    abstract boolean exists(String key)
    abstract Set<String> getKeys()
}