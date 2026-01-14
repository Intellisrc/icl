package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.net.LocalHost
import com.intellisrc.web.WebService
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Response
import com.intellisrc.web.service.Service
import groovy.transform.CompileStatic

/**
 * Simple class which is used to test features manually
 * @since 2022/07/27.
 */
@CompileStatic
class SimpleService {
    static File resourcesDir =  File.get(File.userDir, "modules", "web", "res")
    static File publicDir = File.get(resourcesDir, "public")

    static File storeFile = File.get(resourcesDir, "private", "keystore.jks")
    static String pass = "password"
    static int fixedPort = 6789

    static void main(String[] args) {
        WebService ws = new WebService(
            //protocol: Protocol.HTTP2,
            port: fixedPort ?: LocalHost.freePort,
            resources: publicDir,
            cache: [   // It will use the lower age for the matching rule:
                "{jpg,png,gif}" : 500,
                "*.js"          : 100,
                "/css/*"        : 200,
                "*"             : 800 // Same as 'cacheTime'
            ],
            // max-age header for static resources:
            maxAge: [   // It will use the lower age for the matching rule:
                "{jpg,png,gif}" : 500,
                "*.js"          : 100,
                "/css/*"        : 200,
                "*"             : 800 // Same as 'maxAgeDefault'
            ],
            maxAgeDefault: 300
            //ssl: new KeyStore(storeFile, pass)
        )
        Log.i("Web Service available at port: %d", ws.port)
        ws.add(new Service(
            path: ~/jquery-(?<name>[^.]+)\.js/,
            samplePaths: ["jquery-test.js"],
            maxAge: 120,
            //contentType: Mime.JS, (not needed as we are returning a File object)
            compress: true,
            action: {
                Request request, Response response ->
                    return File.get(publicDir, "jquery.js")
            }
        )).start()
    }
}
