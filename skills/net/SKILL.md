---
name: net
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for networking - sending email through SMTP with attachments (Smtp), email address validation (Email), host discovery (Host ping and open ports, LocalHost free ports and interfaces), netmask/CIDR calculations (Network, NetFace, MacAddress), TCP/UDP clients and servers, or FTP transfers (FtpClient). Triggers on com.intellisrc.net imports in an ICL project. Includes core, etc and crypt.
---

# ICL net Module

Networking utilities for ICL: email, host discovery, subnet math, TCP/UDP and FTP. Includes core, etc, crypt. Bundles `commons-net` and `jakarta.mail` transitively.

```groovy
implementation 'com.intellisrc:net:2.10.5'   // + core + etc + crypt + groovy
```

Package: `com.intellisrc.net`.

## Smtp — send email

Loads defaults from config under a prefix (default `mail.smtp`: host, port, username, password, from, fromName, replyTo, defaultTo, simulate).

```groovy
Smtp smtp = new Smtp(                          // or new Smtp() + config.properties
    host: "smtp.example.com", port: 587,
    username: "user", password: "pass",
    from: "noreply@example.com", fromName: "Notifier"
)
smtp.addAttachment(new File("report.pdf"))     // multiple allowed; clearAttachments()
smtp.send("dest@example.com", "Subject", "<h1>HTML body</h1>", "plain text body")
smtp.send(["a@x.com", "b@x.com"], "Subject", "body")
smtp.send([(to): Smtp.Mode.TO, (cc): Smtp.Mode.CC, (bcc): Smtp.Mode.BCC], "Subject", "body")
smtp.sendDefault("Subject", "body")            // to defaultTo recipients
// SSL automatic on port 465, STARTTLS on 587; otherwise set smtp.startTLS = true
```

`Email.isValid("user@example.com")` validates; `new Email("user@example.com")` throws `EmailMalformedException`.

## Host / LocalHost — discovery

```groovy
Host host = new Host("192.168.1.1".toInet4Address())
host.isUp()                  // ICMP-ish check (timeout ms arg, default 10000)
host.ping()                  // latency ms, -1 on failure; pingMicro() in us
host.hasOpenPort(80)         // TCP connect check
host.name                    // reverse DNS

int port = LocalHost.freePort            // first free port (also getFreePort(address))
LocalHost.isPortAvailable(8080)
LocalHost.name ; LocalHost.ip4Addresses ; LocalHost.interfaces ; LocalHost.netFaces
LocalHost.localNetworkIp4                // LAN address of this machine
```

## Network / NetFace / MacAddress — subnet math

```groovy
Network net = new Network("192.168.1.0", 24)        // CIDR mask; also (Inet4Address, mask)
net.contains("192.168.1.55".toInet4Address())
net.broadcast ; net.netmask ; net.firstInNetwork ; net.lastInNetwork
net.getAllInNetwork() ; net.size ; net.cidr ; net.isLocal() ; net.isLoopBack()
Network.getIpsFromRange("192.168.1.10-250")

NetFace face = LocalHost.netFaces.first()           // wrapper of NetworkInterface
face.name ; face.mac ; face.ip4List ; face.isConnected() ; face.firstIp4

MacAddress mac = MacAddress.fromString("3F:1A:99:05:3C:1B")
mac.separator = '-' ; mac.upperCase = false         // formatting; mac.bytes
```

## TCP / UDP

```groovy
// TCP server: closure receives the request string, returns the response
TCPServer server = new TCPServer(5050, { String cmd -> "echo: $cmd" })
server.quit()

// TCP client
TCPClient client = new TCPClient("192.168.1.20", 5050)   // timeout property (default 20000)
client.sendRequest(new TCPClient.Request("ping", { TCPClient.Response res ->
    if (res.sent) Log.i("reply: %s", res.toString())
}))

// UDP server (same closure pattern) and client:
UDPServer udp = new UDPServer(5051, { String cmd -> "pong: $cmd" })
UDPClient uc = new UDPClient("192.168.1.20".toInet4Address(), 5051)
uc.send("ping", { String response -> Log.i("%s", response) })   // packetSize arg optional (1024)
uc.quit()
```

## FtpClient

```groovy
FtpClient ftp = new FtpClient("host", 21, "user", "pass", "/remote/path")  // secure/verifyHost flags optional
ftp.connect() ; ftp.listFiles() ; ftp.getFile("data.csv") ; ftp.getImage("frame.jpg")
ftp.cd("subdir") ; ftp.cdToParent() ; ftp.uploadFile(new File("local.csv"))
ftp.noop() ; ftp.abort() ; ftp.disconnect()          // passive mode by default; ftp.active = true
```

## Gotchas

- Servers run in background threads; call `quit()` to release ports.
- `LocalHost.freePort` is not atomic — a race can hand the same port twice.
- `Host` default timeout is 10 s; lower it for responsive checks.
- Email ports drive TLS behavior automatically; misconfigured ports are the usual failure.
