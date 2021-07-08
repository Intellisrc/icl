package com.intellisrc.etc.config

import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @since 2021/03/10.
 */
@CompileStatic
interface PropertiesGet {
    String get(String key, String defVal)
    boolean get(String key, boolean defVal)
    short get(String key, short defVal)
    int get(String key, int defVal)
    float get(String key, float defVal)
    double get(String key, double defVal)
    File get(String key, File defVal)
    LocalDate get(String key, LocalDate defVal)
    LocalTime get(String key, LocalTime defVal)
    LocalDateTime get(String key, LocalDateTime defVal)
    Collection<String> get(String key, Collection<String> defVal)
    Map<String,String> get(String key, Map<String,String> defVal)
    Map<String, String> getAll()
    boolean exists(String key)
}