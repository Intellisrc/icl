package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.etc.Mime
import com.intellisrc.net.LocalHost
import com.intellisrc.web.KeyStore
import com.intellisrc.web.Service
import com.intellisrc.web.WebService
import spark.Request
import spark.Response

/**
 * Simple class which is used to test features manually
 * @since 2022/07/27.
 */
class SimpleService {
    static File resourcesDir =  File.get(File.userDir, "modules", "web", "res")
    static File publicDir = File.get(resourcesDir, "public")

    static File storeFile = File.get(resourcesDir, "private", "keystore.jks")
    static String pass = "password"
    static int fixedPort = 9999

    static void main(String[] args) {
        WebService ws = new WebService(
            http2: true,
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
