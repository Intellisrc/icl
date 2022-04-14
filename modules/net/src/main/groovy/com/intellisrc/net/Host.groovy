package com.intellisrc.net

import com.intellisrc.core.Millis
import com.intellisrc.core.SysClock
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
    boolean isUp(int timeout = Millis.SECOND_10) {
        return ip.isReachable(timeout)
    }

    /**
     * Returns the time in microseconds taken to check if host is up
     * @param timeout
     * @return -1 if timeout expired
     */
    long pingMicro(int timeout = Millis.SECOND_10) {
        LocalDateTime start = SysClock.now
        boolean up = isUp(timeout)
        return up ? ChronoUnit.MICROS.between(start, SysClock.now) : -1
    }

    /**
     * Returns the time in milliseconds taken to check if host is up
     * @param timeout
     * @return -1 if timeout expired
     */
    int ping(int timeout = Millis.SECOND_10) {
        LocalDateTime start = SysClock.now
        boolean up = isUp(timeout)
        return up ? ChronoUnit.MILLIS.between(start, SysClock.now) as int : -1
    }
}
