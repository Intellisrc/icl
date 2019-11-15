package com.intellisrc.web

import com.intellisrc.core.Cmd
import com.intellisrc.core.Log
import com.intellisrc.web.samples.ChatClient
import com.intellisrc.web.samples.ChatService
import com.intellisrc.web.samples.EmailService
import com.intellisrc.web.samples.IDService
import com.intellisrc.web.samples.SSLService
import com.intellisrc.web.samples.StackOverflowChatClient
import com.intellisrc.web.samples.UploadService
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @since 17/04/19.
 */
class WebServiceTest extends Specification {
    def publicDir = System.getProperty("user.dir") + "/res/public/"

    def "General Test"() {
        setup:
            Log.logFileName = "web.log"
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                port : port,
              //resources : 'public'    <--- this is the recommended way to specify resources
                cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            println "RES DIR: $publicDir"
            web.setResources(publicDir, true)
            web.addService(new IDService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
            assert ("http://localhost:"+port).toURL().text.contains("Hello")
        when:
            int number = new Random().nextInt(100)
            def json = ("http://localhost:"+port+"/id?i="+number).toURL().text
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
        cleanup:
            if(Log.logFile && Log.logFile.exists()) {
                Log.logFile.delete()
            }
    }

    def "Testing auto cache"() {
        setup:
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    //resources : 'public'    <--- this is the recommended way to specify resources
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            println "RES DIR: $publicDir"
            web.setResources(publicDir, true)
            web.addService(new IDService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
        when:
            def json = new URL("http://localhost:${port}/id?i=1").text
        then:
            assert json
            println "Json: "+json
        when:
            sleep(2000)
            def json_new = new URL("http://localhost:${port}/id?i=1").text
        then:
            //TODO: count if method was called
            assert json == json_new
        when:
            web.stop()
        then:
            assert !web.isRunning()
    }

    def "Test parameters and splat"() {
        setup:
            int port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    //resources : 'public'    <--- this is the recommended way to specify resources
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            println "RES DIR: $publicDir"
            web.setResources(publicDir, true)
            web.addService(new EmailService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
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
                    //resources : 'public'    <--- this is the recommended way to specify resources
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            println "RES DIR: $publicDir"
            web.setResources(publicDir, true)
            web.addService(new UploadService())
        when:
            web.start(true)
        then:
            assert web.isRunning()
        when:
            def emptyGif = publicDir + "empty.gif"
            def field = UploadService.fieldName
        then:
            Cmd.exec("curl",["-s", "-F", "$field=@$emptyGif", "http://localhost:$port/upload"], {
                String out ->
                    assert out.startsWith("GIF89a")
            })
        when:
            web.stop()
        then:
            assert !web.isRunning()

    }

    @Ignore("Broken")
    def "HTTPS"() {
        setup:
            def port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    //resources : 'public'    <--- this is the recommended way to specify resources
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            web.setResources(System.getProperty("user.dir") + "/res/public", true)
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
        //def port = NetworkInterface.getFreePort()
        def web = new WebService(
                port : 8888,
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