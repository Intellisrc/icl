package jp.sharelock.net

import groovy.transform.Immutable
import jp.sharelock.crypt.hash.Hash
import jp.sharelock.etc.SysInfo

/**
 * Device object
 */
@groovy.transform.CompileStatic
@Immutable(knownImmutableClasses = [Inet4Address, Inet6Address])
class Device {
    static final enum DeviceStatus {
        NEW, ONLINE, OFFLINE, BLOCKED, MONITORED, UNKNOWN
        static DeviceStatus fromInt(int istatus) {
            values()[istatus] ?: UNKNOWN
        }
    }
    int id
    Inet4Address address
    Inet6Address address6
    MacAddress mac
    String name
    String hardware
    SysInfo.OSType os
    String osVersion
    String workgroup
    Collection<Service> services
    // Methods
    String toString() {
        String footPrint = address.getHostAddress() + "|" + mac + "|" + name
        return Hash.MD5(footPrint.toCharArray())
    }
    void print() {
        println "Name       : "+name
        println "Address IP4: "+address?.getHostAddress()
        println "Address IP6: "+address6?.getHostAddress()
        println "MAC        : "+mac
        println "Hardware   : "+hardware
        println "OS         : "+os
        println "OS version : "+osVersion
        println "Workgroup  : "+workgroup
        println "Services   :"
        services.each {
            Service srv ->
                println "   "+srv.port+" ("+srv.protocol+") : "+srv.name+" : "+srv.version
        }
    }
}