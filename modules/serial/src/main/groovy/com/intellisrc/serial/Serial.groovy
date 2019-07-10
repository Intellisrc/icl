package com.intellisrc.serial

import com.intellisrc.core.Log
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic
import jssc.SerialPort
import jssc.SerialPortEvent
import jssc.SerialPortList

/**
 * @since 19/02/20.
 */
@CompileStatic
class Serial extends Seriable {
    final private SerialPort portComm
    Serial(String port) {
        serialPort = port
        portComm = findPort()
    }
    static List<String> listPorts() {
        return SerialPortList.portNames.toList()
    }

    private SerialPort findPort() {
        return new SerialPort(serialPort)
    }

    void connect(SerialEvent event = null) {
        if(portComm) {
            Log.i("Connecting to device: %s", serialPort)
            try {
                portComm.openPort()
                portComm.setParams(baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        parity)
                connected = true
                if (event) {
                    portComm.addEventListener({
                        SerialPortEvent ev ->
                            event.call(ev)
                    })
                }
            } catch(Exception e) {
                Log.w("Unable to open port [%s] : %s", serialPort, e.message)
                sleep(1000)
            }
        }
    }
    /**
     * Terminate
     */
    void disconnect() {
        connected = false
        if(portComm?.opened) {
            portComm.closePort()
        }
    }

    /**
     * Read from serial N bytes
     * @param byteCount
     * @param onResponse
     */
    void read(int byteCount, SerialReader onResponse) {
        if(connected) {
            try {
                onResponse(portComm.readBytes(byteCount))
            } catch(Exception e) {
                Log.e("unable to read from device: %s", serialPort, e)
            }
        } else {
            Log.w("Port: $serialPort not found.")
        }
    }

    /**
     * Read one line from the buffer
     * @param onResponse
     */
    void readLine(SerialReaderStr onResponse) {
        String str = portComm.readString()
        onResponse.call(str)
    }

    /**
     * Return a digit from the serial
     * @param onResponse
     */
    void readNum(SerialReaderInt onResponse) {
        String buff = portComm.readString()
        if(buff != null) {
            onResponse.call(Integer.parseInt(buff[0]))
        }
    }

    /**
     * Send to port some character
     * @param msg
     */
    void write(SerialWriter writer) {
        if(portComm) {
            byte[] toSend = writer.call()
            if (toSend != null) {
                try {
                    Log.v("Sending to port %s : %s", serialPort, Bytes.toString(toSend))
                    portComm.writeBytes(toSend)
                } catch(Exception e) {
                    Log.w("Unable to write to port: %s", serialPort)
                }
            }
        } else {
            Log.w("Port: $serialPort not found.")
        }
    }

    void writeStr(SerialWriterStr writer) {
        String toSend = writer.call()
        if(toSend != null) {
            portComm.writeString(toSend)
        }
    }
    void writeNum(SerialWriterInt writer) {
        Integer toSend = writer.call()
        if(toSend != null) {
            portComm.writeInt(toSend)
        }
    }
}
