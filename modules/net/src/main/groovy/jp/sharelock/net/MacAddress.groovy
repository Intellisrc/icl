package jp.sharelock.net

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * @since 17/03/10.
 */
@Immutable
@CompileStatic
class MacAddress {
    byte[] mac

    static MacAddress fromString(String macStr) {
        new MacAddress(mac: macStr.replaceAll(/[^0-9a-fA-F]/,"").decodeHex())
    }

    @Override
    String toString() {
        String.format("%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5])
    }
}
