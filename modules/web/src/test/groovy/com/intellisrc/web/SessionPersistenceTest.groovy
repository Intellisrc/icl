package com.intellisrc.web

import com.intellisrc.core.Millis
import com.intellisrc.etc.BerkeleyDB
import com.intellisrc.etc.JSON
import com.intellisrc.etc.Redis
import com.intellisrc.net.LocalHost
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.session.BerkeleySession
import com.intellisrc.web.service.session.FileSession
import com.intellisrc.web.service.session.RedisSession
import com.intellisrc.web.service.session.SessionPersistable
import com.intellisrc.web.services.LoginService
import com.intellisrc.web.services.LoginService.Level
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.net.InetSocketAddress
import java.net.Socket
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

/**
 * Tests that sessions can be kept across a WebService restart
 * by using a persistent session store (SessionPersistable)
 *
 * @since 2026/09/11.
 */
class SessionPersistenceTest extends Specification {

    private LoginService auth() {
        return new LoginService(
            rootPath: "auth",
            onLoginAuth: { String user, String password ->
                return (user == "test" && password == "test") ? Level.USER : Level.GUEST
            } as LoginService.LoginAuth
        )
    }

    private Service privateService() {
        return new Service(
            path: "private",
            allow: LoginService.User,
            action: { Request request ->
                return [user: request.session.attribute("user")]
            }
        )
    }

    /**
     * Start a WebService with authentication.
     * A "restart" is simulated by creating a new WebService that
     * shares the same session store.
     */
    private WebService startService(int port, SessionPersistable store = null, int timeout = 0) {
        WebService web = new WebService(port: port)
        if (store != null) {
            web.sessionType = store
        }
        web.sessionTimeout = timeout
        web.add(auth())
        web.add(privateService())
        web.start(true)
        return web
    }

    private URI privatePage(int port) {
        return "http://localhost:${port}/private".toURI()
    }

    private HttpResponse<String> get(URI uri, String sessionId = null) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).GET()
        if (sessionId) {
            builder.header("Cookie", "JSESSIONID=" + sessionId)
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    /**
     * Login and return the session id
     */
    private String login(int port) {
        URI uri = "http://localhost:${port}/auth/login?user=test&pass=test".toURI()
        HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build()
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        assert response.statusCode() == 200 : "Login failed: " + response.body()
        return JSON.decode(response.body()).session_id?.toString()
    }

    def "File session store keeps sessions across a restart"() {
        setup:
            int port = LocalHost.freePort
            File storeDir = Files.createTempDirectory("web-sessions-file").toFile()
            WebService web1 = startService(port, new FileSession(storeDir))
        when: "a user logs in"
            String sessionId = login(port)
            HttpResponse<String> before = get(privatePage(port), sessionId)
        and: "the service is restarted"
            web1.stop()
            sleep(Millis.SECOND)
            WebService web2 = startService(port, new FileSession(storeDir))
            HttpResponse<String> after = get(privatePage(port), sessionId)
        then:
            before.statusCode() == 200
            storeDir.listFiles().length > 0 // session was written to disk
            after.statusCode() == 200
            Map body = JSON.decode(after.body()) as Map
            body.user == "test"
        cleanup:
            web1?.stop()
            web2?.stop()
            storeDir?.deleteDir()
    }

    def "Berkeley session store keeps sessions across a restart"() {
        setup:
            int port = LocalHost.freePort
            File dbDir = Files.createTempDirectory("web-sessions-berkeley").toFile()
            // Disable JE free-disk check: the temp dir may be a small tmpfs and JE
            // prohibits writes when free space is below its 5GB default (je.freeDisk)
            new File(dbDir, "je.properties").text = "je.freeDisk=0"
            WebService web1 = startService(port, new BerkeleySession(new BerkeleyDB(new File(dbDir, "sessions"))))
        when: "a user logs in"
            String sessionId = login(port)
            HttpResponse<String> before = get(privatePage(port), sessionId)
        and: "the service is restarted"
            web1.stop() // this closes the database
            sleep(Millis.SECOND)
            WebService web2 = startService(port, new BerkeleySession(new BerkeleyDB(new File(dbDir, "sessions"))))
            HttpResponse<String> after = get(privatePage(port), sessionId)
        then:
            before.statusCode() == 200
            after.statusCode() == 200
            Map body = JSON.decode(after.body()) as Map
            body.user == "test"
        cleanup:
            web1?.stop()
            web2?.stop()
            dbDir?.deleteDir()
    }

    @IgnoreIf({ !SessionPersistenceTest.redisAvailable() })
    def "Redis session store keeps sessions across a restart"() {
        setup:
            int port = LocalHost.freePort
            String prefix = "web-sessions-test"
            new Redis(prefix).clear()
            WebService web1 = startService(port, new RedisSession(new Redis(prefix)))
        when: "a user logs in"
            String sessionId = login(port)
            HttpResponse<String> before = get(privatePage(port), sessionId)
        and: "the service is restarted"
            web1.stop()
            sleep(Millis.SECOND)
            WebService web2 = startService(port, new RedisSession(new Redis(prefix)))
            HttpResponse<String> after = get(privatePage(port), sessionId)
        then:
            before.statusCode() == 200
            after.statusCode() == 200
            Map body = JSON.decode(after.body()) as Map
            body.user == "test"
        cleanup:
            web1?.stop()
            web2?.stop()
            new Redis(prefix).clear()
    }

    def "Logout invalidates the persisted session"() {
        setup:
            int port = LocalHost.freePort
            File storeDir = Files.createTempDirectory("web-sessions-logout").toFile()
            WebService web1 = startService(port, new FileSession(storeDir))
            String sessionId = login(port)
        when: "the user logs out and the service is restarted"
            HttpResponse<String> logout = get("http://localhost:${port}/auth/logout".toURI(), sessionId)
            web1.stop()
            sleep(Millis.SECOND)
            WebService web2 = startService(port, new FileSession(storeDir))
            HttpResponse<String> after = get(privatePage(port), sessionId)
        then:
            logout.statusCode() == 200
            after.statusCode() == 403
        cleanup:
            web1?.stop()
            web2?.stop()
            storeDir?.deleteDir()
    }

    def "Sessions are kept in memory by default and do not survive a restart"() {
        setup:
            int port = LocalHost.freePort
            WebService web1 = startService(port)
        when: "a user logs in"
            String sessionId = login(port)
            HttpResponse<String> before = get(privatePage(port), sessionId)
        and: "the service is restarted"
            web1.stop()
            sleep(Millis.SECOND)
            WebService web2 = startService(port)
            HttpResponse<String> after = get(privatePage(port), sessionId)
        then:
            web1.sessionType.name() == "memory" // default store
            before.statusCode() == 200
            after.statusCode() == 403
        cleanup:
            web1?.stop()
            web2?.stop()
    }

    def "Session timeout applies to in-memory sessions"() {
        setup:
            int port = LocalHost.freePort
            WebService web = startService(port, null, Millis.SECOND_2)
            String sessionId = login(port)
        when: "the session is not used for a while"
            HttpResponse<String> before = get(privatePage(port), sessionId)
            sleep(Millis.SECOND_4)
            HttpResponse<String> after = get(privatePage(port), sessionId)
        then:
            before.statusCode() == 200
            after.statusCode() == 403
        cleanup:
            web?.stop()
    }

    def "web.session.type selects the session store"() {
        when:
            // Note: webSessionType is read once (static), so we set it directly:
            WebService.webSessionType = "file"
            WebService web = new WebService()
        then:
            web.createSessionType().name() == "file"
        cleanup:
            WebService.webSessionType = "memory"
            File.get(".sessions").deleteDir()
    }

    def "Unknown web.session.type falls back to memory"() {
        when:
            WebService.webSessionType = "whatever"
            WebService web = new WebService()
        then:
            web.createSessionType().name() == "memory"
        cleanup:
            WebService.webSessionType = "memory"
    }

    private static boolean redisAvailable() {
        boolean available = false
        try {
            Socket socket = new Socket()
            socket.connect(new InetSocketAddress(Redis.host, Redis.port), 500)
            socket.close()
            available = true
        } catch (Exception ignore) {}
        return available
    }
}
