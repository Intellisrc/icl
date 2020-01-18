package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * @since 17/04/19.
 */
@CompileStatic
interface ServiciableWebSocket extends Serviciable {
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
        WSMessage(List<String> toClients, Map jsonObj) {
            to = toClients
            this.jsonObj = jsonObj
        }
    }
    abstract boolean getReplaceOnDuplicate()
    abstract void setBroadCaster(WebSocketService.MsgBroadCaster msgBroadCaster)
    abstract String getUserID(Map<String, List<String>> params, InetAddress remoteIP)
    abstract WSMessage onConnect(Session session)
    abstract WSMessage onDisconnect(Session session, int statusCode, String reason)
    abstract WSMessage onMessage(Session session, String message)
    abstract void onClientsChange(List<String> list)
    abstract void onError(Session session, String message)
}
