package jp.sharelock.web

import jp.sharelock.etc.Cmd
import jp.sharelock.etc.Command
import jp.sharelock.etc.Log
import jp.sharelock.web.samples.ChatClient
import jp.sharelock.web.samples.ChatService
import jp.sharelock.web.samples.EmailService
import jp.sharelock.web.samples.IDService
import jp.sharelock.web.samples.LoginServiceExample
import jp.sharelock.web.samples.SSLService
import jp.sharelock.web.samples.StackOverflowChatClient
import groovy.json.JsonSlurper
import jp.sharelock.web.samples.UploadService
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
            def jsonSlurper = new JsonSlurper()
            def res = jsonSlurper.parseText(json)
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
            web.start()
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