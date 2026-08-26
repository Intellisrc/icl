---
name: web
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) to build HTTP services - REST APIs returning JSON, WebService (Jetty 12 based server with HTTP/1, HTTP/2, HTTP/3, HTTPS, static files, compression), Service definition with path and method, Serviciable/ServiciableAuth/ServiciableWebSocket interfaces, sessions and login, Server-Sent Events (SSE), WebSocket services and broadcast, or WebClient for calling HTTP APIs. Triggers on com.intellisrc.web imports in an ICL project. Note the module was migrated from Spark-Java to Jetty in 2.10.x.
---

# ICL web Module

Web server/client for ICL, wrapping Jetty 12 (EE10). Define services as closures; get JSON serialization, sessions, SSE, WebSockets, static files, compression and access logs. Includes core, etc, net.

```groovy
implementation 'com.intellisrc:web:2.10.5'
// optional Brotli compression:
implementation 'com.nixxcode.jvmbrotli:jvmbrotli'         // + jvmbrotli-<os>-<arch>
```

Packages: `com.intellisrc.web` (WebService, WebClient), `web.service.*` (Service, Request, Response, sessions, SSE, WebSockets), `web.services.*` (ready-made Login/Log/AutoConfig services), `web.tools.AccessLog`.

**2.10 migration note:** built on Jetty 12, not Spark-Java. Older examples showing `Service.get(...)`, `path.staticPath`, `Service.Message` or `msgBroadCaster` are outdated — use the APIs below.

## Minimal server

```groovy
import com.intellisrc.web.*
import com.intellisrc.web.service.Service

WebService web = new WebService(port: 8080)
web.add(new Service(path: "/api/status", action: { -> [ok: true, time: SysClock.now] }))
web.add(new Service(path: "/api/user/:id", method: HttpMethod.GET, action: { Request req ->
    [id: req.params("id")]
}))
web.start()          // start(true) for background; also .stop() ; .join() ; isRunning()
```

## Service — parameters that matter

`path` (supports `:param`, `*` glob, regex), `method` (HttpMethod.GET/POST/PUT/DELETE..., default GET), `action` closure (`{ -> }`, `{ Request req -> }` or `{ Request req, Response res -> }`), `allow` closure (per-request authorization), `allowOrigin` (CORS), `contentType`, `download`/`downloadFileName`, `headers` (extra response headers), `maxAge` (browser cache seconds), `compress`, `beforeRequest`, `onError`.

What the action returns is serialized: Map/List -> JSON, String -> text, `File`/`byte[]` -> binary (mime auto-detected), `BufferedImage` -> image, `URL` -> proxied content. Throw `new WebException(HttpStatus.NOT_FOUND_404, "msg")` for errors; `Response` gives full control (`status(200)`, `type(...)`, `header(...)`, `redirect(...)`).

```groovy
new Service(
    path: "/api/upload", method: HttpMethod.POST,
    action: { Request req ->
        UploadFile f = req.files?.first()       // multipart uploads
        [size: f?.content?.length]
    }
)
```

## Serviciable — bundle services into one object

```groovy
class Api implements ServiciableMultiple {
    String path = "/api"                        // base path
    @Override List<Service> getServices() {[
        new Service(path: "/list",  action: { -> [1, 2, 3] }),
        new Service(path: "/echo",  method: HttpMethod.POST, action: { Request req -> req.body() })
    ]}
}
web.add(new Api())
// Single service: implement ServiciableSingle getService(); or extend SingleService.
```

### Auth — ServiciableAuth

```groovy
class PrivateApi implements ServiciableAuth {
    String path = "/private"
    String loginPath = "/login"                 // POST user/password -> session
    String logoutPath = "/logout"
    @Override AuthData onLogin(Request req, Response res) {
        def user = req.queryParams("user"), pass = req.queryParams("password")
        if (check(user, pass)) {
            return new AuthData(
                toStoreInServer: [user: user, level: Level.USER],   // saved in Session
                toSendToClient: [ok: true, redirect: "/dashboard"]  // returned as JSON
            )
        }
        return AuthData.empty                   // 401
    }
    @Override boolean onLogout(Request req, Response res) { true }
}
```

Services under an auth `Serviciable` require a valid session; use `allow: { Request r -> ... }` for per-service rules. `request.session()` gives `Session` with `attribute(key[, value])`, `id`, `invalidate()`. Ready-made: `LoginService`, `AutoConfigService` (edit AutoConfig values at `/cfg`), `LogService` (browse logs).

## Server-Sent Events

```groovy
class Ticker extends ServerSentEvent {
    String path = "/events"
}
web.add(new Ticker())
// anywhere: broadcast to all connected clients
ticker.broadcast("plain text")
ticker.broadcast([online: true], "status")     // data + event name
ticker.broadcast(new WebMessage([k: "v"]))    // WebMessage wraps data/event
```

## WebSockets

```groovy
class Chat extends WebSocketService {          // simple: broadcast to everyone
    String path = "/chat"
    @Override WebMessage onClientConnect(EventClient c) { new WebMessage([welcome: true]) }
    @Override WebMessage onMessage(EventClient c, WebMessage msg) {
        broadcast([from: c.id, text: msg.toString()])
        return new WebMessage([sent: true])
    }
    @Override WebMessage onClientDisconnect(EventClient c) { null }
}
// Low-level control: implement ServiciableWebSocket (getPath() + getWebSocketService())
// returning WebSocketBroadcastService with onClientConnect/onMessageReceived/onClientDisconnect
// closures and identifier: { Request r -> r.session().id }.
```

## WebClient — HTTP client

```groovy
WebClient wc = new WebClient("https://api.example.com")
wc.headers = ["Authorization": "Bearer token"] ; wc.timeout = 5000

wc.get([q: "groovy"], { String body -> })                          // query params + callback
wc.post([user: "john", pass: "x"], { Map json -> }, WebClient.Output.JSON)
// also put(...) and delete(...); async callbacks
wc.get([fmt: "json"], { String body -> }, [ "X-Api-Key": "k" ])    // extra headers overload
```

## WebService — common options

```groovy
new WebService(
    port: 8080, address: "0.0.0.0".toInet4Address(),
    threads: 20, minThreads: 2, timeout: 600000,          // ms
    resources: "res",                                      // static files path (String/File/Collection)
    embedded: false,                                       // resources inside the JAR
    ssl: new KeyStore(new File("keystore.p12"), "pass".toCharArray()),  // enables HTTPS
    protocol: Protocol.HTTP2,                              // HTTP | HTTP2 | HTTP3
    compress: true,                                        // gzip/brotli
    allowOrigin: "*",                                      // CORS
    log: true, accessLog: "access.log",                    // access logging
    filePolicy: { File f -> true }, pathPolicy: { String p -> true }   // resource guards
)
web.clearCache() ; web.clearCache("api/")                  // response cache invalidation
```

Config keys: `web.port`, `web.threads`, `web.log`, `web.log.dir`, `web.log.access/warn/notfound`, `web.collision.error` (duplicate paths throw unless false), `web.upload.force`, `web.charset`.

Generate a self-signed keystore for dev with `crypt` module `KeyStoreGenerator`.

## Gotchas

- HTTP/2 and HTTP/3 effectively require HTTPS/TLS; plain HTTP stays on HTTP/1.1 (`h2c` support is unreliable in browsers).
- Brotli falls back to gzip without the native libs.
- Don't mix an SSE/WebSocket service path with an HTTP service path — collisions throw at startup.
- Regex service paths need `samplePaths` in the Service so collision detection can work.
- Large binary responses (2.10.5+) stream rather than buffering in memory.
- Client disconnects are logged at DEBUG level — not errors.
