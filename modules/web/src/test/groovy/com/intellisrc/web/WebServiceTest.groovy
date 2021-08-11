package com.intellisrc.web

import com.intellisrc.core.Cmd
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.intellisrc.web.samples.*
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

/**
 * @since 17/04/19.
 */
class WebServiceTest extends Specification {
    File publicDir = SysInfo.getFile(SysInfo.userDir, "res", "public")

    def "General Test"() {
        setup:
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                port : port,
                resources: publicDir,
                cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            Log.i("Running in port: %d with resources at: %s", port, publicDir)
            web.addService(new IDService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
            assert ("http://localhost:"+port).toURL().text.contains("Hello")
        when:
            int number = new Random().nextInt(100)
            def json = ("http://localhost:"+port+"/id/"+number+"/").toURL().text
        then:
            assert json
            assert !json.contains("<html>")
        when:
            def res = JSON.decode(json).toMap()
        then:
            assert res instanceof Map
            assert (res.i as int) == number
            assert res.t.toString().matches(/\d{2}:\d{2}:\d{2}/)    //res.t returns current time in HH:mm:ss
        when:
            web.stop()
        then:
            assert !web.isRunning()
    }

    /**
     * Testing also resources as File
     */
    def "Testing auto cache"() {
        setup:
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    resources : publicDir,
                    cacheTime: 60
            )
            web.addService(new IDService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
        when:
            def json = new URL("http://localhost:${port}/id/1/").text
        then:
            assert json
            println "Json: "+json
        when:
            sleep(2000)
            def json_new = new URL("http://localhost:${port}/id/1/").text
        then:
            //TODO: count if method was called
            assert json == json_new
        when:
            web.stop()
        then:
            assert !web.isRunning()
    }

    /**
     * Testing also string as resources, callback 'onStart' and chained calls
     */
    def "Test parameters and splat"() {
        setup:
            def conds = new AsyncConditions()
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    resources: publicDir,
                    cacheTime: 60,
                    allowOrigin: "*"
            ).add(new EmailService()).start(true, {
                conds.evaluate {
                    assert true
                }
            })
        expect:
            conds.await()
            assert web.isRunning()
            println "Server running on port: $port"
        when:
            def text = new URL("http://localhost:${port}/emails/john/example.com").text
            println "Email is: $text"
        then:
            assert text == "john@example.com"
        when:
            web.stop()
        then:
            assert !web.isRunning()
    }

    def "Test Upload"() {
        setup:
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    resources: publicDir
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            Log.i("Public directory is: %s", publicDir.absolutePath)
            File uploadDir = new File(publicDir, "upload")
            Log.i("Upload directory is: %s", uploadDir.absolutePath)
            if(!uploadDir.exists()) {
                uploadDir.mkdirs()
            }
            web.addService(new UploadService(uploadDir))
        when:
            web.start(true)
        then:
            assert web.isRunning()
        when:
            URL chkUrl = "http://localhost:$port/check".toURL()
        then:
            assert chkUrl.text == "ok" : "Web Server failed to respond"
            Log.i("Web server responded 'ok'")
        when:
            File emptyGif = new File(publicDir, "empty.gif")
        then:
            URL url = "http://localhost:$port/upload".toURL()
            Log.i("Uploading file to: %s", url.toExternalForm())
            Cmd.exec("curl",["-s", "-F", "image_name=@${emptyGif.absolutePath}", url.toExternalForm()], {
                String out ->
                    assert out.startsWith("GIF89a")
                    assert new File(uploadDir, "empty.gif").exists()
                    Log.i("File uploaded successfully")
            })
        when:
            web.stop()
        then:
            assert !web.isRunning()
        cleanup:
            uploadDir.eachFile { it.delete() }
    }

    @Ignore("Broken") // Requires a valid certificate
    def "HTTPS"() {
        setup:
            def port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    resources: publicDir,
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            web.addService(new SSLService())
            web.start(true)
        expect:
            assert web.isRunning()
            assert "https://localhost:$port/admin.txt".toURL().text.contains("9EEyY")
            web.stop()
            assert !web.isRunning()
    }

    def "Websocket Test"() {
        setup:
        def keepalive = false // change to 'true' to test manually WebSocket Clients
        def web = new WebService(
                port : NetworkInterface.getFreePort(),
                // Resources set as full path because code is executed under /tst/
                resources : System.getProperty("user.dir") + "/res/public/",
                cacheTime: 60
        )
        web.addService(new ChatService())
        web.start(!keepalive)
        expect:
        assert web.isRunning()
        web.stop()
        assert !web.isRunning()
    }

    /* Comment next line to test and set "keepalive = true" in the server test */
    @Ignore("Broken")
    def "WebSocket Client"() {
        setup:
        ChatClient cc = new ChatClient()
        cc.Connect()
    }

    /* Comment next line to test and set "keepalive = true" in the server test */
    @Ignore("Broken")
    def "WebSocket StackOverflow Client"() {
        setup:
        StackOverflowChatClient cc = new StackOverflowChatClient()
        cc.Connect()
    }
}