package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * @since 17/04/19.
 */
@CompileStatic
trait ServiciableWebSocket extends Serviciable {
    /**
     * Class used to specify WHOM and WHAT to send
     * Set to null if no recipient is desired
     */
    static class WSMessage {
        List<String> to = []
        Map jsonObj = [:]
        /**
         * Send to all recipients
         * @param jsonObj
         */
        WSMessage(Map jsonObj) {
            this.jsonObj = jsonObj
        }
        /**
         * Private Message
         * @param toClient
         * @param jsonObj
         */
        WSMessage(String toClient, Map jsonObj) {
            to << toClient
            this.jsonObj = jsonObj
        }
        /**
         * Send to specific clients
         * @param toClients
         * @param jsonObj
         */
        WSMessage(Collection<String> toClients, Map jsonObj) {
            to = toClients.toList()
            this.jsonObj = jsonObj
        }
    }

    // To be assigned by WebService
    WebSocketService service
    // To be assigned by WebSocketService
    WebSocketService.MsgBroadCaster broadCaster
    boolean replaceOnDuplicate = false
    /**
     * Return the user ID (required). If null or empty is returned, the session will be dropped.
     * @param params
     * @param remoteIP
     * @return
     */
    abstract String getUserID(Map<String, List<String>> params, InetAddress remoteIP)
    abstract WSMessage onConnect(Session session)
    abstract WSMessage onDisconnect(Session session, int statusCode, String reason)
    abstract WSMessage onMessage(Session session, String message)
    abstract void onClientsChange(List<String> list)
    abstract void onError(Session session, String message)
    /**
     * Sends a Message to specific client (using broadcaster)
     * @param session
     * @param response
     * @param data
     */
    boolean sendMessageTo(String userID, final Map data) {
        boolean sent = false
        broadCaster.call(new WSMessage(userID, data), {
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
    void setTimeout(long timeoutSec) {
        service?.timeout = timeoutSec
    }
}
