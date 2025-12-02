package com.intellisrc.web.service

import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic

import static com.intellisrc.web.service.WebMessage.WebMessageType.*

/**
 * Automatic wrap messages to handle Map, Collection and String messages
 * When the code auto-detects a JSON object, 'data' property will be assigned.
 * When the code auto-detects a JSON array, 'data' property will be converted into Map: [ list : [...] ]
 * In any case, the original text message will be assigned into 'text' property.
 */
@CompileStatic
class WebMessage {
    static enum WebMessageType {
        STRING, MAP, LIST
    }
    protected final Map map
    protected final String text
    protected final WebMessageType type
    WebMessage(Map data) {
        map = data
        text = ""
        type = MAP
    }
    WebMessage(Collection data) {
        map = [list: data]
        text = data
        type = LIST
    }
    WebMessage(String data) {
        boolean string = true
        Map tmpData = [:]
        WebMessageType tmpType = STRING
        if (data.startsWith("{")) {
            try {
                tmpData = JSON.decode(data) as Map
                tmpType = MAP
                string = false
            } catch (Exception ignore) {
                // Not JSON
            }
        } else if (data.startsWith("[")) {
            try {
                tmpData = [ list : JSON.decode(data) as Collection ]
                tmpType = LIST
                string = false
            } catch (Exception ignore) {
                // Not JSON
            }
        }
        if(string) {
            tmpData = [ text : data ]
            tmpType = STRING
        }
        map = tmpData
        type = tmpType
        text = data
    }
    String toString() {
        return switch (type) {
            case STRING -> text
            case MAP -> JSON.encode(map)
            case LIST -> JSON.encode(map.list)
        }

    }
    // Alias
    String getText() {
        return toString()
    }
    Map getData() {
        return map
    }
}