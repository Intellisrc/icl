package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.net.LocalHost
import com.intellisrc.web.samples.IDService
import com.intellisrc.web.samples.UploadService
import groovy.transform.CompileStatic

/**
 * Stand alone class to perform manual tests
 * @since 2022/07/22.
 */
@CompileStatic
class LaunchWebService {
    static File publicDir = File.get(File.userDir, "public")
    static void main(String[] args) {
        int port = LocalHost.freePort
        def web = new WebService(
            port : port,
            resources: publicDir,
            cacheTime: 60
        )
        web.addService(new IDService())
        web.addService(new UploadService(File.tempDir))
        Log.i("Running in port: %d with resources at: %s", port, publicDir)
        web.start()
    }
}
