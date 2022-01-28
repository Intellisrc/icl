# NET Module (ICL.net)

Classes related to networking. For example,  
sending emails through SMTP, connecting or  
creating TCP/UDP servers, getting network  
interfaces and perform netmask calculations, etc.

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/net)

## Email
By using this class, you can be sure the emails
conform with the expected format.
```groovy
try {
    Email email = new Email("some@example-com")
} catch(EmailMalformedException e) {
    Log.e("Email was not correct", e)
}
```

## Smtp
Class to send emails using SMTP server. For convenience, you
can set up most of the settings in the configuration file:

### Configuration
```properties
# file: config.properties

mail.smtp.host=mail.example.com
# If port 465 or 587 are used, encryption settings will be added automatically
mail.smtp.port=25
# If no username is set, authentication will be disabled
mail.smtp.username=user9000
mail.smtp.password=top-secret
mail.smtp.from=sender@example.com
mail.smtp.fromName=Sender Name
mail.smtp.replyTo=no-reply@example.com

# If most or all of the messages are destined to a single account:
mail.smtp.defaultTo=info@example.com

# When true, it will only report in logs and it won't send the message
mail.smtp.simulate=false
```

### Example

```groovy
Smtp smtp = new Smtp()
smtp.addAttachment(new File("manual.pdf"))
if(smtp.send(new Email("user@example.com"), "This is a test", "Please ignore this message")) {
    Log.i("Mail sent successfully")
}
// Other options:
smtp.sendDefault(subject, body)
smtp.send(listOfAddresses, subject, body)
```

## MacAddress
Convert format from and to MacAddress: XX:XX:XX:XX
```groovy
MacAddress mac = MacAddress.fromString("AA:BB:CC:00:11") //it will be stored as byte[]
println mac.toString()
```

## Network
Useful methods about networking

### Examples

```groovy
// Get all IPs in a range
List<Inet4Address> addresses = Network.getIpsFromRange("192.168.1.10-250")

// Get all network interfaces
List<NetFace> interfaces = Network.networkInterfaces()
```

## NetFace
Simple representation of a Network Interface, it stores:

* Interface name
* Mac Address
* IP (Inet4Address and Inet6Address)

## FtpClient
Simple to use FTP client wrapper of `FTPClient` from Apache Commons

### Configuration
```properties
# By default the client will use 'passive' mode, you can change it with:
ftp.active=true
```

### Example

```groovy
FtpClient ftp = new FtpClient(ip, port, user, pass, path)
if (ftp.connect()) {
    ftp.cd("pictures")
    List<String> files = ftp.listFiles()
    File file = ftp.getFile(files.first())
    BufferedImage img = ftp.getImage(files.last())
    ftp.noop()
    ftp.cdToParent()
    ftp.uploadFile(file)
    ftp.abort()
    ftp.disconnect()
}
```

## TCPServer && TCPClient
TCP servers and clients implemented using java `ServerSocket` class.

### Example
```groovy
TCPServer server = new TCPServer(port, {
    String clientCommand ->
        /* do something with the command */
        return "pong" //Response
})

TCPClient client = new TCPClient(serverIP, port)
client.sendRequest(new Request("ping", {
    Response response ->
        if(response.sent) {
            println response.toString() //output: "pong"
        }
}))

server.quit()
```

## UDPServer && UDPClient
UDP server is an implementation of Java `DatagramSocket`:

### Example

```groovy
UDPServer server = new UDPServer(port, {
    String clientCommand ->
        return "pong" // response
})

UDPClient client = new UDPClient(serverIP, port)
client.send("ping", {
    String response ->
        println response // output: "pong"
})
client.quit()
```

