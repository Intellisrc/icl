package jp.sharelock.net

import groovy.transform.Immutable

/**
 * Defines a Service (running process in port)
 * @since 3/5/17.
 */
@groovy.transform.CompileStatic
@Immutable
class Service {
    final static enum Protocol {
        UNKNOWN, TCP, UDP
        /**
         * Returns a protocol from a String
         * @param proto
         * @return
         */
        static Protocol fromString(String proto) {
            Protocol thisProtocol
            switch(proto) {
                case ~/(?i)tcp/:
                    thisProtocol = TCP
                    break
                case ~/(?i)udp/:
                    thisProtocol = UDP
                    break
                default:
                    thisProtocol = UNKNOWN
                    break
            }
            return thisProtocol
        }
    }
    int port
    String name
    String version //Maybe will change to split ints to be able to compare easily
    Protocol protocol
}
