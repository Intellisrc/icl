# WEB Module

Create restful HTTP (GET, POST, PUT, DELETE, etc) or WebSocket application services. 
Manage JSON data from and to the server easily. It is build on top of Spark-Java Web Framework, 
so it is very flexible and powerful, but designed to be elegant and easier to use.

## WebService (HTTP Web Server)

The easiest way to create a web server. For example:

```groovy
new WebService(port: 8080, resources: "res").start()
```
That one-liner will start a web server on port 8080 and serve static files located on "res/" directory.

`WebService` options:

```groovy
new WebService(
    // Commonly used:    
    port        : 80,       // Port in which the Web Server will listen
    resources   : "",       // Path location or File where static resources exists
    threads     : 20,       // Number of threads that will allow to process at the same time
    // Cache related:    
    cacheTime   : 0,        // Default amount of time to keep in memory requests and resources
    eTagMaxKB   : 1024,     // Below this amount of KB, we will calculate eTag automatically
    // Other:    
    embedded    : false,    // Turn to true if resources are inside jar
    allowOrigin : ""        // apply by default to all
)
```

### Services (ServiciableSingle, ServiciableMultiple)

One advantage of using this wrapper against using Spark-Java Web Framework, is that you can 
easily create services and add them to your web server, making them totally recyclable among
your projects. For example, to create one service:

```groovy
class VersionService implements ServiciableSingle {
    Service getService() {
        return new Service(
                path : "/version",
                action : {
                    return [
                        version : Version.get()
                    ]
                }
        )
    }
}
// Then when you launch your web server:
new WebService(port: 8080, resources: "res")
        .add(new VersionService())
        .start()
```

`ServiciableSingle` interface will only return a single service, to return a list of services
use `ServiciableMultiple`:

```groovy
class CRUDService implements ServiciableMultiple {
    /**
     * Prefix path for all services in this class
     */
    @Override
    String path = "/api/users"
    
    List<Service> getServices() {
        return [
            new Service(
                method : Method.POST,
                action : {
                    /* .. action here .. */
                }
            ),
            new Service(
                method : Method.PUT,
                action : {
                    /* .. action here .. */
                }
            ),
            new Service(
                method : Method.DELETE,
                action : {
                    /* .. action here .. */
                }
            )
        ]
    }
}
```
`WebService` manages the server, which handles one or more `Service`(s). A `Service` executes an `Action`.

`Service` can also take several others properties:
```groovy
new Service(
    // Required:    
    action        : { } as Action,        // Closure that will return an Object (usually Map) to be converted to JSON as response
    // Most common:
    path          : "",                   // URL path relative to parent (inherited from `Spark` : check Spark documentation)
    method        : Method.GET,           // HTTP Method to be used
    contentType   : "",                   // Content Type, for example: Mime.getType("png") or "image/png". (default : auto)
    allow         : { true } as Allow,    // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    // Cache related:    
    cacheExtend   : false,                // Extend time upon read (similar as sessions)
    cacheTime     : 0,                    // Seconds to store action in Server's Cache // 0 : "no-cache" Browser Rule: If true, the client must revalidate ETag to decide if download or not. Cache.FOREVER : forever
    maxAge        : 0,                    // Seconds to suggest to keep in browser
    noStore       : false,                // Browser Rule: If true, response will never cached (as it may contain sensitive information)
    etag          : { "" } as ETag,       // Method to calculate ETag if its different from default (set it to null, to disable automatic ETag)
    // Less common:    
    isPrivate     : false,                // Browser Rule: These responses are typically intended for a single user
    download      : "",                   // Specify if instead of display, show download dialog with the name of the file.
    allowOrigin   : null,                 // By default only localhost is allowed to perform requests. This will set "Access-Control-Allow-Origin" header.
    headers       : [:]                   // Extra headers to the response. e.g. : "Access-Control-Allow-Origin" : "*"
)
```
`Action` is a group of interfaces with optional return arguments, for example:

 * (empty) // The most simple `Action` with no `Request` or `Response` (which are `Spark` classes)
 * `Request` request ->
 * `Response` response ->
 * `Request` request, `Response` response ->
 * `FileUpload` file, `Request` request ->
 * `List<FileUpload>` files, `Request` request ->
 * (and many other combinations: in Groovy, the order of the arguments is not relevant)

`Action` return value will be sent to the client. By default, it will expect to return a `Map` or a `Collection` which
itself is converted into `JSON`. Depending on the object you are returning, you may need to change the `contentType`
property (it will try to guess it if you don't specify it), for example:

| Object        | Content Type                                          |
|---------------|-------------------------------------------------------|
| String        | automatic: plain, xml, html, svg, etc                 |
| List / Map    | "text/json" unless Mime.getType("yaml") is used       |
| File          | automatic: depending on name and content              |
| BufferedImage | automatic: JPEG or PNG (if transparency is detected)  |
| URL           | automatic: Will "proxy" content and type from remote  |
| byte[]        | "application/octet-stream"                            |

If you want to make the `File` downloadable (by the client), you don't need to specify any `contentType`, 
but you will need to set the property `download` to `true`. Now if you want to "serve" or "stream" a `File`
(like a video), you may need to specify the `contentType` if `WebService` can not guess it correctly. 

For example: 

```groovy
// Get the list of users as JSON array:
new Service(
    path : "/users/list/",
    action : {
        return Users.all().toList() 
    }
)
// Get video file from private location using alias:
new Service(
    path : "/videos/intro.mp4",
    //contentType : Mime.getType("mp4"), <-- not required unless it can not be guessed
    action : {
        return SysInfo.getFile("resources", "private", "videos", "vid001.mp4")
    }
)
// Get user information as YAML
new Service(
    path : "/user/:id/", 
    contentType : Mime.getType("yaml"), // Because Map is returned as JSON by default, you need to specify it
    action : {
        Request request ->
            int id = request.params("id") as int
            User user = Users.get(id)
            return user.toMap()
    }
)
// Simple "Proxy" mode : download a serve a request from a remote location:
// It will set contentType and headers automatically
new Service(
    path : "/google/",
    action : {
        return "https://google.com/".toURL()
    }    
)
// Upload a file
new Service(
    path : "/upload",
    action: {
        UploadFile file ->
            // UploadFile extends File with additional fields:
            Log.i("Uploaded file temporally location is: %s", file.absolutePath)
            Log.i("Uploaded file original name is : %s", file.originalName)
            Log.i("HTML input field name used to upload is: %s", file.inputName)
            // Move file from temporally directory to another location
            File targetFile = SysInfo.getFile("resources", "upload", file.originalName)
            file.moveTo(targetFile)
            return [ uploaded : targetFile.exists() ]
    } 
)
// Upload multiple files at once:
new Service(
    path : "/upload/many",
    action: {
        List<UploadFile> files ->
            files.each {
                UploadFile file -> 
                    /* ... same as previous example ... */
            }
            return [ uploaded : ok ]
    }
)
```

### Authentication (ServiciableAuth)

This interface is used to create and manage sessions in order to enable authentication.

#### Example

```groovy
class AuthService implements ServiciableAuth {
    String path = "/private"         // general prefix
    String loginPath = "/login"      // POST data to    : /private/login
    String logoutPath = "/logout"    // GET request to  : /private/logout

    /**
     * List of available type of users
     */
    static enum Level {
        GUEST, USER, ADMIN
    }
    /**
     * Allow Admin rule (used in `Service`)
     */
    static final Service.Allow admin = {
        Request request ->
            return request.session() ? getLevel(request) >= Level.ADMIN : false
    } as Service.Allow
    /**
     * Main entrance point to authenticate
     * @param request
     * @param response
     * @return
     */
    @Override
    Map<String,Object> onLogin(final Request request, final Response response) {
        String user = request.queryParams("user") ?: ""
        String pass = request.queryParams("password") ?: ""

        Level level = Level.GUEST
        if(user.isEmpty() || pass.isEmpty()) {
            Log.w("User or Password is empty.")
        } else {
            level = Users.auth(user, pass) // Authentication logic
        }
        // Information to store in a session:
        Map session = [
                user    : user,
                level   : level,
                ip      : request.ip(),
                since   : SysClock.now
        ]
        // Whatever we return here, it will stored as session:
        return level > Level.GUEST ? session : [:]
    }

    /**
     * Return User Level according to request
     * This is an example on how to read the session
     * @param request
     * @return
     */
    static Level getLevel(Request request) {
        Level level = Level.GUEST
        String strLevel = request?.session()?.attribute("level")?.toString() ?: ""
        if (strLevel) {
            level = strLevel.toUpperCase() as Level
        }
        return level
    }

    /**
     * On logout
     * @param request
     * @param response
     * @return
     */
    @Override
    boolean onLogout(final Request request, final Response response) {
        return true
    }
}
```
If the login is successful, by default, the server will respond with something like:

```json
{
  "id": "node0auU7v3dL4ysA9Vhzf7bsjsTC",
  "ok": true
}
```
in which `id` is the session ID which should match the value under the cookie `JSESSIONID`.

If you want to return additional information upon login, add a `response : [:]` into your session `Map`:
```groovy
    // See example above...
    Map<String,Object> onLogin(final Request request, final Response response) {
        /* ... */
        // Information to store in a session:
        Map session = [
                user    : user,
                level   : level,
                ip      : request.ip(),
                since   : SysClock.now,
                // Additionally information to return on login success: (must be a Map)
                response : [
                    level       : level.toString(),
                    redirectTo  : level == Level.ADMIN ? "/admin" : "/user"
                ]
        ]
        // Whatever we return here, it will stored as session:
        return level > Level.GUEST ? session : [:]
    }
```
When you return an empty `Map` as session, it will return status `401 (Unauthorized)`.

And don't forget to add it to your `WebService`:
```groovy
new WebService(port: 8080, resources: "res")
        .add(new VersionService())
        .add(new AuthService())
        .start()
```

Then in the `Service` you can specify:

```groovy
new Service(
    path   : "/admin/action",   
    allow  : AuthService.admin,
    action : {
        Request request ->
            String username = request.session()?.attribute("user")?.toString()
            /* ... */
    }    
)
```

### HTTPS (ServiciableHTTPS)

Although I recommend using a reverse proxy server (like HAProxy or Apache) to
handle HTTPS connections, you can add HTTPS support without the need of extra
packages. 

#### Example

```groovy
/**
 * Example of HTTPS Service
 *
 * 1. Issue a certificate (self-signed or not)
 * 2. Execute: java-cert-importer.sh (inside res/public/) 
 *      or follow: https://stackoverflow.com/a/31133183/196507
 *      or : http://sparkjava.com/documentation.html#enable-ssl
 * in order to import keystore file into trusted certs
 */
class SSLService implements ServiciableHTTPS, ServiciableSingle {
    //----- From ServiciableHTTPS:
    
    // Must be absolute path. Do not store it in public directory
    @Override
    String keyStoreFile = SysInfo.getFile(SysInfo.userDir, "res", "key.store").absolutePath
    // The password for the keystore
    @Override
    String password = "e7LcrHoWe3iuogAiwPdTCzAk"

    //----- From ServiciableSingle:
    @Override
    String path = "/admin"

    @Override
    Service getService() {
        return new Service(
            action: {
                /* .. */
            }
        )
    }
}
```

Do not forget to add it to your `WebService`:

```groovy
new WebService(port: 443, resources: "res")
        .add(new SSLService())
        .start()
```

## WebSocket Server

If you prefer to use a WebSocket server, you can create one by
implementing `ServiciableWebSocket` interface:

This WebSocket Server implementation was developed using the Jetty 
WebSocket library.

```groovy
class MyWebSocketService implements ServiciableWebSocket {
    // Replace clients when same ID is received
    boolean replaceOnDuplicate = true
    /**
     * This method is to create a unique User ID. 
     */
    @Override
    String getUserID(Map<String, List<String>> queryParams, InetAddress inetAddress) {
        /*
            You can set the IP address as ID, but that limits a single user
            in a remote network (if they share the same public IP). 
         */
        return queryParams.user
    }
    
    @Override
    WSMessage onConnect(Session session) {
        Log.i("Client connected: %s", session.userID)
        // It is not required, but if you want to send a message to the user
        // you can send it here, otherwise, return null
        return new WSMessage(session.userID, [online: true])
    }
    
    @Override
    WSMessage onDisconnect(Session session, int statusCode, String reason) {
        Log.i("Client disconnected: %s (reason: %s)", session.userID, reason)
        // It is not required, but if you want to send a message to the user
        // you can send it here, otherwise, return null
        return null
    }
    /**
     * This is where the main communication happens:
     * We receive a message, then we send a response
     */
    @Override
    WSMessage onMessage(Session session, String message) {
        String response = ""
        // If message is JSON, decode it: 
        // Map map = JSON.decode(message) as Map
        switch (message) {
            case "ping" : response = "pong"
                /* .. */
        }
        return new WSMessage(session, [
            msg  : response,
            time : SysClock.now.YMDHms
        ])
    }

    @Override
    void onClientsChange(List<String> list) {
        // Here the list has the new list of clients connected (IDs)
        // This is triggered each time a client connects or disconnects
        Log.i("Connected clients: %d", list.size())
        list.each {
            // sendMessageTo is provided by `ServiciableWebSocket`
            sendMessageTo(it, [ users : list ])
        }
    }

    @Override
    void onError(Session session, String errorMessage) {
        Log.w("There was an error: %s (user: %s)", errorMessage, session.userID)
    }
}
```

Finally, add it to your `WebService` :

```groovy
// You can specify connection timeout in seconds and messages maximum size in KB)
new WebService(port: 9999)
        .add(new MyWebSocketService(timeout: 600, maxSize: 1024))
        .start()
```

**NOTE** : Due to technical limitations at the moment, it is not possible to 
mix WebSocket services with HTTP services. You may need to create two instances
of `WebService` in different ports.

You can also set `timeout` and `maxSize` in your `config.properties` file:

```properties
# Set timeout to 10 minutes: (default 5 minutes)
websocket.timeout=600
# Set maximum message size to 1MB: (default 64Kb)
websocket.max.size=1024
```

## WebSocket Client

This WebSocket Client implementation was developed using the Jetty
WebSocket library, its usage is very simple:

#### Example
```groovy
WebSocketServiceClient wssc = new WebSocketServiceClient(
    hostname: "example.com",
    port : 8888,
    path : "ws/chat?user=myuser"
)
// Connect and listen for responses:
wssc.connect({
    Map msg ->  // message received from server:
        /** do something **/
})
// Send a message to the server (String)
wssc.sendMessage("ping")
// Send a message to the server (Map)
wssc.sendMessage([ ping : true ])
// Exit:
wssc.disconnect()
```
