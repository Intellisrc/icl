package jp.sharelock.web

import groovy.transform.Immutable

/**
 * @since 17/03/10.
 */
@Immutable
class MacAddress {
    byte[] mac

    static MacAddress fromString(String macStr) {
        new MacAddress(mac: macStr.replaceAll(/[^0-9a-fA-F]/,"").decodeHex())
    }

    @Override
    String toString() {
        String.format("%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5])
    }
    boolean isVM() {
        byte[][] virtualMacs = [
            [0x00, 0x05, 0x69],             //VMWare
            [0x00, 0x1C, 0x14],             //VMWare
            [0x00, 0x0C, 0x29],             //VMWare
            [0x00, 0x50, 0x56],             //VMWare
            [0x08, 0x00, 0x27],             //Virtualbox
            [0x0A, 0x00, 0x27],             //Virtualbox
            [0x00, 0x03, 0xFF],             //Virtual-PC
            [0x00, 0x15, 0x5D]              //Hyper-V
        ]
        return virtualMacs.find {
            it[0] == mac[0] && it[1] == mac[1] && it[2] == mac[2]
        }
    }
}
