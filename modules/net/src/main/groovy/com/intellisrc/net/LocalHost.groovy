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
     * @param address : If specified will check in specific address
     * @return
     */
    static int getFreePort(InetAddress address = null) {
        int port = 0
        try {
            ServerSocket socket = address ? new ServerSocket(0, 1, address) : new ServerSocket(0)
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
     * @param address : If specified will check if port is available in specific address
     * @return
     */
    static boolean isPortAvailable(int port, InetAddress address = null) {
        // Test port before initializing
        boolean portAvailable = false
        try {
            if(address) {
                new ServerSocket(port, 1, address).close()
            } else {
                new ServerSocket(port).close()
            }
            portAvailable = true
        } catch (IOException ignored) {}
        return portAvailable
    }

    /**
     * Get all network interfaces in local host
     * @return
     */
    static List<NetworkInterface> getInterfaces() {
        return NetworkInterface.getNetworkInterfaces().toList()
    }

    /**
     * Get all network interfaces as NetFace objects
     * @return
     */
    static List<NetFace> getNetFaces() {
        return interfaces.collect { new NetFace(it) }
    }

    /**
     * Get the NetworkInterface object for InetAdress
     * @param ip
     * @return
     */
    static Optional<NetworkInterface> getNetworkInterface(InetAddress ip) {
        NetworkInterface iface = null
        try {
            iface = NetworkInterface.getByInetAddress(ip)
        } catch(Exception ignore) {}
        return Optional.ofNullable(iface)
    }
    /**
     * Get the NetFace object which contains IP4 address
     * @param ip4
     * @return
     */
    static Optional<NetFace> getNetFace(InetAddress ip) {
        NetFace face = null
        Optional<NetworkInterface> iface = getNetworkInterface(ip)
        if(iface.present) {
            face = new NetFace(iface.get())
        }
        return Optional.ofNullable(face)
    }

    /**
     * Get the NetFace by name
     * @param name
     * @return
     */
    static Optional<NetFace> getNetFace(String name) {
        NetFace face = null
        NetworkInterface iface = interfaces.find { it.name.toLowerCase() == name.toLowerCase() }
        if(iface) {
            face = new NetFace(iface)
        }
        return Optional.ofNullable(face)
    }

    /**
     * Get all IP addresses registered in local host
     * @return
     */
    static List<Inet4Address> getIp4Addresses() {
        List<Inet4Address> addresses = []
        interfaces.each {
            NetworkInterface it ->
                addresses.addAll(new NetFace(it).ip4List)
        }
        return addresses
    }

    /**
     * Get the first IP which matches the local network
     * NOTE: if other services are running in server (like docker, lxd,
     * the returned IP address may be not what we expect).
     * If the interface is connected to a network, it works better
     * @return
     */
    static Inet4Address getLocalNetworkIp4() {
        return (getLocalNetworkAddresses(true) ?: getLocalNetworkAddresses())?.first() ?: loopbackAddress
    }

    /**
     * Return local network NetFace object
     * @return
     */
    static NetFace getLocalNetFace() {
        return new NetFace(getNetworkInterface(localNetworkIp4).get())
    }

    /**
     * Get all local network IP addresses registered in local host
     * @return
     */
    static List<Inet4Address> getLocalNetworkAddresses(boolean mustBeConnected = false) {
        List<Inet4Address> addresses = []
        NetworkInterface.getNetworkInterfaces().each {
            NetworkInterface it ->
                if(!mustBeConnected || it.up) {
                    addresses.addAll(new NetFace(it).localAddresses)
                }
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
        return new Host(loopbackAddress).hasOpenPort(port, Millis.SECOND)
    }

    /**
     * Automatically try to get the local network
     * @return
     */
    static Network getLocalNetwork() {
        Inet4Address ip = getLocalNetworkIp4()
        Optional<NetworkInterface> iface = getNetworkInterface(ip)
        return iface.present ? new Network(ip, iface.get().interfaceAddresses.find { it.address.hostAddress == ip.hostAddress }.networkPrefixLength).reset() : new Network(ip)
    }
}
