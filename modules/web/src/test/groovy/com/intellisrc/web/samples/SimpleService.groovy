package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.etc.Mime
import com.intellisrc.net.LocalHost
import com.intellisrc.web.*
import com.intellisrc.web.protocols.Protocol

/**
 * Simple class which is used to test features manually
 * @since 2022/07/27.
 */
class SimpleService {
    static File resourcesDir =  File.get(File.userDir, "modules", "web", "res")
    static File publicDir = File.get(resourcesDir, "public")

    static File storeFile = File.get(resourcesDir, "private", "keystore.jks")
    static String pass = "password"
    static int fixedPort = 0

    static void main(String[] args) {
        WebService ws = new WebService(
            protocol: Protocol.HTTP2,
            port: fixedPort ?: LocalHost.freePort,
            resources: publicDir,
            ssl: new KeyStore(storeFile, pass)
        )
        Log.i("Web Service available at port: %d", ws.port)
        ws.add(new Service(
            path: ~/^jquery-(?<name>[^.]+)\.js/,
            contentType: Mime.JS,
            compress: true,
            compressSize: true,
            action: {
                Request request, Response response ->
                    return File.get(publicDir, "jquery.js")
            }
        )).start()
    }
}
