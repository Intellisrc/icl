package com.intellisrc.serial

import groovy.transform.CompileStatic
import jssc.SerialPort
import jssc.SerialPortEvent

/**
 * @since 19/03/06.
 */
@CompileStatic
abstract class Seriable {
    public boolean connected = false
    public int parity = SerialPort.PARITY_NONE
    public int baudRate = 9600
    public String serialPort

    interface SerialReader {
        void call(byte[] response)
    }
    interface SerialWriter {
        byte[] call()
    }
    interface SerialReaderStr {
        void call(String response)
    }
    interface SerialWriterStr {
        String call()
    }
    interface SerialReaderInt {
        void call(Integer response)
    }
    interface SerialWriterInt {
        Integer call()
    }
    interface SerialEvent {
        void call(SerialPortEvent event)
    }
    abstract void connect(SerialEvent event = null)
    abstract void disconnect()
    abstract void read(int byteCount, SerialReader onResponse)
    abstract void readLine(SerialReaderStr onResponse)
    abstract void readNum(SerialReaderInt onResponse)
    abstract void write(SerialWriter writer)
    abstract void writeStr(SerialWriterStr writer)
    abstract void writeNum(SerialWriterInt writer)
}