package com.intellisrc.net

import com.intellisrc.core.Millis
import groovy.transform.CompileStatic

/**
 * Class which represents a network host
 * @since 2022/04/11.
 */
@CompileStatic
class Host {
    final InetAddress ip

    Host(InetAddress ip) {
        this.ip = ip
    }
    /**
     * Get HostName from IP
     * @return
     */
    String getName() {
        String hn = ip.canonicalHostName
        return hn != ip.hostAddress ? hn : ""
    }

    /**
     * Test if host has an open port
     * @param port
     * @return
     */
    boolean hasOpenPort(int port, int timeout = Millis.SECOND_10) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout)
            return true
        } catch (IOException ignore) {
            return false
        }
    }

    /**
     * Test is host is online
     * @param host
     * @param port
     * @param timeout
     * @return
     */
    boolean isOnline(int timeout = Millis.SECOND_10) {
        return ip.isReachable(timeout)
    }
}
