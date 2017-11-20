package jp.sharelock.web

/**
 * @since 17/04/21.
 */
@groovy.transform.CompileStatic
class Session {
    org.eclipse.jetty.websocket.api.Session websocketSession
    spark.Session httpSession
    InetAddress address
    String userID
    Map data
}