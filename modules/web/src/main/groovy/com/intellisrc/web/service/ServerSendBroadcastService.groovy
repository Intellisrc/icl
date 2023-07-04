package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic

/**
 * This class simplifies sending messages to clients by implementing the
 * browsers expected format
 * @since 2023/07/04.
 */
@CompileStatic
class ServerSendBroadcastService extends BroadcastService {
    static final String newLine = "\n"
    @Override
    void sendTo(Client client, WebMessage wm, SuccessCallback onSuccess, FailCallback onFail) {
        try {
            String content = wm.data.collect {
                it.key.toString() + ":" + it.value.toString()
            }.join(newLine)
            if(content.size() <= maxSize * 1024) {
                org.eclipse.jetty.server.Response res = (client.context.response as org.eclipse.jetty.server.Response)
                wm.data.each {
                    res.writer.write(it.key.toString() + ":" + it.value.toString() + newLine)
                }
                res.writer.write(newLine) // Close last line
                res.writer.flush()
                onSuccess.call()
            } else {
                Log.w("Message was too long. %d > %d", content.size(), maxSize * 1024)
                onFail.call(new Exception("Message was too long"))
            }
        } catch(Exception e) {
            Log.w("Unable to send to client: %s : %s", e.message, e.cause)
            onFail.call(e)
        }
    }
    void sendTo(String clientId, int id, Map msg, String event = "message") {
        sendTo(clientId, [
            id   : id,
            event: event,
            data : JSON.encode(msg)
        ])
    }
    void sendTo(String clientId, Map msg, String event = "message") {
        sendTo(clientId, [
            event: event,
            data : JSON.encode(msg)
        ])
    }
    void sendTo(String clientId, int id, String msg, String event = "message") {
        sendTo(clientId, [
            id   : id,
            event: event,
            data : msg
        ])
    }
    void sendTo(String clientId, String msg, String event = "message") {
        sendTo(clientId, [
            event: event,
            data : msg
        ])
    }
    void sendAll(int id, Map msg, String event = "message") {
        broadcast([
            id   : id,
            event: event,
            data : JSON.encode(msg)
        ])
    }
    void sendAll(Map msg, String event = "message") {
        broadcast([
            event: event,
            data : JSON.encode(msg)
        ])
    }
    void sendAll(int id, String msg, String event = "message") {
        broadcast([
            id   : id,
            event: event,
            data : msg
        ])
    }
    void sendAll(String msg, String event = "message") {
        broadcast([
            event: event,
            data : msg
        ])
    }

}
