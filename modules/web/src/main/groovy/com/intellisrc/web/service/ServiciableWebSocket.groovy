package com.intellisrc.web.service

import groovy.transform.CompileStatic

/**
 * Basic requirements to implement a WebSocket service
 * @see WebSocketService for a simple implementation of this interface.
 * @since 2023/07/25.
 */
@CompileStatic
interface ServiciableWebSocket extends Serviciable {
    @Override
    abstract String getPath()
    abstract WebSocketBroadcastService getWebSocketService()
}