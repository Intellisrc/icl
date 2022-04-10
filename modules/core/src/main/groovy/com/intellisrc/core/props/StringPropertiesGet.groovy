package com.intellisrc.core.props

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic
import java.time.*

/**
 * This class is a partial implementation
 * for those classes which get values
 * as String (Properties)
 *
 */
@CompileStatic
trait StringPropertiesGet implements PropertiesGet {
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
    @Override
    File getFile(String key, String defPath) {
        return getFile(key, SysInfo.getFile(defPath))
    }
    /**
     * Get File using default File object
     * @param key
     * @param defFile
     * @return
     */
    @Override
    File getFile(String key, File defFile) {
        Optional<File> fileOpt = getFile(key)
        return fileOpt.present ? fileOpt.get() : defFile
    }
    /**
     * Alternative getter to getFile()
     * @param key
     * @param defFile
     * @return
     */
    @Override
    File get(String key, File defFile) {
        return getFile(key, defFile)
    }
    /**
     * Get Enum using default Enum object
     * @param key
     * @param defEnum
     * @return
     */
    @Override
    Enum get(String key, Enum defEnum) {
        Optional<Enum> enumOpt = getEnum(key, (Class<Enum>) defEnum.class)
        return enumOpt.present ? enumOpt.get() : defEnum
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
        if(exists(key)) {
            try {
                uri = get(key).toURI()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to URI", key)
            }
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
        if(exists(key)) {
            try {
                url = get(key).toURL()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to URL", key)
            }
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
        if(exists(key)) {
            try {
                time = get(key).toTime()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to LocalTime", key)
            }
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
        if(exists(key)) {
            try {
                date = get(key).toDate()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to LocalDate", key)
            }
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
        if(exists(key)) {
            try {
                time = get(key).toDateTime()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to LocalDateTime", key)
            }
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
        if(exists(key)) {
            try {
                ip = get(key).toInet4Address()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to Inet4Address", key)
            }
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
        if(exists(key)) {
            try {
                ip = get(key).toInet6Address()
            } catch (Exception ignore) {
                Log.w("value of: %s can not be parsed to Inet6Address", key)
            }
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
     * Get value as enum
     * @param key
     * @return
     */
    @Override
    Optional<Enum> getEnum(String key, Class<Enum> type) {
        Enum e = null
        if(exists(key)) {
            try {
                e = Enum.valueOf(type, get(key).toUpperCase())
            } catch(Exception ignore) {
                Log.w("value of: %s can not be converted to Enum", key)
            }
        }
        return Optional.ofNullable(e)
    }
    Optional<Enum> get(String key, Class<Enum> type) {
        return getEnum(key, type)
    }
}
