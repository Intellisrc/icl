package com.intellisrc.web

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import groovy.transform.CompileStatic

import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * @since 17/12/13.
 */
@CompileStatic
class JSON {
    static class JsonData {
        private final String json
        JsonData(final String json) {
            this.json = json
        }
        String toString() {
            return new Gson().fromJson(json, String.class)
        }
        int toInteger() {
            return new Gson().fromJson(json, Integer.class)
        }
        Double toDouble() {
            return new Gson().fromJson(json, Double.class)
        }
        Long toLong() {
            return new Gson().fromJson(json, Long.class)
        }
        LocalDate toDate() {
            return new Gson().fromJson(json, LocalDate.class)
        }
        boolean toBoolean() {
            return new Gson().fromJson(json, Boolean.class)
        }
        Map toMap() {
            return new Gson().fromJson(json, Map.class)
        }
        List toList() {
            return new Gson().fromJson(json, List.class)
        }
    }
    static class LocalDateAdapter implements JsonSerializer<LocalDate> {
        JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.YMD)
        }
    }
    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime> {
        JsonElement serialize(LocalDateTime date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.YMDHms)
        }
    }
    static class LocalTimeAdapter implements JsonSerializer<LocalTime> {
        JsonElement serialize(LocalTime time, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(time.HHmmss)
        }
    }
    static String encode(Object object, boolean pretty = false) {
        def gbuild = new GsonBuilder()
        gbuild.with {
            if(pretty) {
                setPrettyPrinting()
            }
            registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
        }
        return gbuild.create().toJson(object)
    }
    static JsonData decode(String string) {
        return new JsonData(string)
    }
}