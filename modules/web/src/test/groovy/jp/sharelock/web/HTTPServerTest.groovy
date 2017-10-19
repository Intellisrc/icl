package jp.sharelock.web

import groovy.xml.XmlUtil
import spock.lang.Specification

/**
 * Tests for HTTP Server
 * @since 17/07/28.
 */
class HTTPServerTest extends Specification {
    def "Serving one file... any file"() {
        setup:
            def publicDir = System.getProperty("user.dir") + "/res/public"
            /**
             * This example uses XML parser, so HTML must be XML formed.
             */
            def port = NetworkInterface.getFreePort()
            HTTPServer http = new HTTPServer(root: publicDir, port: port, serverString: "Tomboy 2.1", action: {
                Map<String,String> headers, Map<String,String> params, String content ->
                    def page = new XmlSlurper().parseText(content)
                    page.body.h1 = params["title"] ?: "HELLO DEFAULT"
                    content = XmlUtil.serialize(page)
                    return new HTTPServer.Response(content: content, mime: headers["ext"], code: 200)
            })
        when: "Starting server and check that template and getting the file works"
            Thread.start {
                http.start()
            }
            def newTitle = "Hello World!"
            def url = ("http://localhost:$port/template.html?title="+URLEncoder.encode(newTitle,"UTF-8")).toURL()
        then:
            assert url.text.contains("<h1>"+newTitle+"</h1>")
        when: "Closing connection and testing that is closed"
            http.stop()
            "http://localhost:$port/template.html?title=$newTitle".toURL().text
        then:
            thrown ConnectException
    }
}