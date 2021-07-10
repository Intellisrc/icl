This project is a series of modules which makes system development a lot easier and faster.

For example, the most used functionality are:
* Use configuration files easily, log and rotate logs, start your application in a more elegant way and more [core module]
* Set up backend web services (HTTP of WebSockets, using Spark-Java Web Framework) [web module]
* Create specialized threads, analyze them and manage them easily [thread module]
* Encrypt data or hash password easily and safely (using BountyCastle library) [crypt module]
* Create a terminal application in which you can interact using commands (using JLine) [term module]
* Communicate through a serial port (using JSSC) [serial module]
* Automatic create databases based on POJO objects or interact easily with a database (using JDBC) [db module]
* Grab images from a video source, convert and modify them or simply draw over them (using javaCV) [cv and img modules]
* Use cache, monitor your hardware or automate configuration changes (using Jedis) [etc module]

# Usage
How to use it in your project:

Maven:
```xml
<dependency>
  <groupId>com.intellisrc</groupId>
  <artifactId>MODULE</artifactId>
  <version>VERSION</version>
</dependency>
```
Gradle:
```groovy
dependecies {
    implementation 'com.intellisrc:MODULE:VERSION'
    // or extended annotation:
    implementation group: 'com.intellisrc', name: 'MODULE', version: 'VERSION'
}
```

In which `VERSION` is for example: `2.8.0`, and MODULE any of these: 
* `core`
* `crypt`
* `cv`
* `db`
* `etc`
* `img`
* `net`
* `serial`
* `term`
* `thread` 
* `web`

# Example Codes

You can find some examples for each module here: https://gitlab.com/intellisrc/common_examples/

# Modules 

Note: classes marked with `@`, are unlikely to be used directly. 

## core

> Basic functionality that is usually needed in any project. 
> For example, configuration, logging, executing commands, controlling services and displaying colors in console.

 * `AnsiColor`  : Color dictionary for Linux terminal
 * `Cmd`        : Execute system commands
 * `Config`     : Manage configuration files (by default config.properties)
 * `Log`        : Log messages
 * `SysClock`   : Provides simple methods to interact with LocalDateTime. Useful for Unit Testing.
 * `SysInfo`    : Get information about the system (usually paths)
 * `SysMain`    : Convert class into runnable class
 * `SysService` : Convert class into service (only one by project)
 * `Version`    : Return system version
 
## etc 
Includes : core
 
> Extra functionality which is usually very useful in any project.
> For example, monitoring Hardware, compressing or decompressing data, store data in memory cache,
> manage system configuration in a multithreading safe environment (using Redis as default), 
> simple operations with bytes, etc. 

 * `AutoConfig` : A set of classes to automate changes in configuration, backed up on disk (multithreading safe)
 * `Bytes`      : Common operations with byte[]
 * `Cache`      : Keep objects in memory to speed-up applications
 * `CacheObj`   : Generic implementation of `Cache`
 * `Calc`       : Provide common math calculations
 * `Hardware`   : Get information about OS
 * @ `JarResource`: Store resources in JAR
 * @ `Metric`   : Keep track of changes in values (used by Hardware)
 * `Pack`       : Methods to convert byte[] to int, long and back again
 * `Zip`        : Compress files
 
## db 
Includes : core, etc
 
> Manage databases, such as MySQL, SQLite, Berkeley, Postgresql. 
> Create, store and perform CRUD operations to data without
> having to use SQL (a light-weight implementation as alternative to Hibernate).

**NOTE** : You may need to include your database driver as dependency in order to use this module.

 * `Table`, `Model` : Similar to Hibernate but easier (and not dependent on Spring Framework)
 * `BerkeleyDB` : Key - Value No-SQL database which requires almost no configuration
 * @ `Data`       : Object returned by SQL databases and which can be converted into different classes
 * `Database`   : Main class which will handle connections to SQL databases
 * `Databases`  : Enable to use more than one database per application
 * `DB`         : Object to interact with the databases
 * @ `DBPool`     : Keep connections in a pool to increase performance
 * @ `Dummy`      : Dummy database (used for unit testing)
 * @ `DummyConnector` : Simulate and log DB connections (used for unite testing)
 * @ `JDBC`       : Manage JDBC connections
 * @ `JDBCConnector`  : A single JDBC connection
 * @ `MySQL`      : Default JDBC settings for MySQL databases
 * @ `PoolConnector`  : Implements a connector to be used in `DBPool`
 * @ `Query`      : Handles SQL queries
 * @ `SQLite`     : Default JDBC settings for SQLite databases
 
## net         
Includes : core, etc, crypt

> Classes related to networking. For example, sending emails through SMTP, 
> connecting or creating TCP/UDP servers, getting network interfaces and
> perform netmask calculations, etc.
 
 * `Email` : Verify email format
 * @ `ErrorMailer` : Send error and security alerts to administrator
 * `MacAddress` : Convert format from and to MacAddress: XX:XX:XX:XX
 * `Network` : Methods related to networking but not to NetworkInterface
 * `NetFace` : Simple representation of a Network Interface
 * `Smtp` : Class to send emails using SMTP server
 * `TCPClient` : TCP client
 * `TCPServer` : TCP server
 * `UDPClient` : UDP client
 * `UDPServer` : UDP server

## serial       
Includes : core, etc

> Manage serial communication easily. It uses JSSC library on the background.
 
 * @ `Seriable`     : Interface for common ports
 * `Serial`         : Use a serial port (connect, read, write, disconnect)
 * `SerialDummy`    : Dummy implementation of `Seriable` for Unit Testing
 * @ `SerialReader` : Class to only read data from a `Serial` port
 
## web          
Includes : core, etc

> Create restful HTTP (GET, POST, PUT, DELETE, etc) or WebSocket application services. 
> Manage JSON data from and to the server easily. It is build on top of
> Spark-Java Web Framework, so it is very flexible and powerful, but designed
> to be elegant and easier to use.
 
 * `HTTPServer` : Simple HTTP server implementation for static files. `WebService` offers much more than this one
 * `JSON` : Convert from and to JSON format
 * `Service` : Defines a single service to be used in `WebService`
 * @`Serviciable` : Generic interface for services
 * `ServiciableAuth` : Interface to be used in services which requires sessions
 * `ServiciableHTTPS` : Interface to be used to support SSL
 * `ServiciableSingle` : Defines a web service with a single `Service`
 * `ServiciableMultiple` : Defines a web service with multiple `Service`
 * `ServiciableWebSocket` : Defines a web service using websockets
 * @`Session` : Represents a user session
 * `WebService` : Main class used to create a web server. It wraps Spark
 * `WebSocketService` : Wrapper class to use with websockets
 * `WebSocketServiceClient` : Client for websocket services
 * services/`LoginService` : Implementation for login services
 * services/`LogService` : Implementation to browse logs in a browser

## crypt        
Includes : core, etc    

> Offers methods to encode, decode, hash and encrypt information. It is built using 
> the BountyCastle library and simplifying its usage without reducing its safety.

 * `Crypt` : Base class for all other classes in this module. It provides random generators and byte[] related methods
 * `AES` : Two way encryption (required fixed key length)
 * `PGP` : Two way encryption using OpenPGP (doesn't require fixed key length)
 * `Hash` : Provides many simple hash algorithms like MD5, SHA .. SHA512, and other available through BountyCastle (like: TIGER, WHIRLPOOL, etc.)
 * @ `Hashable` : Interface for `Hash` and `PasswordHash`
 * `PasswordHash` : Provides strong hashing algorithms for passwords, like: BCRYPT, SCRYPT and PBKDF2

## thread
Includes : core

> Manage Tasks (Threads) with priority and watches its performance. You can create
> parallel processes easily, processes which are executed in an interval, as a 
> service or after a specified amount of time. This module is very useful to help you
> to identify bottlenecks and to manage your main threads in a single place.
 
 * `Task` : Base class for all Tasks. This is the simplest of all
 * `BlockingTask` : Executes a Task in blocking state (not background)
 * `DelayedTask` : Executes a Task after N milliseconds
 * `IntervalTask` : Executes a Task every N milliseconds
 * `ParallelTask` : Executes several Tasks in a parallel pool
 * `ServiceTask` : Executes a Task in background that is intended to run forever
 * `Killable` : Interface to use to prepare task to be killed
 * @ `TaskInfo` : Keeps track of statistics of a Task
 * @ `TaskLoggable` : Interface to use to provide information to Log
 * @ `TaskManager` : Controls all tasks (start, stop, restart)
 * @ `TaskPool` : Group several Tasks and keep track of them
 * `Tasks` : Generates report of all tasks and serves as entry point to add tasks to be monitored
 * @ `ThreadPool` : Custom ThreadPoolExecutor object (Java management for threads)

## term         
Includes : core

> Anything related to terminal is in this module (except AnsiColor, which is in core). 
> It uses JLine to help you create interactive terminal (console) applications easily.
> It also contains some tools to display progress with colors.
 
 * `Consolable` : Interface to implement for any console application
 * `Console` : Main console class which uses `Consolable` applications. It wraps jLine
 * `ConsoleDefault` : Simple implementation of `Consolable` which provides common commands
 * @ `MatchAnyCompleter` : Utility to find partial matches to commands
 * `Progress` : Show a progress bar in a terminal (it has multiple implementations and options)

## img
Includes : core

> Classes for using Images (BufferedImage, File, FrameShot) and non-opencv related code, 
> trying to keep dependencies to a minimum. It also includes common geometric operations.

 * `BuffImgTools` : Do operations on BufferedImages (rotate, crop, etc)
 * `FileImgTools` : Do operations on image files (convenient wrapper around BuffImgTools)
 * `Converter` : Convert from and to File, BufferedImage, byte array, etc.
 * `FrameShot` : Represents a single frame (BufferedImage with name)
 * `Metry` : Performs geometric operations (e.g, Trignonometry)

## cv           
Includes : core, img

> Classes for Computer Vision (extension to OpenCV). Convert image formats, crop, rotate images
> or draw objects on top of them. It simplifies grabbing images from any video source.

 * `Converter` : Convert from and to CV image formats (extends img.Converter)
 * `CvTools` : Perform common operations in images : rotate, resize, etc
 * `FrameShot` : Extends img.FrameShot to support CV image format
 * @ `JpegFormat` : JPEG Format
 * @ `MjpegFormat` : MJPEG Format
 * @ `MjpegFrame` : MJPEG single frame
 * @ `MjpegInputStream` : Provides MJPEG as InputStream
 * @ `VideoGrab` : Extract frames from most video formats
 * `BufferedVideoGrab` : Get BufferedImages or FrameShot/CvFrameShot from a MJPEG stream
 * `FileVideoGrab` : Get File or FrameShot/CvFrameShot images from a directory (as frames)
 * `FrameVideoGrab` : Get Frame or FrameShot/CvFrameShot objects from video files

# Notes about upgrading to 2.8.x from 2.7.x

Starting from 2.8, this library no longer includes many dependencies, so you need to 
add them separately as you need. This was done to make this library more flexible by allowing
you to choose your desired versions, and reduce the compilation time. Below each module I'm
including the recommended version (the one used during compilation).

* `core` : Groovy version is now up to you (required by any module). 
    * `org.codehaus.groovy:groovy-all:3.0.7`
* `etc`  : Jedis needs to be included if you use `@AutoConfig`
    * `redis.clients:jedis:3.6.1`
* `cv`   : JavaCV needs to be included.
    * `org.bytedeco:javacv-platform:1.5`
* `db`   : Database drivers need to be included:
    * `com.sleepycat:je:18.3.12` (BarkeleyDB)
    * `mysql:mysql-connector-java:8.0.25`
    * `org.postgresql:postgresql:42.2.22`
    * `org.xerial:sqlite-jdbc:3.36.0`
  
The following modules already include the needed libraries (no need to add them):

* `serial` : JSSC library (`org.scream3r:jssc:2.8.0`) 
* `term`   : JLine library (`org.jline:jline:3.20.0`)
* `web`  : Spark Framework (`com.sparkjava:spark-core:2.9.3`)
