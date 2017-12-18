package jp.sharelock.web

import jp.sharelock.etc.Log

import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat

/**
 * Simple HTTP server
 * The main difference with Serviciable* classes is that this code does not depend on Spark
 * This implementation is for basic static server and does not provide any advanced feature
 * present in common Web Servers
 *
 * @since 17/07/28.
 * based in: https://github.com/markwinter/simple-web-server/blob/master/WebServer.java
 */
@groovy.transform.CompileStatic
class HTTPServer {
    private boolean running = false
    //public
    int port = 80
    int timeout = 10 //seconds
    String root = "/var/www"
    String serverString = "WebServer/0.1"
    String indexFile = "index.html"
    ServerSocket serverSocket

    interface Action {
        Response call(Map<String,String> headers, Map<String,String> params, String output)
    }
    Action action = {
        return ""
    } as Action

    /**
     * Starts HTTP Server
     */
    void start() {
        serverSocket = new ServerSocket(port)
        serverSocket.setSoTimeout(timeout * 1000)
        Log.i( "HTTP Server [$serverString] started on port: $port , with root: $root")
        running = true

        while(running) {
            try {
                Socket connectionSocket = serverSocket.accept()
                new Thread(new WorkerRunnable(connectionSocket)).start()
            } catch(SocketException e) {
                running = false
            }
        }
        Log.i( "HTTP Server on port $port stopped")
    }

    /**
     * Stops HTTP Server
     */
    void stop() {
        running = false
        serverSocket.setSoTimeout(100)
        serverSocket.close()
    }

    /**
     * Response object
     */
    static class Response {
        int code = 200
        String mime = "html"
        String charset = "UTF-8"
        String content
    }
    /////////////////////////////////// STATIC /////////////////////////////////////////
    /**
     * Returns Mime type according to extension
     * @param mime
     * @return
     */
    static String getMime(final String mime) {
        switch(mime) {
            case "css": "text/css"; break
            case "gif": "image/gif"; break
            case "htm":
            case "html": "text/html"; break
            case "js": "application/javascript"; break
            case "json": "application/json"; break
            case "jpg":
            case "jpeg": "image/jpeg"; break
            case "mp4": "video/mp4"; break
            case "pdf": "application/pdf"; break
            case "png": "image/png"; break
            case "svg": "image/svg+xml"; break
            case "xml": "application/xml"; break
            case "zip": "application/zip"; break
            case "md":
            case "txt":
            default:
                "text/plain"; break
        }
    }

    /**
     * Generates basic HTTP headers
     * @param code
     * @param mime
     * @param length
     * @param out
     * @throws Exception
     */
    private void respondHeader(final int code, final String mime, final int length, DataOutputStream out) throws Exception {
        out.writeBytes("HTTP/1.0 " + code + " OK\r\n")
        out.writeBytes("Content-Type: " + getMime(mime) + "\r\n")
        out.writeBytes("Content-Length: " + length + "\r\n")
        out.writeBytes("Server: " + serverString)
        out.writeBytes("\r\n\r\n")
    }

    /**
     * Returns a response which will display error message depending on "code"
     * @param code
     * @param charset
     * @return
     */
    Response responseError(final int code, final String charset = "UTF-8") {
        def oFile = new File(root+"/"+code+".html")
        String fileContents
        if(oFile.exists()) {
            // Open file
            fileContents = oFile.getText(charset)
        } else {
            fileContents = "<h1>Error: "+code+"</h1><hr>"
            switch(code) {
                case 400 : fileContents += "Bad Request"; break
                case 401 : fileContents += "Unauthorized"; break
                case 402 : fileContents += "Payment Required"; break
                case 403 : fileContents += "Forbidden"; break
                case 404 : fileContents += "File not found"; break
                case 405 : fileContents += "Method Not Allowed"; break
                case 406 : fileContents += "Not Acceptable"; break
                case 407 : fileContents += "Proxy Authentication Required"; break
                case 408 : fileContents += "Request Timeout"; break
                case 409 : fileContents += "Conflict"; break
                case 410 : fileContents += "Gone"; break
                case 411 : fileContents += "Length Required"; break
                case 412 : fileContents += "Precondition Failed"; break
                case 413 : fileContents += "Payload Too Large"; break
                case 414 : fileContents += "URI Too Long"; break
                case 415 : fileContents += "Unsupported Media Type"; break
                case 416 : fileContents += "Range Not Satisfiable"; break
                case 417 : fileContents += "Expectation Failed"; break
                case 418 : fileContents += "I'm a teapot"; break
                case 421 : fileContents += "Misdirected Request"; break
                case 422 : fileContents += "Unprocessable Entity"; break
                case 423 : fileContents += "Locked"; break
                case 424 : fileContents += "Failed Dependency"; break
                case 426 : fileContents += "Upgrade Required"; break
                case 428 : fileContents += "Precondition Required"; break
                case 429 : fileContents += "Too Many Requests"; break
                case 431 : fileContents += "Request Header Fields Too Large"; break
                case 451 : fileContents += "Unavailable For Legal Reasons"; break
                case 500 : fileContents += "Internal Server Error"; break
                case 501 : fileContents += "Not Implemented"; break
                case 502 : fileContents += "Bad Gateway"; break
                case 503 : fileContents += "Service Unavailable"; break
                case 504 : fileContents += "Gateway Timeout"; break
                case 505 : fileContents += "HTTP Version Not Supported"; break
                case 506 : fileContents += "Variant Also Negotiates"; break
                case 507 : fileContents += "Insufficient Storage"; break
                case 508 : fileContents += "Loop Detected"; break
                case 510 : fileContents += "Not Extended"; break
                case 511 : fileContents += "Network Authentication Required"; break
                default  : fileContents += "Unknown Error"
            }
        }
        return new Response(code: code, mime: "html", content: fileContents)
    }

    /**
     * Returns a Response with file content
     * @param headers
     * @param params
     * @param charset
     * @return
     * @throws Exception
     */
    Response respondContent(final Map<String,String> headers, final Map<String,String> params, String charset = "UTF-8") throws Exception {
        assert headers["file"]
        assert headers["path"]
        assert headers["method"]

        String file = headers["file"]
        String path = headers["path"]
        String method = headers["method"]

        if (params.find { it.key == "charset" }) {
            charset = params["charset"]
        }

        if(method == "GET" || method == "POST") {
            // Return if trying to load file outside of web server root or if file contains potentially bad string
            Path p = Paths.get(root, path)
            if(!p.startsWith(root) || file.contains(";") || file.contains("*")) {
                Log.w("[400] Illegal connection : " + root + " , " +file)
                return responseError(400, charset)
            }

            def oFile = new File(root+path)
            String fileContents = ""
            if(oFile.exists()) {
                // Open file
                fileContents = oFile.getText(charset)
            } else if(params.isEmpty() || method == "GET") {
                Log.w( "[404] File doesn't exists: " + root+path)
                return responseError(404, charset)
            }
            return action.call(headers, params, fileContents)
        } else if(method == "HEAD") {
            return new Response()
        }
        Log.w( "[501] Method not found: " + method)
        return responseError(501, charset)
    }

    /**
     * Converts param=value&other=too into [param : value, other : too]
     * @param url
     * @return
     */
    static Map<String, String> parseURLParams(String url, String charset = "UTF-8") {
        url = url.substring(url.indexOf('?') + 1)
        def queryParams = url?.split('&')
        def mapParams = queryParams.collectEntries { param -> param.split('=').collect { String it -> URLDecoder.decode(it, charset) }}
        return mapParams
    }

    /**
     * Runnable class used to process multiple users at the same time
     */
    private class WorkerRunnable implements Runnable {

        protected Socket socket = null

        BufferedReader   input
        DataOutputStream output
        Map<String,String> headers = [:]
        Map<String,String> params  = [:]

        WorkerRunnable(final Socket connectionSocket) throws Exception {
            socket = connectionSocket
            input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()))
            output = new DataOutputStream(socket.getOutputStream())

            //inString = input.readLine() //Just get first line, ignoring everything else
            String line
            String first = ""
            while((line = input.readLine()).length() != 0) {
                // Handle first line of connection
                if(first.isEmpty()) {
                    first = line
                    String[] parts = line.split(" ")
                    headers["method"] = parts[0] ?: "GET"
                    // Build basic header in case is empty
                    if (!parts[1]) {
                        parts[1] = ""
                        parts[2] = "HTTP/1.0"
                    }
                    String path
                    String query = ""
                    if (parts[1].contains('?')) {
                        String[] subParts = parts[1].split('\\?', 2)
                        path = subParts[0]
                        if(path.endsWith('/')) {
                            path += indexFile
                        }
                        query = subParts[1]
                    } else if(parts[1].isEmpty()) {
                        path = "/" + indexFile
                    } else {
                        path = parts[1]
                    }
                    String file = path.substring(path.lastIndexOf('/') + 1)
                    String ext = file.substring(file.indexOf(".") + 1)

                    headers["uri"] = parts[1]
                    headers["query"] = query
                    headers["path"] = path
                    headers["file"] = file
                    headers["ext"] = ext.toLowerCase()
                    headers["protocol"] = parts[2]
                } else {
                    String[] parts = line.split(":", 2) //limit: will only split on the first occurrence
                    headers[parts[0].toLowerCase()] = parts[1].trim()
                }
            }

            StringBuilder payload = new StringBuilder()
            while(input.ready()){
                payload.append((char) input.read())
            }
            if(payload) {
                params = parseURLParams(payload.toString())
            } else if(headers["query"]) {
                params = parseURLParams(headers["query"])
            }

            Calendar cal = Calendar.getInstance()
            cal.getTime()
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
            String time = "[" + sdf.format(cal.getTime()) + "] "
            String ip = socket.getInetAddress().getHostAddress()
            // Add additional headers
            headers["remote"] = ip
            headers["time"]   = time
            headers["header"] = first
            headers["url"] = "http://" + headers["host"] + headers["uri"]
            headers["hostname"] = headers["host"].contains(':') ? headers["host"].split(':')[1] : headers["host"]
        }

        void run() {
            try{
                if(headers) {
                    Response res = respondContent(headers, params)
                    byte[] outBytes = res.content.getBytes(res.charset)
                    respondHeader(res.code, res.mime, outBytes.length, output)
                    output.write(outBytes)
                    Log.i( headers["remote"] + " " + headers["time"] + ' "' + headers["header"] + '" '+ res.code + " " + outBytes.length +' "' + (headers["user-agent"] ?: "Unknown") + '"')
                }
                output.flush()
                output.close()
                input.close()

            } catch (Exception e) {
                Log.e( "Error flushing and closing: ",e)
            }
        }
    }
}
