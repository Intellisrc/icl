package com.intellisrc.web.service

import com.intellisrc.web.WebSocketService
import groovy.transform.CompileStatic

import static com.intellisrc.web.service.BroadcastService.MsgBroadCaster
import static com.intellisrc.web.service.BroadcastService.WebMessage

/**
 * @since 17/04/19.
 */
@CompileStatic
trait ServiciableWebSocket extends Serviciable {

    // To be assigned by WebService
    WebSocketService service
    // To be assigned by WebSocketService
    MsgBroadCaster broadCaster
    // Set to true if same ID should replace previous session
    boolean replaceOnDuplicate = false
    /**
     * Sends a Message to specific client (using broadcaster)
     * @param session
     * @param response
     * @param data
     */
    boolean sendMessageTo(String userID, final Map data) {
        boolean sent = false
        broadCaster.call(new WebMessage(userID, data), {
            // On success
            sent = true
        }, {
            // On failure
            sent = false
        })
        return sent
    }
    /**
     * Set Max Size for messages (binary or text) in KB
     * @param maxSizeKB
     */
    void setMaxSize(int maxSizeKB) {
        service?.maxSize = maxSizeKB
    }
    /**
     * Set Timeout in seconds
     * @param timeoutSec
     */
    void setTimeout(int timeoutSec) {
        service?.timeout = timeoutSec
    }
}
