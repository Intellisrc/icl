package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.net.LocalHost
import com.intellisrc.web.WebService

/**
 * Example on how to execute WebSocket server
 * @since 2022/07/27.
 */
class WebSocketTestService {
    static File resourcesDir =  File.get(File.userDir, "modules", "web", "res")
    static File publicDir = File.get(resourcesDir, "public")
    static int fixedPort = 9999

    static void main(String[] args) {
        WebService ws = new WebService(
            port: fixedPort ?: LocalHost.freePort,
            resources: publicDir
        )
        Log.i("Open a browser at: http://localhost:%d/chat.html", ws.port)
        ws.add(new ChatWebSocketServiceIface())
        ws.start()
    }
}
