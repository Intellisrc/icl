package jp.sharelock.web

import com.google.gson.Gson
import groovy.transform.CompileStatic

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
        Date toDate() {
            return new Gson().fromJson(json, Date.class)
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
    static String encode(Object object) {
        return new Gson().toJson(object)
    }
    static JsonData decode(String string) {
        return new JsonData(string)
    }
}