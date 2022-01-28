# SERIAL Module (ICL.serial)

Manage serial communication easily. It uses  
JSSC library on the background.

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/serial)

## Classes

* `Serial`         : Use a serial port (connect, read, write, disconnect)
* `SerialDummy`    : Dummy implementation of `Seriable` for Unit Testing

### Example
```groovy
// Get serial ports available:
List<String> ports = Serial.listPorts()
// Connect to serial port: (e.g. ttyUSB0)
Serial serial = new Serial(ports.first())
//SerialPort serialPort = serial.port <-- will return SerialPort object from JSSC
serial.connect()
// Sending data
serial.write("hello")
serial.write(100)
serial.write("data".bytes)
// Reading data
serial.readNum({
    Integer val ->
        Log.i("Received: %d", val)
})
serial.readLine({
    String line ->
        Log.i("Received %s", line)
})
serial.read(16, {
    byte[] bytes ->
        Log.i("Received 16 bytes")
})
serial.disconnect()
```
