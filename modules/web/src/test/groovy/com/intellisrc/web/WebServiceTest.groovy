package com.intellisrc.web

import com.intellisrc.core.Cmd
import com.intellisrc.core.Log
import com.intellisrc.etc.Cache
import com.intellisrc.etc.JSON
import com.intellisrc.net.LocalHost
import com.intellisrc.web.samples.*
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Service
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.eclipse.jetty.http.HttpStatus.*

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
            URL url = ("http://localhost:" + port + "/test").toURL()
            def conn = url.openConnection() as HttpURLConnection
        then:
            assert conn.responseCode == OK_200 : "Incorrect response code"
            assert web.isRunning() : "Web Server is not running"
            assert url.text.contains("ok")
        when:
            url = ("http://localhost:" + port + "/non-existant").toURL()
            conn = url.openConnection() as HttpURLConnection
        then:
            assert conn.responseCode == NOT_FOUND_404: "Page should not exists"
            assert web.isRunning() : "Server should not crash"
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
            IDService idService = new IDService()
            web.addService(idService)
        when:
            web.start(true)
        then:
            assert web.isRunning() : "Web Server is not running"
            assert ("http://localhost:" + port).toURL().text.contains("Hello") : "Static content not found"
        when:
            int number = new Random().nextInt(100)
            URL url = ("http://localhost:" + port + "/id/" + number + "/").toURL()
            def conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
        then:
            assert conn.getHeaderField("Access-Control-Allow-Origin") == "127.0.0.1"
            assert conn.responseCode == OK_200 : "Incorrect response code"
        when:
            def json = url.text
        then:
            assert json : "Expected JSON but got empty response from ${url}"
            assert !json.contains("<html>")
        when:
            def res = JSON.decode(json) as Map
        then:
            assert res instanceof Map : "Response was not a map"
            assert (res.i as int) == number
            assert res.t.toString().matches(/\d{2}:\d{2}:\d{2}/)    //res.t returns current time in HH:mm:ss
        when:
            String strErrorUrl = "http://localhost:" + port + "/id/error503/"
            URL errorUrl = strErrorUrl.toURL()
            def conn2 = errorUrl.openConnection() as HttpURLConnection
            conn2.requestMethod = "GET"
        then:
            assert conn2.responseCode == SERVICE_UNAVAILABLE_503: "Incorrect response code"
            assert web.isRunning() : "Server should not crash"
            assert url.text.startsWith("{") : "Server should work as usual"
            assert idService.errors.get() == 1 : "onError should be called"
        when:
            String strErrorUrl2 = "http://localhost:" + port + "/id/error501/"
            URL errorUrl2 = strErrorUrl2.toURL()
            def conn3 = errorUrl2.openConnection() as HttpURLConnection
            conn3.requestMethod = "GET"
        then:
            assert conn3.responseCode == NOT_IMPLEMENTED_501: "Incorrect response code"
            assert web.isRunning() : "Server should not crash"
            assert url.text.startsWith("{") : "Server should work as usual"
            assert idService.errors.get() == 2 : "onError should be called"
        when:
            URL boomUrl = ("http://localhost:" + port + "/id/boom/").toURL()
            def conn4 = boomUrl.openConnection() as HttpURLConnection
            conn4.requestMethod = "GET"
        then:
            assert conn4.responseCode == INTERNAL_SERVER_ERROR_500 : "Incorrect response code"
            assert web.isRunning() : "Server should not crash"
            assert url.text.startsWith("{") : "Server should work as usual"
            assert idService.errors.get() == 3 : "onError should be called"
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
            IDService idService = new IDService()
            web.addService(idService)
        when:
            web.start(true)
        then:
            assert web.isRunning() : "Web Server is not running"
        when:
            def str = new URL("http://localhost:${port}/id/1/").text
        then:
            assert str : "Empty response"
            def json = JSON.decode(str) as Map
            assert json.i && json.t : "Incorrect JSON content"
            println "Json: " + json
            assert idService.calls.get() == 1
        when:
            def str_new = new URL("http://localhost:${port}/id/1/").text
        then:
            assert idService.calls.get() == 1   // It should not increment
            assert str == str_new
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
            assert web.isRunning() : "Web Server is not running"
            println "Server running on port: $port"
        when:
            URL url = new URL("http://localhost:${port}/emails/john/example.com")
            def conn = url.openConnection() as HttpURLConnection
        then:
            assert conn.getHeaderField("Access-Control-Allow-Origin") == "*"
            assert conn.responseCode == OK_200: "Incorrect response code"
        when:
            def text = url.text
            println "Email is: $text"
        then:
            assert text == "john@example.com"
        when:
            web.stop()
        then:
            assert !web.isRunning()
    }

    def "Add root should work fine"() {
        expect:
            assert WebService.addRoot(root, service) == expected
        where:
            root        | service           | expected
            ""          | "/hello"          | "/hello"
            "/"         | "/hello"          | "/hello"
            "/hello"    | ""                | "/hello"
            "/hello/"   | "world/"          | "/hello/world/"
            "/hello/"   | ":name/"          | "/hello/:name/"
            "/my"       | ".do"             | "/my.do"
            ""          | "hello"           | "/hello"
            "/my"       | "~/\\d+/"        | "~/my\\d+/"
            "/my"       | ":path"           | "/my/:path"
            "/my"       | "*"               | "/my/*"
            "/my"       | "/some/:path"     | "/my/some/:path"
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
            assert web.isRunning() : "Web Server is not running"
            println "Server running on port: $port"
        when:
            URL url = "http://localhost:${port}/${path}".toURL()
            def conn = url.openConnection() as HttpURLConnection
        then:
            assert conn.responseCode == OK_200: "Incorrect response code"
        when:
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
            assert web.isRunning() : "Web Server is not running"
        when:
            URL chkUrl = "http://localhost:$port/check".toURL()
            def conn = chkUrl.openConnection() as HttpURLConnection
        then:
            assert conn.responseCode == OK_200: "Incorrect response code"
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

    def "Test concurrency"() {
        setup:
            int port = LocalHost.freePort
            def web = new WebService(
                port: port,
                resources: publicDir
            )
            IDService idService = new IDService(cacheTime: Cache.DISABLED)
            web.addService(idService)
            AtomicInteger count = new AtomicInteger(0)
        when:
            web.start(true)
            def pool = Executors.newFixedThreadPool(10)
            (1..50).each {
                Integer id ->
                    pool.submit {
                        String txt = new URL("http://localhost:${port}/id/$id/").text
                        if(txt.contains("{")) {
                            count.getAndIncrement()
                        }
                    }
            }
        then:
            pool.shutdown()
            pool.awaitTermination(5, TimeUnit.SECONDS)
            assert count.get() == 50
            web.stop()
            assert !web.isRunning()
    }
}