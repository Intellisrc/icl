package com.intellisrc.web

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.HttpChannel
import org.eclipse.jetty.server.HttpInput
import org.eclipse.jetty.server.Request as JettyRequest
/**
 * @since 2023/05/19.
 */
@CompileStatic
class Request extends JettyRequest {
    Request(HttpChannel channel, HttpInput input) {
        super(channel, input)
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    String ip() {
        return ip.hostAddress
    }
    /**
     * Get the IP address of client
     * @return
     */
    InetAddress getIp() {
        return null //TODO
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    String uri() {
        return uri.toString()
    }
    URI getUri() {
        return null //TODO
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    String url() {
        return url.toString()
    }
    URL getUrl() {
        return null //TODO
    }
    /**
     * Get Session
     * @return
     */
    Session session(boolean checkme = true /* FIXME */) {
        return new Session()//TODO
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
        //TODO
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    List<String> params() {
        return getPathParams()
    }
    /**
     * Get the list of path parameters
     * @return
     */
    List<String> getPathParams() {
        [] //TODO
    }
    boolean hasPathParams() {
        return true // TODO
    }
    /**
     * Get all covered by "*"
     * @return
     */
    List<String> splat() {
        return [] //TODO
    }
    //------------- QUERY PARAMS ---------------
    String queryString() {
        //TODO
    }
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
        //TODO
    }
    /**
     * Backward compatibility with Spark
     * @return
     */
    List<String> queryParams() {
        return getQueryParams()
    }
    /**
     * Get the list of query parameters
     * @return
     */
    List<String> getQueryParams() {
        [] //TODO
    }
    boolean hasQueryParams() {
        return true // TODO
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

    }
}
