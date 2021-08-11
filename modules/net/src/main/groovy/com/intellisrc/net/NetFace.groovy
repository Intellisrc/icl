package com.intellisrc.net

import com.intellisrc.core.Log
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic

/**
 * Simple class to represent a NetworkInterface
 * @since 19/04/08.
 */
@CompileStatic
class NetFace {
    public final String name
    public final MacAddress mac
    public final List<Inet4Address> ip4List = []
    public final List<Inet6Address> ip6List = []
    /**
     * Create a NetFace instance based in a NetworkInterface
     * @param iface
     */
    NetFace(NetworkInterface iface) {
        name = iface.name
        mac = iface.hardwareAddress ? new MacAddress(iface.hardwareAddress) : null
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
    Inet4Address getIp4Address() {
        if(ip4List.empty) {
            Log.w("Interface %s has no IP address", name)
        }
        return ip4List.empty ? null : ip4List.first()
    }
}
