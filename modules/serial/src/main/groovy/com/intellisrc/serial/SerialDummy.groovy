package com.intellisrc.serial

import com.intellisrc.core.Log
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic

/**
 * A Serial that does nothing
 * @since 19/03/06.
 */
@CompileStatic
class SerialDummy extends Seriable {
    SerialDummy() {
        serialPort = "dummy"
    }
    void connect(SerialEvent event = null) {
        connected = true
    }
    void disconnect() {
        connected = false
    }
    void read(int byteCount, SerialReader onResponse) {
        onResponse.call(Bytes.fromString("0")) //TODO: simulate
    }
    void readLine(SerialReaderStr onResponse) {
        onResponse.call("0") //TODO: simulate
    }
    void readNum(SerialReaderInt onResponse) {
        onResponse.call(0) //TODO: simulate
    }
    void write(SerialWriter writer) {
        byte[] toSend = writer.call()
        Log.i("Sending: %s", Bytes.toString(toSend))
    }
    void writeStr(SerialWriterStr writer) {
        String toSend = writer.call()
        Log.i("Sending: %s", toSend)
    }
    void writeNum(SerialWriterInt writer) {
        int toSend = writer.call()
        Log.i("Sending: %s", toSend)
    }
}
