package com.intellisrc.serial

import groovy.transform.CompileStatic

/**
 * @since 19/03/07.
 */
@CompileStatic
class SerialReader {
    public final Serial reader
    protected boolean running = true
    SerialReader(String serialPort) {
        reader = new Serial(serialPort)
    }
    void read() {
        while(running) {
            println "Waiting for command:"
            reader.readLine({
                println "> [" + it + "]"
            })
        }
    }
    void stop() {
        running = false
    }
}
