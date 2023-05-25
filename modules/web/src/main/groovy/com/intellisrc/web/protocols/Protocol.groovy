package com.intellisrc.web.protocols


import com.intellisrc.web.WebService
import groovy.transform.CompileStatic

/**
 * @since 2023/05/19.
 */
@CompileStatic
enum Protocol {
    HTTP, HTTP2, HTTP3
    HttpProtocol get(final WebService server) {
        HttpProtocol protocol
        switch (this) {
            case HTTP2:
                protocol = new Http2(server)
                break
            case HTTP3:
                protocol = new Http3(server)
                break
            default:
                protocol = new Http(server)
                break
        }
        protocol.init()
        return protocol
    }
}