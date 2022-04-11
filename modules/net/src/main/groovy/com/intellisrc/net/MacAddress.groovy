package com.intellisrc.net

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Handle Mac Address - like values, for example:
 *
 *  3A:BB:0F:12:99:AA
 *
 *  It store the value as byte[] and can be converted back using `toString()`
 *  It can handle as many bytes as needed
 *
 * @since 17/03/10.
 */
@Immutable
@CompileStatic
class MacAddress {
    byte[] bytes
    String separator = ':'
    boolean upperCase = true

    static MacAddress fromString(String macStr) {
        new MacAddress(bytes: macStr.replaceAll(/[^0-9a-fA-F]/,"").decodeHex())
    }

    @Override
    String toString() {
        String fmt = Collections.nCopies(bytes.length, "%02X").join(separator)
        String str = String.format(fmt, bytes)
        return upperCase ? str.toUpperCase() : str.toLowerCase()
    }
}
