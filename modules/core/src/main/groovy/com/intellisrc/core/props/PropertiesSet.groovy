package com.intellisrc.core.props

import groovy.transform.CompileStatic

import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * This interface provides a
 * standard way to set key,values
 * for most common types
 * @see PropertiesGet for the contrapart
 *
 * @since 2021/03/10.
 */
@CompileStatic
interface PropertiesSet {
    boolean set(String key, boolean val)
    boolean set(String key, String value)
    boolean set(String key, Number val)

    boolean set(String key, byte[] val)
    boolean set(String key, InetAddress val)
    boolean set(String key, File val)
    boolean set(String key, URI val)
    boolean set(String key, URL val)
    boolean set(String key, LocalTime time)
    boolean set(String key, LocalDate time)
    boolean set(String key, LocalDateTime time)
    boolean set(String key, Collection list)
    boolean set(String key, Map map)
    boolean set(String key, Field field)
    boolean set(String key, Enum val)

    boolean delete(String key)
    boolean clear()
}
