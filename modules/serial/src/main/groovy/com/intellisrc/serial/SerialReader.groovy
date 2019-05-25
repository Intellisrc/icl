package com.intellisrc.serial

import groovy.transform.CompileStatic

/**
 * @since 19/03/07.
 */
@CompileStatic
class SerialReader {
    final Serial reader
    SerialReader(String serialPort) {
        reader = new Serial(serialPort)
    }
    void read() {
        while(true) {
            println "Waiting for command:"
            reader.readLine({
                println "> [" + it + "]"
            })
        }
    }
}
