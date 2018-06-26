package com.intellisrc.web

import com.intellisrc.etc.Cmd
import com.intellisrc.etc.Log
import com.intellisrc.web.samples.IDService
import com.intellisrc.web.samples.StackOverflowChatClient
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @since 17/04/19.
 */
class WebServiceTest extends Specification {
    def publicDir = System.getProperty("user.dir") + "/res/public/"

    def "General Test"() {
        setup:
            Log.logFile = "web.log"
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
            web.start()
        then:
            assert web.isRunning()
            assert ("http://localhost:"+port).toURL().text.contains("Hello")
        when:
            def number = new Random().nextInt(100)
            def json = ("http://localhost:"+port+"/id?i="+number).toURL().text
        then:
            assert json
            assert !json.contains("<html>")
        when:
            def res = JSON.toMap(json)
        then:
            assert res instanceof Map
            assert res.i == number
            assert res.t > 0
        when:
            web.stop()
        then:
            assert !web.isRunning()
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
            web.start()
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
            web.addService(new com.intellisrc.web.samples.EmailService())
        when:
            web.start()
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
            web.addService(new com.intellisrc.web.samples.UploadService())
        when:
            web.start()
        then:
            assert web.isRunning()
        when:
            def emptyGif = publicDir + "empty.gif"
            def field = com.intellisrc.web.samples.UploadService.fieldName
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
            web.addService(new com.intellisrc.web.samples.SSLService())
            web.start()
        expect:
            assert web.isRunning()
            assert "https://localhost:$port/admin.txt".toURL().text.contains("9EEyY")
            web.stop()
            assert !web.isRunning()
    }

    def "Websocket Test"() {
        setup:
            def port = NetworkInterface.getFreePort()
            def web = new WebService(
                    port : port,
                    //resources : 'public'    <--- this is the recommended way to specify resources
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            web.setResources(System.getProperty("user.dir") + "/res/public/", true)
            web.addService(new com.intellisrc.web.samples.ChatService())
            web.start()
        expect:
            assert web.isRunning()
            web.stop()
            assert !web.isRunning()
    }

    @Ignore("Broken")
    def "WebSocket Client"() {
        setup:
            com.intellisrc.web.samples.ChatClient cc = new com.intellisrc.web.samples.ChatClient()
            cc.Connect()
    }

    @Ignore("Broken")
    def "WebSocket StackOverflow Client"() {
        setup:
        StackOverflowChatClient cc = new StackOverflowChatClient()
        cc.Connect()
    }
}