package com.intellisrc.web.service

import groovy.transform.CompileStatic

/**
 * Simple interface for classes implementing WebSockets
 * @since 2023/07/05.
 */
@CompileStatic
interface MessageHandler {
    WebMessage call(WebMessage message)
}
