package com.intellisrc.web

import com.intellisrc.etc.JSON
import com.intellisrc.etc.Mime
import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

/**
 * This class is a wrapper around HttpURLConnection
 * This client is designed to send a body as string (either encoded in JSON or not)
 * and process the return per line or all at once
 * For simple GET requests, use `myUrlWithParams.toURL().text` (Groovy)
 * @since 2024/05/24.
 */
@CompileStatic
class WebClient {
    final URL url
    String charset = "UTF-8"
    Output eachLine = null

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
    void get(Map query, Output onResponse = null) {
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

    void request(Object data, Output onResponse = null, HttpMethod method) {
        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.doOutput = true
        String body
        //noinspection GroovyFallthrough
        switch (data) {
            case Map :
            case List :
                con.setRequestProperty("Accept", Mime.JSON)
                body = JSON.encode(data)
                break
            default:
                con.requestMethod = method.toString()
                body = data.toString()
                break
        }
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = body.getBytes(charset)
            os.write(input, 0, input.length)
        }
        try(BufferedReader br = new BufferedReader(
        new InputStreamReader(con.inputStream, charset))) {
            StringBuilder response = new StringBuilder()
            String responseLine
            while ((responseLine = br.readLine()) != null) {
                if(eachLine) {
                    eachLine.call(responseLine.trim())
                }
                response.append(responseLine.trim())
            }
            if(onResponse) {
                switch (onResponse) {
                    case JsonOutput:
                        (onResponse as JsonOutput).call(JSON.decode(response.toString()) as Map)
                        break
                    default:
                        onResponse.call(response.toString())
                        break
                }
            }
        }
    }
}
