package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.web.service.ServiciableWebSocket
import groovy.transform.CompileStatic
import org.eclipse.jetty.websocket.api.Session

import static com.intellisrc.web.service.BroadcastService.MsgBroadCaster
import static com.intellisrc.web.service.BroadcastService.WebMessage

/**
 * @since 17/04/19.
 */
@CompileStatic
class ChatService implements ServiciableWebSocket {
    List<String> currentList = []
    boolean replaceOnDuplicate = false

    void setBroadCaster(MsgBroadCaster msgBroadCaster) {}

    String getUserID(Map<String, List<String>> params, InetAddress source) {
        String sessionID
        if(params != null && params.containsKey("user")) {
            sessionID = params.get("user").first()
        } else {
            sessionID = "Guest "+(new Random().nextInt(100) + 1)
        }
        return sessionID
    }

    WebMessage onConnect(Session session) {
        return new WebMessage(
                    user : "System",
                    message : "User : ", //FIXME+session.userID+" connected",
                    list : currentList,
                    type : "in"
                )
    }
    WebMessage onDisconnect(Session session, int statusCode, String reason) {
        return new WebMessage(
                    user : "System",
                    message : "User : ", //FIXME+session.userID+" disconnected",
                    list : currentList,
                    type : "out"
                )
    }
    WebMessage onMessage(Session session, String message) {
        return new WebMessage(
                    user : "", //FIXME session.userID,
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
