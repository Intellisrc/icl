# WEB Module (ICL.web)

Create restful HTTP (GET, POST, PUT, DELETE, etc), WebSocket or Server-Sent Event application services.
Manage JSON data from and to the server easily. It is build on top of [Jetty](https://github.com/eclipse/jetty.project) 

[JavaDoc](https://intellisrc.gitlab.io/common/#web)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/web)

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

`WebService` manages the server, which handles one or more `Service`(s). A `Service` executes an `Action`.

### Service

One advantage of using this library instead of using Jetty directly, is that you can 
easily create services and make them totally reusable among your projects. 
For example, to create one service:

```groovy
class VersionService extends Service {
     String path = "/version"
     Action action = {
        [ version : Version.get() ]
     }
}
```

Then add it to your web server and start it:
```groovy
new WebService(port: 8080, resources: "res")
        .add(new VersionService())
        .start()
```

The `VersionService` (above example) could be written as well (easier to use in Groovy):

```groovy
class VersionService extends SingleService {
    Service getService() {
        return new Service(
            path: "/version",
            action: {
                return [
                    version: Version.get()
                ]
            }
        )
    }
}
```

If you prefer to use an interface, you can use `ServiciableSingle`:

```groovy
class VersionService implements SeviciableSingle {
    /* The code is exactly the same as above */
}
```

`SingleService` abstract class or `ServiciableSingle` interface will only return a single service, 
to return a list of services use `Services` (abstract class) or `ServiciableMultiple` (interface):

```groovy
class CRUDService extends Services {
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

`Service` can also take several others properties:
```groovy
new Service(
    action              : { },                   // Closure that will return an Object (usually Map) to be converted to JSON as response
    allow               : { true } as Allow,     // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    allowOrigin         : null,                  // By default only localhost is allowed to perform requests. This will set "Access-Control-Allow-Origin" header.
    allowType           : "",                    // By default it accepts all mime types, but you can set to accept only specific types like `application/json` (default `*/*`)
    cacheExtend         : false,                 // Extend time upon read (similar as sessions)
    cacheTime           : 0,                     // Seconds to store action in Server's Cache // 0 = "no-cache" Browser Rule: If true, the client must revalidate ETag to decide if download or not. Cache.FOREVER : forever
    charSet             : "UTF-8",               // Output charset (default: UTF-8)
    compress            : false,                 // Whether to compress or not the output
    compressSize        : false,                 // If true, when compressed will buffer the output to report size
    contentType         : "",                    // Content Type, for example: Mime.getType("png") or "image/png". (default : auto)
    download            : false,                 // Specify if instead of display, show download dialog
    downloadFileName    : "",                    // Use this name if download is requested
    etag                : { "" } as ETag,        // Method to calculate ETag if its different from default (set it to null, to disable automatic ETag)
    headers             : [:],                   // Extra headers to the response. e.g. : "Access-Control-Allow-Origin" : "*"
    isPrivate           : false,                 // Browser Rule: These responses are typically intended for a single user
    maxAge              : 0,                     // Seconds to suggest to keep in browser
    method              : Method.GET,            // HTTP Method to be used
    noStore             : false,                 // Browser Rule: If true, response will never cached (as it may contain sensitive information)
    path                : "",                    // URL path relative to parent
)
```

### Action

`Action` is a group of interfaces with optional return arguments, for example:

 * (empty) // The most simple `Action` with no `Request` or `Response` 
 * `Request` request ->
 * `Response` response ->
 * `Request` request, `Response` response ->
 * `FileUpload` file, `Request` request ->
 * `List<FileUpload>` files, `Request` request ->
 * (and many other combinations: in Groovy, the order of the arguments is not relevant)

`Action` return value will be sent to the client. By default, it will expect to return a `Map` or a `Collection` which
itself is converted into `JSON`. Depending on the object you are returning, you may need to change the `contentType`
property (it will try to guess it if you don't specify it), for example:

| Object        | Content Type                                         |
|---------------|------------------------------------------------------|
| String        | automatic: plain, xml, html, svg, etc                |
| List / Map    | "text/json" unless "Mime.YAML" is used               |
| File          | automatic: depending on name and content             |
| BufferedImage | automatic: JPEG or PNG (if transparency is detected) |
| URL           | automatic: Will "proxy" content and type from remote |
| byte[]        | "application/octet-stream"                           |
| OutputStream  | "application/octet-stream"                           |

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
        return File.get("resources", "private", "videos", "vid001.mp4")
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
            File targetFile = File.get("resources", "upload", file.originalName)
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

### Path

`path` can be either a string or a `Pattern` object (RegExp). Paths don't need to start with a slash (is automatically added).

For example:

You can use params:
```groovy
new Service(
    path : "/user/:id/",
    action: {
        Request request ->
            String id = request.params("id")
            return "ok"
    }
)
```

You can use `splat`:
```groovy
new Service(
    path : "/user/*/panel",
    action: {
        Request request ->
            String splat = request.splat()[0]
            return "ok"
    }
)
```

Trailing slash can be optional:
```groovy
new Service(
    path : "/main/page/?", 
    action: {
        Request request ->
            return "ok"
    }
)
```

You can use regular expressions to define and validate paths and create groups:
```groovy
new Service(
    path : /^(\d+)-(\w+)\.([a-z]+)$/,
    action: {
        Request request ->
            int id = request.params(1) as int
            String title = request.params(2)
            String ext = request.params(3)
            return "ok"
    }
)
```

**NOTE** : If you use `Java`, you can specify your path as RegExp by starting it with `~/`, for example:

```java
class PrepareService {
    public static void main(String[] args) {
        Service service = new Service();
        service.path ="~/^(\\d+)-(\\w+)\\.([a-z]+)$/";
        /* ... */
    }
}
```

You can use named groups in your regular expressions (in this example we are using a `Pattern` object instead of String
just for illustration):
```groovy
new Service(
    path : ~/u(?<uid>\d+)/,  // equivalent to Pattern.compile("/u(?<uid>\\d+)/")
    action: {
        Request request ->
            int id = request.params("uid") as int
            return "ok"
    }
)
```

### Downloading files

```groovy
new Service(
    path : "/download/file",
    download : true, // Set this property and `WebService` will handle the rest
    action: {
        return File.get(File.userDir, "resources", "some.file.pdf")
    }
)
```

By setting `download` to `true`, the needed headers will be added into the response, for example: 
`Content-Disposition`, `Content-Encoding`, `Content-Length`, etc

#### Customizing the Output

As in the example above, `WebService` will automatically set the required headers so the file
can be downloaded (instead of displayed on the browser). But there are some special cases
in which you may want to customize the name of the file each time (without changing the 
original file name). In such cases, you can customize the output:

```groovy
new Service(
    path : "/download/custom",
    action : {
        Request request ->
            String downloadName = request.queryParams("name") ?: "default.file"
            File toDownload = File.get(File.userDir, "resources", "last.file")
            return new ServiceOutput(
                content     : toDownload.bytes,
                fileName    : downloadName,
                size        : toDownload.size(),
                etag        : toDownload.bytes.md5()
            )
    }
)
```

### Compressing the Output

You can request the compression of your output by setting the property `compress` to `true`. Additionally, it will
set the required header (`Content-Encoding`).

```groovy
new Service(
    path: "/download.txt",
    download : true,
    compress : true,
    action : {    
        return File.get("~/report.csv")
    }
)
```

If you don't use compression, `Content-Length` will always be added automatically, however, when compression is 
requested, the output is compressed in chunks at the same time it is being downloaded, so there is no easy
way to calculate the final size until all the data has been served (by which point setting the `Content-Length` header
is useless). We can compress the output before serving it, but this will require more resources (specially memory or cpu), 
and may delay the response time. If you want to include the size anyway (to show the progress bar in the browser), 
you can specify the property : `compressSize` to `true`:

```groovy
new Service(
    path: "/download.txt",
    download : true,
    compress : true,
    compressSize : true,
    action : {    
        return File.get("~/report.csv")
    }
)
```

By default, it will use `GZip` to compress the output, but you can use [Brotli](https://en.wikipedia.org/wiki/Brotli) compression,
which is [widely supported](https://caniuse.com/brotli) by modern browsers.

In order to use `Brotli` compression, you only need to add the
[com.nixxcode.jvmbrotli](https://mvnrepository.com/artifact/com.nixxcode.jvmbrotli) dependency
in your project (Gradle, Maven, etc).

**NOTE**: If you are using Gradle, you may need to also include the native library according to your
system architecture.

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

### HTTPS 

To enable HTTPS, you will need a certificate. You can generate one and create the key store in a single command:

```bash
# It will ask you some basic information
keytool -genkey -keyalg RSA -alias localhost -keystore keystore.jks -storepass yourpasswordhere -validity 365 -keysize 2048
```

If you already have a valid certificate, you can create the key store with these commands instead:

```bash
# First export your x.509 certificate and key to pkcs12:
openssl pkcs12 -export -in example.crt -inkey example.key -out example.p12 -name example.com -CAfile ca.crt -chain
# If you use Let's Encrypt, use this command instead:
# openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out example.p12 -name example.com

#NOTE#: Be sure you set a password or you won't be able to import it

# Then, create or import into the key store:
keytool -importkeystore -deststorepass yourpasswordhere -destkeypass yourpasswordhere -destkeystore keystore.jks \
        -srckeystore example.p12 -srcstoretype PKCS12 -srcstorepass thePasswordYouSetInTheStepBefore \
        -deststoretype JKS -alias example.com
```

That command will generate a file named `keystore.jks` (or the name you specified). 
Feel free to move that file anywhere you want.
Then, use the file and your password in your `WebService` initialization:

```groovy
new WebService(
        port: 443, 
        resources: "res",
        ssl : new KeyStore(
            File.get("private", "keystore.jks"), 
            "yourpasswordhere"
        ),
    ).add(new Service(
        path : "/ssl",
        action : { "ok" }
    )).start()
```

Opening `https://localhost:443/ssl` should display "ok" (after showing a security warning, as it is self-signed).

### HTTP/2

You can enable HTTP/2 protocol for your services by setting the `http2` property to `true`:

```groovy
new WebService(
    port: 443,
    resources: "res",
    ssl: new KeyStore(
        File.get("private", "keystore.jks"),
        "yourpasswordhere"
    ),
    http2 : true
).add(new Service(
    path : "/admin",
    action : { "ok" }
)).start()
```

**NOTE** : Must browsers require HTTPS to be enabled in order to use HTTP/2. 

## WebSocket Server

If you prefer to use a WebSocket server, you can create one by
implementing `ServiciableWebSocket` interface:

This WebSocket Server implementation was developed using the Jetty 
WebSocket library.

```groovy
class MyWebSocketService implements ServiciableWebSocket {
    // Replace clients when same ID is received
    boolean replaceOnDuplicate = true
    // If you want, you can keep the list of connected clients:
    synchronized Set<String> clients = []
    // If you want messages to be originated from the server:
    WebSocketService.MsgBroadCaster broadCaster

    /**
     * This method is to create a unique User ID. 
     */
    @Override
    String getUserID(Map<String, List<String>> queryParams, InetAddress inetAddress) {
        /*
            You can set the IP address as ID, but that limits a single user
            in a remote network (if they share the same public IP).
            To allow multiple IP addresses for a single user return for example:
               queryParams.user + "-" + inetAddress.hostname 
         */
        return queryParams.user
    }
    
    @Override
    WSMessage onConnect(Session session) {
        Log.i("Client connected: %s", session.userID)
        clients << session.userID
        // It is not required, but if you want to send a message to the user
        // you can send it here, otherwise, return null
        return new WSMessage(session.userID, [online: true])
    }
    
    @Override
    WSMessage onDisconnect(Session session, int statusCode, String reason) {
        Log.i("Client disconnected: %s (reason: %s)", session.userID, reason)
        clients.remove(session.userID)
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

    /**
     * Sends a Message to a client
     * @param sessionId
     * @param data
     */
    boolean sendMessageTo(String id, final Map data) {
        boolean sent = false
        broadCaster.call(new WSMessage(id, data), {
            // On success
            sent = true
        }, {
            // On failure
            sent = false
        })
        return sent
    }

    /**
     * Sends a Message to all clients
     * @param data : Map
     */
    boolean sendMessage(final Map data) {
        boolean sent = false
        clients.each {
            String id ->
                Log.v("Sent to: %s , message: %s", id, data.toString() ?: "")
                sent = sendMessageTo(id, data)
        }
        return sent
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

## WebSocketSecure (WSS)

You can add a SSL certificate to enable `wss` protocol:

**NOTE**: To generate a certificate or import an existing one, please see [https](#https) section above.

```groovy
new WebService(
    port: 9999,
    ssl : new KeyStore(
        File.get("private", "keystore.jks"),
        "yourpasswordhere"
    )
).add(new MyWebSocketService(timeout: 600, maxSize: 1024))
 .start()
```

Then you can access that in: `wss://localhost:9999/`

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

## Troubleshooting

If you compile your project using gradle and `shadowJar` to produce a jar, you may face
an `ArrayIndexOutOfBoundsException` due to a missing `HttpFieldPreEncoder`.
The reason this is happening is that the compilation is overwriting instead of appending
the `org.eclipse.jetty.http2:http2-hpack` encoder in the file:
`META-INF/services/org.eclipse.jetty.http.HttpFieldPreEncoder`. 
In order to fix this, you need to specify that those service files should be merged:

```groovy
shadowJar {
    mergeServiceFiles()
}
```
