package com.intellisrc.serial

import groovy.transform.CompileStatic
import jssc.SerialPort
import jssc.SerialPortEvent

/**
 * @since 19/03/06.
 */
@SuppressWarnings('SpellCheckingInspection')
@CompileStatic
abstract class Seriable {
    public boolean connected = false
    public int parity = SerialPort.PARITY_NONE
    public int baudRate = 9600
    public String serialPort

    static interface SerialReader {
        void call(byte[] response)
    }
    static interface SerialWriter {
        byte[] call()
    }
    static interface SerialReaderStr {
        void call(String response)
    }
    static interface SerialWriterStr {
        String call()
    }
    static interface SerialReaderInt {
        void call(Integer response)
    }
    static interface SerialWriterInt {
        Integer call()
    }
    static interface SerialEvent {
        void call(SerialPortEvent event)
    }
    abstract void connect(SerialEvent event = null)
    abstract void disconnect()
    abstract void read(int byteCount, SerialReader onResponse)
    abstract void readLine(SerialReaderStr onResponse)
    abstract void readNum(SerialReaderInt onResponse)
    abstract void write(byte[] bytes)
    abstract void write(String str)
    abstract void write(Integer num)

    void write(SerialWriter writer) {
        byte[] toSend = writer.call()
        write(toSend)
    }
    void write(SerialWriterStr writer) {
        String toSend = writer.call()
        write(toSend)
    }
    void write(SerialWriterInt writer) {
        Integer toSend = writer.call()
        write(toSend)
    }
}