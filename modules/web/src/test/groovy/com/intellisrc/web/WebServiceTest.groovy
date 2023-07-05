package com.intellisrc.web

import com.intellisrc.core.Cmd
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.etc.JSON
import com.intellisrc.net.LocalHost
import com.intellisrc.web.samples.*
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Service
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

/**
 * @since 17/04/19.
 */
class WebServiceTest extends Specification {
    File publicDir = File.get(File.userDir, "res", "public")

    def "Starting server without resources directory"() {
        setup:
            int port = LocalHost.freePort
            def web = new WebService(
                    port: port
            )
            Log.i("Running in port: %d", port)
            web.add(new Service(
                path: "test",
                action: { "ok" }
            ))
        when:
            web.start(true)
        then:
            assert web.isRunning()
            assert ("http://localhost:" + port + "/test").toURL().text.contains("ok")
        cleanup:
            web.stop()
            assert ! web.running
    }

    def "General Test"() {
        setup:
            int port = LocalHost.freePort
            def web = new WebService(
                port: port,
                resources: publicDir,
                cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/
            Log.i("Running in port: %d with resources at: %s", port, publicDir)
            web.addService(new IDService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
            assert ("http://localhost:" + port).toURL().text.contains("Hello")
        when:
            int number = new Random().nextInt(100)
            URL url = ("http://localhost:" + port + "/id/" + number + "/").toURL()
            def json = url.text
        then:
            assert json
            assert !json.contains("<html>")
        when:
            def res = JSON.decode(json) as Map
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
            int port = LocalHost.freePort
            def web = new WebService(
                port: port,
                resources: publicDir,
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
            println "Json: " + json
        when:
            sleep(Millis.SECOND_2)
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
            int port = LocalHost.freePort
            def web = new WebService(
                port: port,
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

    def "Test Regex paths"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            def srv = new Service(
                path : regex,
                action: {
                    Request request ->
                        return request.params("number")
                }
            )
            def web = new WebService(
                port: port,
                resources: publicDir,
                allowOrigin: "*"
            ).add(srv).start(true, {
                conds.evaluate {
                    assert true
                }
            })
            Log.i("Testing regex: %s  :  %s <-- %s", regex, srv.path, path)
        expect:
            conds.await()
            assert web.isRunning()
            println "Server running on port: $port"
        when:
            URL url = "http://localhost:${port}/${path}".toURL()
            Log.i("Requesting: %s", url)
            def text = url.text
            int num = text as int
        then:
            assert num == id
        when:
            web.stop()
        then:
            assert !web.isRunning()
        where:
            regex                                   | path                              | id
            ~/(?<number>\d+)-\w+\.html/             | "1234-hello.html"                 | 1234
            /(?<number>\d+)-\w+\.html/              | "9999-hello.html"                 | 9999
            "/(?<number>\\d+)-\\w+\\.html/"         | "5432-hello.html"                 | 5432
            ~/^(?<number>\d+)-\w+\.html$/           | "6868-hello.html"                 | 6868
    }

    def "Test Upload"() {
        setup:
            int port = LocalHost.freePort
            def web = new WebService(
                port: port,
                resources: publicDir
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            Log.i("Public directory is: %s", publicDir.absolutePath)
            File uploadDir = new File(publicDir, "upload")
            Log.i("Upload directory is: %s", uploadDir.absolutePath)
            if (!uploadDir.exists()) {
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
            assert chkUrl.text == "ok": "Web Server failed to respond"
            Log.i("Web server responded 'ok'")
        when:
            File emptyGif = new File(publicDir, "empty.gif")
        then:
            URL url = "http://localhost:$port/upload".toURL()
            Log.i("Uploading file to: %s", url.toExternalForm())
            Cmd.exec("curl", ["-s", "-F", "image_name=@${emptyGif.absolutePath}", url.toExternalForm()], {
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

    def "Websocket Test"() {
        setup:
            // change to 'true' to test manually WebSocket Clients
            // and open the browser in /chat.html
            def keepalive = false
            def web = new WebService(
                port: LocalHost.freePort,
                // Resources set as full path because code is executed under /tst/
                resources: System.getProperty("user.dir") + "/res/public/",
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

    //@PendingFeature
    def "WebSocket Client"() {
        setup:
            ChatClient cc = new ChatClient()
            cc.Connect()
    }

    /* Comment next line to test and set "keepalive = true" in the server test */

    @PendingFeature
    def "WebSocket StackOverflow Client"() {
        setup:
            StackOverflowChatClient cc = new StackOverflowChatClient()
            cc.Connect()
    }
}