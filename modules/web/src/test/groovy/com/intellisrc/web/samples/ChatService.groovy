package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.web.ServiciableWebSocket
import com.intellisrc.web.Session
import com.intellisrc.web.WebSocketService
import groovy.transform.CompileStatic

/**
 * @since 17/04/19.
 */
@CompileStatic
class ChatService implements ServiciableWebSocket {
    List<String> currentList = []

    @Override
    boolean getReplaceOnDuplicate() {
        return false
    }

    void setBroadCaster(WebSocketService.MsgBroadCaster msgBroadCaster) {}

    String getUserID(Map<String, List<String>> params, InetAddress source) {
        String sessionID
        if(params != null && params.containsKey("user")) {
            sessionID = params.get("user").first()
        } else {
            sessionID = "Guest "+(Random.newInstance().nextInt(100) + 1)
        }
        return sessionID
    }

    WSMessage onConnect(Session session) {
        return new WSMessage(
                    user : "System",
                    message : "User : "+session.userID+" connected",
                    list : currentList,
                    type : "in"
                )
    }
    WSMessage onDisconnect(Session session, int statusCode, String reason) {
        return new WSMessage(
                    user : "System",
                    message : "User : "+session.userID+" disconnected",
                    list : currentList,
                    type : "out"
                )
    }
    WSMessage onMessage(Session session, String message) {
        return new WSMessage(
                    user : session.userID,
                    message : message,
                    list : currentList,
                    type : "txt"
                )
    }

    void onClientsChange(List<String> list) {
        currentList = list
    }

    void onError(Session session, String message) {
        Log.e( message)
    }

    String getPath() {
        "/chat"
    }
}
