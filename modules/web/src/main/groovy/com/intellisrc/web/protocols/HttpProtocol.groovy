package com.intellisrc.web.protocols

import com.intellisrc.core.Log
import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
abstract class HttpProtocol {
    final WebService server
    boolean trustForwardHeaders = true
    boolean checkSNIHostname = true

    static class ErrorListener implements HttpChannel.Listener {
        /**
         * Invoked when the application threw an exception.
         *
         * @param request the request object
         * @param failure the exception thrown by the application
         */
        void onDispatchFailure(Request request, Throwable failure) {
            Log.e("Dispatch failure", failure)
        }
        /**
         * Invoked when the request processing failed.
         *
         * @param request the request object
         * @param failure the request failure
         */
        void onRequestFailure(Request request, Throwable failure) {
            Log.e("Request failure", failure)
        }
        /**
         * Invoked when the response processing failed.
         *
         * @param request the request object
         * @param failure the response failure
         */
        void onResponseFailure(Request request, Throwable failure) {
            Log.e("Response failure", failure)
        }
    }

    HttpProtocol(WebService server) {
        this.server = server
    }

    Connector getConnector() {
        return setup(prepareConnector())
    }

    protected Connector setup(AbstractNetworkConnector connector) {
        connector.setIdleTimeout(server.timeout)
        connector.setHost(server.address.hostAddress)
        connector.setPort(server.port)
        return connector
    }

    abstract void init()
    abstract protected AbstractNetworkConnector prepareConnector()
    abstract protected SslContextFactory.Server getSSLContextFactory()
    abstract protected HttpConnectionFactory getConnectionFactory(HttpConfiguration httpConfiguration)
}