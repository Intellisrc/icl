package jp.sharelock.web

import com.google.gson.Gson
/**
 * @since 17/12/13.
 */
class JSON {
    static String toString(Object object) {
        return new Gson().toJson(object)
    }
    static Map toMap(String json) {
        return new Gson().fromJson(json, Map.class)
    }
    static List toList(String json) {
        return new Gson().fromJson(json, List.class)
    }
}