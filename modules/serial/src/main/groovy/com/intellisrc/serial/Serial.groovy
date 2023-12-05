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
    int dataBits = SerialPort.DATABITS_8
    int stopBits = SerialPort.STOPBITS_1
    Serial(String port, SerialPort comm = null) {
        serialPort = port
        portComm = comm ?: findPort()
    }
    static List<String> listPorts() {
        return SerialPortList.portNames.toList()
    }
    
    SerialPort getPort() {
        return portComm
    }
    
    SerialPort findPort() {
        return new SerialPort(serialPort)
    }

    @Override
    void connect(SerialEvent event = null) {
        if(portComm) {
            Log.i("Connecting to device: %s", serialPort)
            try {
                portComm.openPort()
                portComm.setParams(baudRate,
                        dataBits,
                        stopBits,
                        parity)
                connected = true
                if (event) {
                    portComm.addEventListener({
                        SerialPortEvent ev ->
                            event.call(ev)
                    })
                }
            } catch(Exception e) {
                Log.w("Unable to open port [%s] : %s", serialPort, e)
                sleep(waitOnFailure)
            }
        }
    }
    /**
     * Terminate
     */
    @Override
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
    @Override
    void read(int byteCount, SerialReader onResponse) {
        if(connected) {
            try {
                onResponse.call(timeout ? portComm.readBytes(byteCount, timeout) : portComm.readBytes(byteCount))
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
    @Override
    void readLine(SerialReaderStr onResponse) {
        String str = timeout ? portComm.readString(timeout) : portComm.readString()
        onResponse.call(str)
    }

    /**
     * Return a digit from the serial
     * @param onResponse
     */
    @Override
    void readNum(SerialReaderInt onResponse) {
        String buff = timeout ? portComm.readString(timeout) : portComm.readString()
        if(buff != null) {
            onResponse.call(Integer.parseInt(buff[0]))
        }
    }

    /**
     * Send to port some character
     * @param msg
     */
    @Override
    void write(byte[] toSend) {
        if(portComm) {
            if (toSend != null) {
                try {
                    Log.v("Sending to port %s : %s", serialPort, Bytes.toString(toSend))
                    portComm.writeBytes(toSend)
                } catch(Exception e) {
                    Log.w("Unable to write to port: %s", serialPort, e)
                }
            }
        } else {
            Log.w("Port: $serialPort not found.")
        }
    }
    @Override
    void write(String toSend) {
        if(portComm) {
            if(toSend != null) {
                try {
                    Log.v("Sending to port %s : %s", serialPort, toSend)
                    portComm.writeString(toSend)
                } catch(Exception e) {
                    Log.w("Unable to write to port: %s", serialPort, e)
                }
            }
        } else {
            Log.w("Port: $serialPort not found.")
        }
    }
    @Override
    void write(Integer toSend) {
        if(portComm) {
            if(toSend != null) {
                try {
                    Log.v("Sending to port %s : %d", serialPort, toSend)
                    portComm.writeInt(toSend)
                } catch(Exception e) {
                    Log.w("Unable to write to port: %s", serialPort, e)
                }
            }
        } else {
            Log.w("Port: $serialPort not found.")
        }
    }
}
