package com.intellisrc.net

import groovy.transform.CompileStatic

/**
 * Simple class to represent a NetworkInterface and extends its functionality
 * @since 19/04/08.
 */
@CompileStatic
class NetFace {
    public final String name
    public final MacAddress mac
    public final List<Inet4Address> ip4List = []
    public final List<Inet6Address> ip6List = []
    public final NetworkInterface iface
    /**
     * Create a NetFace instance based in a NetworkInterface
     * @param iface
     */
    NetFace(NetworkInterface iface) {
        this.iface = iface
        name = iface.name
        mac = iface.hardwareAddress ? new MacAddress(bytes: iface.hardwareAddress) : null
        iface.inetAddresses.each {
            InetAddress ip ->
                if (ip.address.length == 4) { //IP4
                    ip4List << (Inet4Address) ip
                } else if(ip.address.length == 16) { //IP6
                    ip6List << (Inet6Address) ip
                }
        }
    }
    /**
     * For convenience, it returns the first IP4 found
     * @return
     */
    Optional<Inet4Address> getFirstIp4() {
        return Optional.ofNullable(ip4List.empty ? null : ip4List.first())
    }
    /**
     * For convenience, it returns the first IP4 found
     * @return
     */
    Optional<Inet6Address> getFirstIp6() {
        return Optional.ofNullable(ip6List.empty ? null : ip6List.first())
    }
    /**
     * Return the IP address which starts with...
     * for example, the first one which starts with "192"
     * @param partialMatch
     * @return
     */
    Optional<Inet4Address> getIpStarts(String startsWith) {
        return Optional.ofNullable(ip4List.find { it.hostAddress.startsWith(startsWith) })
    }
    /**
     * Return all private local network IP4 addresses
     * @return
     */
    List<Inet4Address> getLocalAddresses() {
        return ip4List.findAll { Network.isIpLocal(it) }
    }
    /**
     * Return true if interface is connected
     * @return
     */
    boolean isConnected() {
        return iface.up
    }
}
