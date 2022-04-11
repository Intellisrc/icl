package com.intellisrc.net

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
     * Test is host is online
     * @param host
     * @param port
     * @param timeout
     * @return
     */
    boolean isOnline(int port = 0, int timeout = 30 * 1000) {
        if(port) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), timeout)
                return true
            } catch (IOException ignore) {
                return false
            }
        } else {
            return ip.isReachable(timeout)
        }
    }
}
