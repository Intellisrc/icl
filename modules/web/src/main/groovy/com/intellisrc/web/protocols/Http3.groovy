package com.intellisrc.web.protocols

import com.intellisrc.core.Millis
import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory
import org.eclipse.jetty.http3.server.HTTP3ServerQuicConfiguration
import org.eclipse.jetty.server.AbstractNetworkConnector
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2025/12/19
 *
 * FIXME: Browsers only switch to HTTP/3 if you advertise it.
response.setHeader(
    "Alt-Svc",
    'h3=":443"; ma=86400'
)

 How to test Chrome
 chrome://net-export

 or DevTools → Network → Protocol = h3

 curl --http3 https://localhost:443

 Troubleshooting:
 Sharing same port with HTTP/2  -> ❌ Won’t work
 Missing Alt-Svc	            -> ❌ Browser stays on h2
 TLS < 1.3	                    -> ❌ QUIC fails
 UDP blocked	                -> ❌ Silent fallback

 Recommended server layout:
 TCP 443 → HTTP/1.1 + HTTP/2
 UDP 443 → HTTP/3

 Please check the documentation as HTTP/3 require native libraries:
 https://jetty.org/docs/jetty/12.1/programming-guide/server/http.html#connector-protocol-http3
 */
@CompileStatic
class Http3 extends Http {
    final static boolean isAvailable = false // Remove when available

    int idleTimeout = Millis.MIN_5

    Http3(WebService server) {
        super(server)
    }

    @Override
    AbstractNetworkConnector prepareConnector() {
        assert server : "No server specified"

        if (!server.secure) {
            throw new IllegalStateException(
                "HTTP/3 requires TLS. Enable HTTPS."
            )
        }

        // TLS (must be TLS 1.3 for QUIC)
        SslContextFactory.Server sslContextFactory =
            getSSLContextFactory()

        // HTTP/3 protocol factory
        HTTP3ServerConnectionFactory h3 =
            new HTTP3ServerConnectionFactory()

        // QUIC connector (UDP)
        /*QuicServerConnector connector =
            new QuicServerConnector(
                server.server,
                sslContextFactory,
                h3
            )

        connector.setPort(server.port)
        connector.setIdleTimeout(idleTimeout)
        */
        /* According to documentation: https://jetty.org/docs/jetty/12.1/programming-guide/server/http.html#connector-protocol-http3
        QuicheServerQuicConfiguration serverQuicConfig = HTTP3ServerQuicConfiguration.configure(new QuicheServerQuicConfiguration(pemWorkDir))
        QuicheServerConnector connector = new QuicheServerConnector(server, sslContextFactory, serverQuicConfig, new HTTP3ServerConnectionFactory())
        connector.setPort(843)
        */
        assert isAvailable : "HTTP3 is not yet available in Jetty 12.1"
        return null //connector
    }
}