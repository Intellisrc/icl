---
name: serial
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for serial port communication (RS-232, Arduino, modems, industrial devices) - listing ports, connecting with a baud rate, reading lines or bytes, writing, and SerialDummy for unit tests without hardware. Wraps JSSC. Triggers on com.intellisrc.serial imports or Serial class usage in an ICL project. Includes core and etc.
---

# ICL serial Module

Serial port access for ICL, wrapping JSSC (bundled). Includes core and etc.

```groovy
implementation 'com.intellisrc:serial:2.10.5'   // + core + etc + groovy
```

Package: `com.intellisrc.serial`.

## Serial — connect, read, write

```groovy
import com.intellisrc.serial.Serial

List<String> ports = Serial.listPorts()          // e.g. ["/dev/ttyUSB0", "COM3"]; [] if none/permissions
Serial serial = new Serial(ports.first())
serial.baudRate = 115200                         // default 9600; also parity, timeout properties
serial.connect()                                 // connect { SerialPortEvent ev -> } for event mode

serial.write("AT\r\n")                           // String, byte[] or int
serial.readLine { String line -> handle(line) }  // callback per line
serial.read(16) { byte[] bytes -> handle(bytes) }  // fixed-size reads

serial.disconnect()
```

`SerialReader` wraps a blocking read loop (`read()` / `stop()`) when callbacks are not wanted.

## SerialDummy — unit testing

```groovy
SerialDummy dummy = new SerialDummy()   // serialPort = "dummy"
dummy.connect()                         // all methods no-op; reads return "0"/0, writes are logged
```

Inject `Seriable` (common interface of Serial and SerialDummy) so hardware-free tests run the same code.

## Gotchas

- JSSC ships native libs per OS/arch; on Linux, port access may need user in `dialout` group.
- `timeout = 0` (default) blocks reads; set milliseconds for non-blocking behavior.
- `listPorts()` returns an empty list when no ports exist or permission is denied — check before connecting.
