package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.etc.JSON
import com.intellisrc.etc.Mime
import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * This class is a wrapper around HttpURLConnection
 * This client is designed to send a body as string (either encoded in JSON or not)
 * or specify custom request headers
 * and process the return per line or all at once
 * For simple GET requests, use `myUrlWithParams.toURL().text` (Groovy)
 * @since 2024/05/24.
 */
@CompileStatic
class WebClient {
    final URL url
    String charset = "UTF-8"
    int timeout = Millis.SECOND_10
    Output eachLine = null
    protected boolean getParamsAsBody = false
    Map<String, String> headers = [:]

    static interface Output {
        void call(String out)
    }
    static interface JsonOutput extends Output {
        void call(Map json)
    }

    WebClient(String url) {
        this(url.toURL())
    }
    WebClient(URL url) {
        this.url = url
    }
    /**
     * Call url with a query
     * @param query
     */
    void get(Map query, Output onResponse = null, boolean sendParamsAsBody = false) {
        this.getParamsAsBody = sendParamsAsBody
        request(query, onResponse, HttpMethod.GET)
    }
    /**
     * POST using plain text body
     * @param data
     */
    void post(String data, Output onResponse = null) {
        request(data, onResponse, HttpMethod.POST)
    }
    /**
     * POST using JSON data
     * @param data
     */
    void post(Map data, Output onResponse = null) {
        request(data, onResponse, HttpMethod.POST)
    }
    /**
     * PUT using JSON data
     * @param data
     * @param onResponse
     */
    void put(Map data, Output onResponse = null) {
        request(data, onResponse, HttpMethod.PUT)
    }
    /**
     * DELETE using JSON data
     * @param data
     * @param onResponse
     */
    void delete(Map data, Output onResponse = null) {
        request(data, onResponse, HttpMethod.DELETE)
    }
    /**
     * Perform the request
     * @param data
     * @param onResponse
     * @param method
     */
    void request(Object data, Output onResponse = null, HttpMethod method) {
        if(method == HttpMethod.GET &&! getParamsAsBody) {
            String uriStr = url.toExternalForm()
            switch (data) {
                case Map:
                    uriStr += (uriStr.contains("?") ? "&" : "?") + (data as Map).toQueryString()
                    break
                default:
                    if(data.toString().contains("=")) { // Query String
                        uriStr +=  (uriStr.contains("?") ? "&" : "?") + data.toString()
                    } else {
                        Log.w("Not sure what to do with the arguments specified in the GET request: %s. Ignored", uriStr)
                    }
                    break
            }
            URI uri = uriStr.toURI()
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeout))
                .method(method.toString(), HttpRequest.BodyPublishers.noBody())
            if(! headers.isEmpty()) {
                headers.each {
                    builder.header(it.key, it.value)
                }
            }
            HttpRequest request = builder.build()
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
            if (onResponse) {
                switch (onResponse) {
                    case JsonOutput:
                        (onResponse as JsonOutput).call(JSON.decode(response.body()) as Map)
                        break
                    default:
                        onResponse.call(response.body())
                        break
                }
            }
        } else {
            HttpURLConnection con = (HttpURLConnection) url.openConnection()
            if (onResponse) {
                con.doOutput = true
            }
            con.requestMethod = method.toString()
            con.readTimeout = timeout
            con.connectTimeout = timeout
            String body
            //noinspection GroovyFallthrough
            switch (data) {
                case Map:
                case List:
                    con.setRequestProperty("Accept", Mime.JSON)
                    body = JSON.encode(data)
                    break
                default:
                    body = data.toString()
                    break
            }
            if (!headers.isEmpty()) {
                headers.each {
                    con.setRequestProperty(it.key, it.value)
                }
            }
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = body.getBytes(charset)
                os.write(input, 0, input.length)
            }
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.inputStream, charset))) {
                StringBuilder response = new StringBuilder()
                String responseLine
                while ((responseLine = br.readLine()) != null) {
                    if (eachLine) {
                        eachLine.call(responseLine.trim())
                    }
                    response.append(responseLine.trim())
                }
                br.close()
                if (onResponse) {
                    switch (onResponse) {
                        case JsonOutput:
                            (onResponse as JsonOutput).call(JSON.decode(response.toString()) as Map)
                            break
                        default:
                            onResponse.call(response.toString())
                            break
                    }
                }
                con.disconnect()
            }
        }
    }
}
