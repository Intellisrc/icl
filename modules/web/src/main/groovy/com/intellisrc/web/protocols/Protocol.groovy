package com.intellisrc.web.protocols

import groovy.transform.CompileStatic

/**
 * @since 2023/05/19.
 */
@CompileStatic
enum Protocol {
    HTTP, HTTP2, HTTP3
    Protocolable init() {
        Protocolable protocolable
        switch (this) {
            case HTTP2:
                protocolable = new Http2()
                break
            case HTTP3:
                protocolable = new Http3()
                break
            default:
                protocolable = new Http()
                break
        }
        return protocolable
    }
}