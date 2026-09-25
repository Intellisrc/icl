package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.net.Network
import groovy.transform.CompileStatic
import org.eclipse.jetty.util.HostPort

/**
 * List of trusted proxies used to decide when the X-Forwarded-For header
 * can be trusted to calculate the client IP (see Request.ip()).
 *
 * The proxies can be specified as String (IP address, hostname, CIDR
 * annotation like "10.0.0.0/8" or ranges like "10.0.0.1-100"),
 * InetAddress or Network objects (or a mix of them, including nested
 * collections).
 *
 * Hostnames are resolved only once, when the list is created.
 *
 * @since 2026/09/11.
 */
@CompileStatic
class TrustedProxies {
    /** Servlet request attribute used to pass the parsed list to each Request */
    public static final String ATTRIBUTE = "com.intellisrc.web.trustedProxies"

    /** Matchers: Network (IPv4 or CIDR) or InetAddress (exact IP) */
    protected final List<Object> matchers = new ArrayList<>()

    /**
     * @param proxies : List of proxies (String, InetAddress, Network or collections of them)
     */
    TrustedProxies(List<?> proxies) {
        addAll(proxies)
    }

    /**
     * Add more proxies to the list
     * @param proxies
     */
    void addAll(List<?> proxies) {
        if (proxies != null) {
            for (Object proxy : proxies.flatten()) {
                switch (proxy) {
                    case Network:
                    case InetAddress:
                        matchers.add(proxy)
                        break
                    case String:
                        addString((String) proxy)
                        break
                    case null:
                        break
                    default:
                        Log.w("Unsupported trusted proxy [%s] of type: %s", proxy.toString(), proxy.getClass().getName())
                }
            }
        }
    }

    /**
     * Add a proxy specified as String: IP address, hostname,
     * CIDR (e.g. "192.168.0.0/16") or range (e.g. "10.0.1-3.0-255")
     * @param proxy
     */
    protected void addString(String proxy) {
        String str = proxy.trim()
        try {
            if (str.contains("/")) { // CIDR annotation
                matchers.add(new Network(str))
            } else if (str ==~ /\d{1,3}(-\d{1,3})?(\.\d{1,3}(-\d{1,3})?){3}/) { // Range (e.g. 10.0.0.1-100)
                matchers.addAll(Network.getIpsFromRange(str))
            } else { // IP address or hostname (resolved only once, here)
                matchers.add(str.toInetAddress())
            }
        } catch (Exception e) {
            Log.w("Unable to use [%s] as trusted proxy: %s", str, e.getMessage())
        }
    }

    /**
     * @return True if no proxy was specified (X-Forwarded-For is never trusted)
     */
    boolean isEmpty() {
        return matchers.isEmpty()
    }

    /**
     * Check if an IP address belongs to a trusted proxy
     * @param ip : IP address (it can contain a port, e.g. "10.0.0.1:1234")
     * @return True if the IP address is one of the trusted proxies
     */
    boolean isTrusted(String ip) {
        if (ip == null || ip.isEmpty()) { return false }
        String clean = cleanIp(ip)
        if (!isIp(clean)) { return false } // "unknown", obfuscated names, etc
        for (Object matcher : matchers) {
            if (isTrusted(clean, matcher)) { return true }
        }
        return false
    }

    /**
     * Check an IP address (clean, without port) against one matcher
     */
    protected boolean isTrusted(String ip, Object matcher) {
        try {
            if (matcher instanceof Network) {
                return ((Network) matcher).contains(ip.toInet4Address())
            } else if (matcher instanceof InetAddress) {
                return matcher == ip.toInetAddress()
            }
        } catch (Exception ignore) {
            // The IP doesn't match the matcher type (e.g. IPv6 with a Network)
        }
        return false
    }

    /**
     * Remove the port from an IP address if present
     * (e.g. "10.0.0.1:1234" -> "10.0.0.1", "[::1]:1234" -> "::1")
     * @param ip
     * @return
     */
    static String cleanIp(String ip) {
        String value = ip.trim()
        try {
            value = new HostPort(value).getHost()
        } catch (Exception ignore) {}
        return value.replaceFirst(/^\[/, "").replaceFirst(/\]$/, "")
    }

    /**
     * Check that a value looks like an IP address
     * (so names are never resolved when matching)
     * @param value
     * @return
     */
    static boolean isIp(String value) {
        return value ==~ /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/ ||
            (value.contains(":") && !value.contains("."))
    }
}
