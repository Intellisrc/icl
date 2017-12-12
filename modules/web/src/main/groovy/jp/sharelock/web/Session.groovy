package jp.sharelock.web

import org.eclipse.jetty.websocket.api.Session as JettySession
import spark.Session as SparkSession
/**
 * @since 17/04/21.
 */
@groovy.transform.CompileStatic
class Session {
    JettySession websocketSession
    SparkSession httpSession
    InetAddress address
    String userID
    Map data
}