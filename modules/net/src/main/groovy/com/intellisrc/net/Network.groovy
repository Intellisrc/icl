package com.intellisrc.net

import com.intellisrc.groovy.StringExt
import groovy.transform.CompileStatic
import org.apache.commons.net.util.SubnetUtils

@CompileStatic
/**
 * Methods related to Networking (not related to NetworkInterface)
 * It can be used to store IP4 addresses or network in CIDR annotation
 */
class Network {
    final SubnetUtils subnet
    /**
     * Constructor having a SubnetUtils object
     * @param subnet
     */
    Network(SubnetUtils subnet) {
        this.subnet = subnet
        setInclusive()
    }
    /**
     * Constructor from Inet4Address and mask (optional)
     * @param address
     * @param mask
     */
    Network(Inet4Address address, int mask = 32) {
        subnet = new SubnetUtils(address.hostAddress + "/" + mask.toString())
        setInclusive()
    }
    /**
     * Constructor having CIDR annotation (e.g. 192.168.10.0/24)
     * @param cidr
     */
    Network(String cidr, int mask = 32) {
        if(!cidr.contains("/")) {
            cidr = cidr += ("/" + mask)
        } else if(mask != 32) {
            cidr = cidr.replaceAll(/\/\d+/, "/" + mask)
        }
        subnet = new SubnetUtils(cidr)
        setInclusive()
    }
    /**
     * Create network from Integer (usually exported with .toInteger())
     * @param value
     * @param mask
     */
    Network(int value, int mask = 32) {
        byte[] bytes = value.toBigInteger().toByteArray()
        subnet = new SubnetUtils(Inet4Address.getByAddress(bytes).hostAddress + "/" + mask.toString())
        setInclusive()
    }

    /**
     * Use network address instead of specified
     * @return
     */
    Network reset() {
        return new Network(getNetworkSubnetUtils(subnet))
    }

    /**
     * Turn inclusive ON for /32
     */
    protected void setInclusive() {
        if(subnet.info.netmask == "255.255.255.255") { //mask = 32
            subnet.inclusiveHostCount = true
        }
    }

    /**
     * Returns true if IP address is contained in subnetwork
     * @param subnet
     * @param ip
     * @return
     */
    boolean contains(Inet4Address ip) {
        return subnet.info.isInRange(ip.hostAddress)
    }
    /**
     * Check if the address is from a Local Network
     * @return
     */
    boolean isLocal() {
        String addr = address.hostAddress
        int second = addr.tokenize(".")[1] as int
        return addr.startsWith("10.") || addr.startsWith("192.168") || (addr.startsWith("172.") && (second >= 16 && second <= 31))
    }
    /**
     * Check if address is a loop back address
     * @return
     */
    boolean isLoopBack() {
        return address.hostAddress.startsWith("127")
    }
    /**
     * Return IP address
     * @return
     */
    Inet4Address getAddress() {
        return subnet.info.address.toInet4Address()
    }
    /**
     * Get Next address
     * @return
     */
    Inet4Address getNextAddress() {
        return subnet.info.nextAddress.toInet4Address()
    }
    /**
     * Get Prev address
     * @return
     */
    Inet4Address getPrevAddress() {
        return subnet.info.previousAddress.toInet4Address()
    }
    /**
     * Return network (eg. 192.168.1.0)
     * @return
     */
    Inet4Address getNetwork() {
        return subnet.info.networkAddress.toInet4Address()
    }
    /**
     * Return the first IP in network
     * @param subnet
     * @return
     */
    Inet4Address getFirstInNetwork() {
        return subnet.info.lowAddress.toInet4Address()
    }

    /**
     * Return the last IP in network
     * @param subnet
     * @return
     */
    Inet4Address getLastInNetwork() {
        return subnet.info.highAddress.toInet4Address()
    }

    /**
     * Return broadcast IP address
     * @return
     */
    Inet4Address getBroadcast() {
        return subnet.info.broadcastAddress.toInet4Address()
    }

    /**
     * Return netmask, e.g: 255.255.255.0 as Inet4Address
     * @return
     */
    Inet4Address getNetmask4() {
        return subnet.info.netmask.toInet4Address()
    }
    /**
     * Return netmask as string
     * @return
     */
    String getNetmask() {
        return subnet.info.netmask
    }

    /**
     * List all IP addresses in network
     * @param subnet
     * @return
     */
    List<Inet4Address> getAllInNetwork() {
        return subnet.info.allAddresses.collect { it.toInet4Address() }
    }

    /**
     * Number of addresses in that network
     * @return
     */
    long getSize() {
        return subnet.info.addressCountLong
    }

    /**
     * Generates a random IP4 (optionally a subnet can be specified)
     * @return
     * @since 17/02/22.
     */
    Inet4Address randomIP4() {
        return getAllInNetwork().random()
    }

    /**
     * Return network as CIDR annotation
     * @return
     */
    String getCidr() {
        return subnet.info.cidrSignature
    }

    /**
     * Return as String
     * @return
     */
    String toString() {
        return cidr
    }

    /**
     * Return address as int
     * @return
     */
    int toInteger() {
        return subnet.info.asInteger(subnet.info.address)
    }

    //////////////////////////////// STATIC ///////////////////////////////////////

    /**
     * Get range of IP addresses based in StringE range
     * http://stackoverflow.com/questions/31386323/
     * @param ipRange
     * Examples:
     *  10.0.0.1-100
     *  10.0.1-3.0-255
     *
     * @return
     */
    @SuppressWarnings('GroovyUnusedAssignment')
    static List<Inet4Address> getIpsFromRange(final String ipRange) { //117.211.141-147.20-218
        String[] segments = ipRange.split("\\.")    //split into 4 segments
        int seg1Lower, seg1Upper, seg2Lower, seg2Upper
        int seg3Lower, seg3Upper, seg4Lower, seg4Upper

        // get lower and upper bound of 1st segment
        String[] seg1 = segments[0].split("-")
        if (seg1.length == 1) {
            seg1Lower = Integer.parseInt(seg1[0])
            seg1Upper = Integer.parseInt(seg1[0])
        } else {
            seg1Lower = Integer.parseInt(seg1[0])
            seg1Upper = Integer.parseInt(seg1[1])
        }

        // get lower and upper bound of 2nd segment
        String[] seg2 = segments[1].split("-")
        if (seg2.length == 1) {
            seg2Lower = Integer.parseInt(seg2[0])
            seg2Upper = Integer.parseInt(seg2[0])
        } else {
            seg2Lower = Integer.parseInt(seg2[0])
            seg2Upper = Integer.parseInt(seg2[1])
        }

        // get lower and upper bound of 3rd segment
        String[] seg3 = segments[2].split("-")
        if (seg3.length == 1) {
            seg3Lower = Integer.parseInt(seg3[0])
            seg3Upper = Integer.parseInt(seg3[0])
        } else {
            seg3Lower = Integer.parseInt(seg3[0])
            seg3Upper = Integer.parseInt(seg3[1])
        }

        // get lower and upper bound of 4th segment
        String[] seg4 = segments[3].split("-")
        if (seg4.length == 1) {
            seg4Lower = Integer.parseInt(seg4[0])
            seg4Upper = Integer.parseInt(seg4[0])
        } else {
            seg4Lower = Integer.parseInt(seg4[0])
            seg4Upper = Integer.parseInt(seg4[1])
        }

        //generate all IPs
        List<Inet4Address> IPs = []
        for (int i = seg1Lower; i <= seg1Upper; i++) {
            for (int j = seg2Lower; j <= seg2Upper; j++) {
                for (int k = seg3Lower; k <= seg3Upper; k++) {
                    for (int l = seg4Lower; l <= seg4Upper; l++) {
                        IPs.add((Inet4Address) InetAddress.getByName(i + "." + j + "." + k + "." + l))
                    }
                }
            }
        }

        return IPs
    }

    /**
     * Generates a random IP4 any network
     * @return
     * @since 17/02/22.
     */
    static Inet4Address anyRandomIP4() {
        Random rand = new Random()
        return StringExt.toInet4Address(rand.nextInt(256) + "." + rand.nextInt(256) + "." + rand.nextInt(256) + "." + rand.nextInt(256))
    }

    /**
     * Resets the network IP to the network address
     * @param tmp
     * @return
     */
    static SubnetUtils getNetworkSubnetUtils(SubnetUtils tmp) {
        return new SubnetUtils(tmp.info.networkAddress, tmp.info.netmask)
    }

    /**
     * Return true if IP address is local network address
     * @return
     */
    static boolean isIpLocal(Inet4Address inet) {
        return new Network(inet).isLocal()
    }

    /**
     * Static method to convert from string
     * @param cidr
     * @return
     */
    static Network fromString(String cidr) {
        return new Network(cidr)
    }
}
