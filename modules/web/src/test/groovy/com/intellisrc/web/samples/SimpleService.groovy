package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.etc.Mime
import com.intellisrc.web.Service
import com.intellisrc.web.WebService
import spark.Request
import spark.Response

import static spark.Response.Compression.BROTLI_COMPRESS

/**
 * @since 2022/07/27.
 */
class SimpleService {
    static File publicDir = File.get(File.userDir, "modules", "web", "res", "public")

    static void main(String[] args) {
        WebService ws = new WebService(
            port: 5757, //LocalHost.freePort,
            resources: publicDir
        )
        Log.i("Web Service available at port: %d", ws.port)
        ws.add(new Service(
            //path: "~/\\/test-(?<name>[^.]+)\\.down",
            path: "/test.txt",
            contentType: Mime.TXT,
            //reportSize: true,
            action: {
                Request request, Response response ->
                    response.compression = BROTLI_COMPRESS
                    return File.get(publicDir, "jquery.js")
            }
        )).start()
    }
}
