package com.intellisrc.net

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic

/**
 * Information about the local host
 * @since 2022/04/11.
 */
@CompileStatic
class LocalHost {
    final static Inet4Address loopbackAddress = "127.0.0.1".toInet4Address()

    /**
     * Returns the localhost name
     * @return
     */
    static String getName() {
        return InetAddress.getLocalHost().getHostName()
    }

    /**
     * Retrieves an available port.
     * NOTE: Be aware that it is not protected against race conditions
     * @return
     */
    static int getFreePort() {
        int port = 0
        try {
            ServerSocket socket = new ServerSocket(0)
            port = socket.localPort
            socket.close()
        } catch (Exception ignore) {
            Log.w("Unable to get free port")
        }
        return port
    }

    /**
     * Checks if a port is free (available)
     * @param self
     * @param port
     * @return
     */
    static boolean isPortAvailable(int port) {
        // Test port before initializing
        boolean portAvailable = false
        try {
            new ServerSocket(port).close()
            portAvailable = true
        } catch (IOException ignored) {}
        return portAvailable
    }

    /**
     * Get all IP addresses registered in local host
     * @return
     */
    static List<Inet4Address> getIp4Addresses() {
        List<Inet4Address> addresses = []
        NetworkInterface.getNetworkInterfaces().each {
            NetworkInterface it ->
                addresses.addAll(new NetFace(it).ip4List)
        }
        return addresses
    }

    /**
     * Get all IP addresses registered in local host
     * @return
     */
    static List<Inet6Address> getIp6Addresses() {
        List<Inet6Address> addresses = []
        NetworkInterface.getNetworkInterfaces().each {
            NetworkInterface it ->
                addresses.addAll(new NetFace(it).ip6List)
        }
        return addresses
    }

    /**
     * Test if port is open in localhost
     * @param port
     * @return
     */
    static boolean hasOpenPort(int port) {
        new Host(loopbackAddress).hasOpenPort(port, Millis.SECOND)
    }
}
