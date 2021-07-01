package com.intellisrc.etc.config

import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @since 2021/03/10.
 */
@CompileStatic
interface PropertiesSet {
    void set(String key, String value)
    void set(String key, short val)
    void set(String key, int val)
    void set(String key, float val)
    void set(String key, double val)
    void set(String key, boolean val)
    void set(String key, File val)
    void set(String key, LocalDate time)
    void set(String key, LocalTime time)
    void set(String key, LocalDateTime time)
    void set(String key, Collection<String> list)
    void set(String key, Map<String, String> map)
    boolean delete(String key)
    boolean clear()
}
