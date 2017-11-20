package jp.sharelock.web

import jp.sharelock.etc.Log
import jp.sharelock.web.samples.ChatClient
import jp.sharelock.web.samples.ChatService
import jp.sharelock.web.samples.EmailService
import jp.sharelock.web.samples.IDService
import jp.sharelock.web.samples.LoginServiceExample
import jp.sharelock.web.samples.SSLService
import jp.sharelock.web.samples.StackOverflowChatClient
import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @since 17/04/19.
 */
class WebServiceTest extends Specification {

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
            println "RES DIR:" + System.getProperty("user.dir") + "/res/public/"
            web.setResources(System.getProperty("user.dir") + "/res/public/", true)
            web.addService(new EmailService())
            web.addService(new IDService())
            web.addService(new LoginServiceExample())
            web.start()
        expect:
            assert web.isRunning()
            //assert ("http://localhost:"+port).toURL().text.contains("Hello")
            def jsonSlurper = new JsonSlurper()
            //def res = jsonSlurper.parseText(("http://localhost:"+port+"/id").toURL().text)
            //assert res instanceof Map
            //assert res.i > 0
            web.stop()
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
            println "RES DIR:" + System.getProperty("user.dir") + "/res/public/"
            web.setResources(System.getProperty("user.dir") + "/res/public/", true)
            web.addService(new IDService())
            web.start()
        expect:
            assert web.isRunning()
            assert new URL("http://localhost:${port}/id?i=1").text
            sleep(2000)
            assert new URL("http://localhost:${port}/id?i=1").text
            //TODO: count if method was called
            web.stop()
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
            web.start()
        expect:
            assert web.isRunning()
            assert "https://localhost:$port/admin.txt".toURL().text.contains("9EEyY")
            web.stop()
            assert !web.isRunning()
    }

    def "Websocket Test"() {
        setup:
            def port = 8888
            def web = new WebService(
                    port : port,
                    //resources : 'public'    <--- this is the recommended way to specify resources
                    cacheTime: 60
            )
            // Resources set as full path because code is executed under /tst/ usually use above method
            web.setResources(System.getProperty("user.dir") + "/res/public/", true)
            web.addService(new ChatService())
            web.start()
        expect:
            assert web.isRunning()
            web.stop()
            assert !web.isRunning()
    }

    @Ignore("Broken")
    def "WebSocket Client"() {
        setup:
            ChatClient cc = new ChatClient()
            cc.Connect()
    }

    @Ignore("Broken")
    def "WebSocket StackOverflow Client"() {
        setup:
        StackOverflowChatClient cc = new StackOverflowChatClient()
        cc.Connect()
    }
}