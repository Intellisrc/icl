package com.intellisrc.web.service

import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic

/**
 * Simple class to convert data to String
 */
@CompileStatic
class WebMessage {
    protected final Map data
    protected final Class type
    WebMessage(Map data) {
        this.data = data
        type = Map
    }
    WebMessage(Collection data) {
        this.data = [ _data_ : data ]
        type = Collection
    }
    WebMessage(String data) {
        this.data = [ _data_ : data ]
        type = String
    }
    String toString() {
        return JSON.encode(type != Map ? data._data_ : data)
    }
    Map getData() {
        return type != Map ? [ data : data._data_ ] : data
    }
}
