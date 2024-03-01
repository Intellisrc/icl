package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Response as JettyResponse

/**
 * This class simplifies sending messages to clients by implementing the
 * browsers expected format
 * @since 2023/07/04.
 */
@CompileStatic
class ServerSendBroadcastService extends HttpServlet implements BroadcastService {
    static final String newLine = "\n"
    int maxSize = Config.any.get("web.sse.max.size", 64) // KB

    /**
     * Perform GET action
     * @param req
     * @param resp
     * @throws jakarta.servlet.ServletException
     * @throws IOException
     */
    @Override
    void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Log.i("Client connected: %s", req.remoteUser) //TODO: check
        EventClient client = new EventClient(req, identifier.call(new Request(req)), timeout, maxSize)
        clientList << client
        onClientConnect.call(client)
        onClientListUpdated.call(clientList.toList())
    }
    @Override
    void sendTo(EventClient client, WebMessage wm, SuccessCallback onSuccess, FailCallback onFail) {
        try {
            String content = wm.data.collect {
                it.key.toString() + ":" + it.value.toString()
            }.join(newLine)
            if(content.size() <= maxSize * 1024) {
                if(client.context) {
                    JettyResponse res = (client.context.response as JettyResponse)
                    wm.data.each {
                        res.writer.write(it.key.toString() + ":" + it.value.toString() + newLine)
                    }
                    res.writer.write(newLine) // Close last line
                    res.writer.flush()
                    onSuccess?.call()
                } else {
                    Log.w("Context was empty")
                    onFail?.call(new Exception("Context was empty"))
                }
            } else {
                Log.w("Message was too long. %d > %d", content.size(), maxSize * 1024)
                onFail?.call(new Exception("Message was too long"))
            }
        } catch(Exception e) {
            Log.w("Unable to send to client: %s", e)
            onFail?.call(e)
        }
    }
    protected boolean sendTo(EventClient client, int id, Object msg, String event) {
        boolean sent = false
        Map map = [
            event: event,
            data : msg instanceof String ? msg : JSON.encode(msg)
        ]
        if(id) {
            map.id = id
        }
        sendTo(client, new WebMessage(map), { sent = true }, null)
        return sent
    }
    protected boolean sendTo(Optional<EventClient> clientOpt, int id, Object msg, String event) {
        boolean sent = false
        if(clientOpt.present) {
            Map map = [
                event: event,
                data : msg instanceof String ? msg : JSON.encode(msg)
            ]
            if(id) {
                map.id = id
            }
            sendTo(clientOpt.get(), new WebMessage(map), { sent = true }, null)
        }
        return sent
    }
    boolean sendTo(String clientId, int id, Object msg, String event) {
        return sendTo(get(clientId), id, msg, event)
    }
    boolean sendTo(String clientId, Map msg, String event = "message") {
        return sendTo(get(clientId), 0, msg, event)
    }
    boolean sendTo(String clientId, int id, String msg, String event = "message") {
        return sendTo(get(clientId), id, msg, event)
    }
    boolean sendTo(String clientId, String msg, String event = "message") {
        return sendTo(get(clientId), 0, msg, event)
    }
    protected boolean sendToAll(int id, Object msg, String event) {
        return clientList.every {
            sendTo(it, id, msg, event)
        }
    }
    boolean broadcast(int id, Map msg, String event = "message") {
        return sendToAll(id, msg, event)
    }
    boolean broadcast(Map msg, String event = "message") {
        return sendToAll(0, msg, event)
    }
    boolean broadcast(int id, String msg, String event = "message") {
        return sendToAll(id, msg, event)
    }
    boolean broadcast(String msg, String event = "message") {
        return sendToAll(0, msg, event)
    }
}
