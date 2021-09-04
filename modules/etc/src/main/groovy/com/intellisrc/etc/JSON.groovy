package com.intellisrc.etc

import com.intellisrc.core.Log
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * JSON wrapper around Groovy Json classes
 * @since 17/12/13.
 */
@CompileStatic
class JSON {
    /**
     * Decode a JSON string into an object
     * @param json
     * @param largeSize : recommended for documents larger than 2MB
     * @return
     */
    static <T> T decode(String json, boolean largeSize = false) {
        T ret = null
        try {
            ret = (T) new JsonSlurper(type: largeSize ? JsonParserType.CHARACTER_SOURCE : JsonParserType.LAX).parseText(json)
        } catch(IllegalArgumentException e1) {
            Log.e("Json string was empty or null", e1)
        } catch(Exception e2) {
            Log.e("Json string can not be decoded", e2)
        }
        return ret
    }
    /**
     * Encode an object into a JSON string
     * @param object
     * @param pretty
     * @return
     *
     * NOTE: if JsonBuilder generates a stackOverflow exception
     * is likely to be that an object inside the "object" to be
     * converted is not in the list inside `DefaultJsonGenerator.writeObject`
     * to solve it, add the converter here.
     */
    static String encode(Object object, boolean pretty = false) {
        JsonBuilder jb = new JsonBuilder(object , new JsonGenerator.Options()
                .addConverter(LocalDateTime, {it.YMDHms })
                .addConverter(LocalDate, {it.YMD })
                .addConverter(LocalTime, {it.HHmmss })
                .addConverter(InetAddress, {it.hostAddress })
                .addConverter(File, {it.absolutePath })
                .addConverter(URI, {it.toString() })
                .build())
        return pretty ? jb.toPrettyString() : jb.toString()
    }
}