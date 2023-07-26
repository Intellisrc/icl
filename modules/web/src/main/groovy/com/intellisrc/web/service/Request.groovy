package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic
import jakarta.servlet.ServletRequest
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Request as JettyRequest

import java.util.concurrent.ConcurrentHashMap

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Request extends JettyRequest {
    static final String X_FORWARDED_FOR = "X-Forwarded-For"
    static String SESSION_ID = "JSESSIONID"
    //protected final Session requestSession = new Session()
    final ConcurrentHashMap<String, String> pathParameters = new ConcurrentHashMap<>()
    protected String splat = ""

    Request(ServletRequest request) {
        this(getBaseRequest(request))
    }
    Request(JettyRequest request) {
        super(request.httpChannel, request.httpInput)
        importFrom(request, JettyRequest)
    }
    /**
     * Improve properties from another class
     * @param fromClass
     */
    void importFrom(Object request, Class fromClass) {
        fromClass.declaredFields.each {
            try {
                it.setAccessible(true)
                Object value = it.get(request)
                it.set(this, value)
            } catch (IllegalAccessException ignore) {
                // Handle the exception as needed
            }
        }
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    String ip() {
        return ip
    }
    String getIp() {
        return headerNames.toList().contains(X_FORWARDED_FOR) ? headers(X_FORWARDED_FOR) : remoteAddr
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    String uri() {
        return requestURI
    }
    /**
     * Backward compatibility with Spark
     * Get request host name
     * @return
     */
    String host() {
        return host
    }
    /**
     * Get request host
     * @return
     */
    String getHost() {
        return getHeader("host") ?: "localhost"
    }
    /**
     * Backward compatibility with Spark
     * Return scheme
     * @return
     */
    String scheme() {
        return getScheme()
    }
    //------------- HEADERS / ATTRIBUTES --------------
    String headers(String key) {
        return getHeader(key)
    }
    String attribute(String key) {
        return getAttribute(key)
    }
    void attribute(String key, Object value) {
        setAttribute(key, value)
    }
    //------------- PATH PARAMS ---------------
    void setPathParameters(Map<String, String> params) {
        params.keySet().each {
            if(it == "splat") {
                splat = params[it]
            } else {
                pathParameters.put(it, params[it])
            }
        }
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    String params(String key) {
        return getPathParam(key)
    }
    /**
     * Get a path parameter
     * @param key
     * @return
     */
    String getPathParam(String key) {
        return pathParameters.containsKey(key) ? pathParameters[key] : ""
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    Set<String> params() {
        return pathParameters.keys().toSet()
    }
    /**
     * Get the list of path parameters
     * @return
     */
    Map<String, String> getPathParams() {
        return Collections.unmodifiableMap(pathParameters)
    }
    /**
     * True if it has path parameters
     * @return
     */
    boolean hasPathParams() {
        return ! pathParameters.isEmpty()
    }
    /**
     * Backward compatibility with Spark
     * Get all covered by "*"
     * @return list of strings of each part of the path
     */
    List<String> splat() {
        return splat.tokenize("/")
    }
    //------------- QUERY PARAMS ---------------
    /**
     * Backward compatibility with Spark
     * @return
     */
    String queryParams(String key) {
        return getQueryParam(key)
    }
    /**
     * Get a query parameter
     * @param key
     * @return
     */
    String getQueryParam(String key) {
        return getParameter(key)
    }
    /**
     * Get a query parameter that contains a list
     * @param key
     * @return
     */
    List<String> getQueryParamAsList(String key) {
        return parameterMap.get(key).toList()
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    List<String> queryParams() {
        return getQueryParams().keySet().toList()
    }
    /**
     * Get the list of query parameters
     * @return
     */
    Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(parameterMap.collectEntries {
            [(it.key): it.value.join(",")]
        })
    }
    /**
     * True if it has query params
     * @return
     */
    boolean hasQueryParams() {
        return ! queryParams.empty
    }
    //------------ SESSION ------------
    /**
     * Return HTTPSession wrapper
     * @return
     */
    Session session() {
        String id = cookies.toList().find { it.name == SESSION_ID }?.value ?: UUID.randomUUID().toString()
        return new Session(id, session)
    }
    //------------ OTHER --------------
    /**
     * Backward compatibility with Spark
     * @return
     */
    String body() {
        return getBody()
    }
    String getBody() {
        return Bytes.toString(bodyAsBytes, getCharacterEncoding())
    }
    byte[] getBodyAsBytes() {
        byte[] bodyAsBytes = null
        try {
            bodyAsBytes = IOUtils.toByteArray(getInputStream())
        } catch (Exception e) {
            Log.w("Exception when reading body", e)
        }
        return bodyAsBytes
    }
    String getUserAgent() {
        return headers("User-Agent")
    }
}
