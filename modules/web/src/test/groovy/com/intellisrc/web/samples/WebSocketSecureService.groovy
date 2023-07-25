package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.net.LocalHost
import com.intellisrc.web.WebService
import com.intellisrc.web.service.KeyStore

/**
 * Example on how to enable WebSocket over SSL
 * @since 2022/07/27.
 */
class WebSocketSecureService {
    static File resourcesDir =  File.get(File.userDir, "modules", "web", "res")
    static File publicDir = File.get(resourcesDir, "public")

    static File storeFile = File.get(resourcesDir, "private", "keystore.jks")
    static String pass = "password"
    static int fixedPort = 9999

    static void main(String[] args) {
        WebService ws = new WebService(
            port: fixedPort ?: LocalHost.freePort,
            resources: publicDir,
            ssl: new KeyStore(storeFile, pass)
        )
        Log.i("Web Service available at port: %d", ws.port)
        ws.add(new ChatWebSocketService()).start()
        ws.start()
    }
}
