# ICL Library

This library can help you to write less code and speedup the development of your next great application. It is particularly 
designed to accomplish most of the common tasks required by backend services.

## Goal

Provide the best implementation or wrap the best libraries to make it easier to accomplish most common development tasks 
(like web applications, database connections, security, etc).

## What is in it?

With this library you can:

- Simplify your code by using [GroovyExtend](https://gitlab.com/intellisrc/groovy-extend) library (already included)
- Use configuration files easily, log and rotate logs, start your application in a more elegant way and more [core module](#core) and [log module](#log)
- Set up back-end web services (HTTP or WebSockets, using Spark-Java Web Framework) [web module](#web)
- Create specialized threads, analyze them and manage them easily [thread module](#thread)
- Manage databases using only Java objects or interact with them using a simple fluid SQL query builder (using JDBC) [db module](#db)
- Send emails with attachments through SMTP, create FTP, UDP or TCP servers and clients [net module](#net)
- Encrypt data or hash passwords easily and safely (using BountyCastle library) [crypt module](#crypt)
- Create a terminal application in which you can interact using commands (using JLine) [term module](#term)
- Communicate through a serial port (JSSC wrapper) [serial module](#serial)
- Grab images from a video source, convert and modify them or simply draw over them (tools for javaCV) [cv module](#cv) and [img module](#img)
- Use cache, monitor your hardware or automate configuration changes (using Jedis or BerkeleyDB) [etc module](#etc)

**NOTE** : This library was designed using Groovy, but it can be used in Java and Kotlin (however, all the examples in the
documentation are in Groovy).

## Usage
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
dependencies {
    implementation 'com.intellisrc:MODULE:VERSION'
    // or extended annotation:
    implementation group: 'com.intellisrc', name: 'MODULE', version: 'VERSION'
}
```

Follow the instructions on the last published version for each module in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/)

In which `VERSION` is for example: `2.8.x`, and MODULE any of these: 
* [core](#core)
* [crypt](#crypt)
* [cv](#cv)
* [db](#db)
* [etc](#etc)
* [img](#img)
* [log](#log)
* [net](#net)
* [serial](#serial)
* [term](#term)
* [thread](#thread)
* [web](#web)

You can find some examples for each module here: https://gitlab.com/intellisrc/common_examples/

## Modules 

For more detailed explanation, click on the module title.<br>

**Notes**: 
* classes marked with `★` are the most commonly used classes for that module. 
* classes marked with `@`, are unlikely to be used directly, but may be of your interest. 

### [core](modules/core/README.md)

> Basic functionality that is usually needed in any project. 
> For example, configuration, logging, executing commands, controlling services and 
> displaying colors in the console. [read more...](modules/core/README.md)

|     | Class               | Usage                                                                          |
|-----|---------------------|--------------------------------------------------------------------------------|
|     |`AnsiColor`          | Color dictionary for Linux terminal                                            |
|     |`Cmd`                | Execute system commands                                                        |
| ★  |`Config`             | Manage configuration files (by default config.properties)                      |
| ★  |`Log`                | Log messages to `SLF4J`                                                        |
| @   |`StringProperties` | Base class for properties setters and getters                                  |
| ★   |`SysClock`           | Provides simple methods to interact with LocalDateTime. Useful for Unit Testing. |
| ★   |`SysInfo`            | Get information about the system (usually paths)                               |
|     |`SysMain`            | Convert class into runnable class                                              |
| ★   |`SysService`         | Convert class into service (only one by project)                               |
|     |`Version`            | Return system/application version and makes it easy to handle versions         |

[Documentation](modules/core/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#core) 

### [log](modules/log/README.md)
Includes : core

> SLF4J colorful logger with many options and easy to use.
> You can add customized loggers and personalize
> the way your logs look.
> Generally you will use this module through `core.Log` class.
> [read more...](modules/log/README.md)

|     | Class                 | Usage                                           |
|-----|-----------------------|-------------------------------------------------|
| @   | `CommonLogger`        | Main logger which will handle multiple printers |
| @   | `BaseLogger`          | Provides basic settings for printers            |
| @   | `PrintLogger`         | Prints to stdout (supports cache)               |
| @   | `PrintStdErrLogger`   | Prints to stderr (supports cache)               |
| @   | `FileLogger`          | Prints to a file                                |
| @   | `CommonLoggerFactory` | Used by SLF4J to get `CommonLogger` instance    |
| @   | `CommonLoggerService` | Service provider for SLF4J                      |

[Documentation](modules/log/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#log)

### [etc](modules/etc/README.md)
Includes : core
 
> Extra functionality which is usually very useful in any project.
> For example, monitoring Hardware, compressing or decompressing data, store data in memory cache,
> manage system configuration in a multithreading safe environment (using Redis or BerkeyleyDB),
> simple operations with bytes, etc. [read more...](modules/etc/README.md)

|     | Class         | Usage                                                                                                             |
|-----|---------------|-------------------------------------------------------------------------------------------------------------------|
 | ★   | `AutoConfig`  | A set of classes to automate changes in configuration with documentation, backed up on disk (multithreading safe) |
 | ★   | `BerkeleyDB`  | Key - Value No-SQL database which requires almost no configuration (wrapper)                                      |
 |     | `Bytes`       | Common operations with byte[]                                                                                     |
 | ★   | `Cache`       | Keep objects in memory to speed-up applications                                                                   |
 |     | `CacheObj`    | Generic implementation of `Cache`                                                                                 |
 |     | `Calc`        | Provide common math calculations                                                                                  |
 | ★   | `Hardware`    | Get information about OS                                                                                          |
 | ★   | `JSON`        | Convert from and to JSON format (Using Groovy JSON)                                                               |
 |     | `Mime`        | Get Mime types from files or streams                                                                              |
 | @   | `JarResource` | Store resources in JAR                                                                                            |
 | @   | `Metric`      | Keep track of changes in values (used by Hardware)                                                                |
 |     | `Pack`        | Methods to convert byte[] to int, long and back again                                                             |
 | ★   | `Redis`       | A wrapper around `Jedis` to make it even easier                                                                   |
 | ★   | `YAML`        | Convert from and to YAML format                                                                                   |
 | ★   | `Zip`         | Compress files                                                                                                    |

[Documentation](modules/etc/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#etc)

### [db](modules/db/README.md) 
Includes : core, etc
 
> Manage SQL databases, such as MySQL, SQLite, Postgresql. 
> (for no-sql databases, see `etc` module)
> Create, store and perform CRUD operations to data without
> having to use SQL (a light-weight implementation as an alternative to Hibernate).
> [read more...](modules/db/README.md)

**NOTE** : You may need to include your database driver as dependency in order to use this module.

|     | Class                      | Usage                                                                              |
|-----|----------------------------|------------------------------------------------------------------------------------|
 | ★   | `Database`                 | Main class which will handle connections to SQL databases                          |
 | ★   | `DB`                       | Object to interact with the databases (Fluid SQL query builder)                    |
 |     | `Query`                    | Handles SQL queries                                                                |
 | ★   | `Table`, `Model`           | Automatic table creation/update and CRUD data manipulations                        |
 |     | `@TableMeta`, `@ModelMeta` | Additional options to the above classes                                            |
 | @   | `Data`                     | Object returned by SQL databases and which can be converted into different classes |
 | @   | `DBPool`                   | Keep connections in a pool to increase performance                                 |
 | @   | `Dummy`                    | Dummy database (used for unit testing)                                             |
 | @   | `DummyConnector`           | Simulate and log DB connections (used for unite testing)                           |
 | @   | `JDBCConnector`            | A single JDBC connection                                                           |
 | @   | `PoolConnector`            | Implements a connector to be used in `DBPool`                                      |
 | @   | `JDBC`                     | Extend this class to create your own JDBC connection                               |
|     | `Derby`                    | JDBC implementation for Derby databases                                            |
|     | `Firebird`                 | JDBC implementation for Firebird databases                                         |
|     | `MySQL`                    | JDBC implementation for MySQL databases                                            |
|     | `MariaDB`                  | JDBC implementation for MariaDB databases                                          |
|     | `Oracle`                   | JDBC implementation for Oracle databases                                           |
|     | `PostgreSQL`               | JDBC implementation for PostgreSQL databases                                       |
 |     | `SQLite`                   | JDBC implementation for SQLite databases                                           |
|     | `SQLServer`                | JDBC implementation for SQL Server databases                                       |

[Documentation](modules/db/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#db)

### [net](modules/net/README.md)         
Includes : core, etc, crypt

> Classes related to networking. For example, sending emails through SMTP, 
> connecting or creating TCP/UDP servers, getting network interfaces and
> perform netmask calculations, etc. [read more...](modules/net/README.md)

|     | Class         | Usage                                                     |
|-----|---------------|-----------------------------------------------------------|
 | ★   | `Email`       | Verify email format                                       |
 | @   | `ErrorMailer` | Send error and security alerts to administrator           |
 |     | `MacAddress`  | Convert format from and to MacAddress  XX:XX:XX:XX        |
 |     | `Network`     | Methods related to networking but not to NetworkInterface |
 |     | `NetFace`     | Simple representation of a Network Interface              |
 | ★  | `Smtp`        | Class to send emails using SMTP server                    |
 |     | `FtpClient`   | FTP client                                                |
 |     | `TCPClient`   | TCP client                                                |
 |     | `TCPServer`   | TCP server                                                |
 |     | `UDPClient`   | UDP client                                                |
 |     | `UDPServer`   | UDP server                                                |

[Documentation](modules/net/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#net)

### [serial](modules/serial/README.md)
Includes : core, etc

> Manage serial communication easily. It uses the JSSC library in the background.
> [read more...](modules/serial/README.md)

|     | Class          | Usage                                                |
|-----|----------------|------------------------------------------------------|
 | @   | `Seriable`     | Interface for common ports                           |
 | ★   | `Serial`       | Use a serial port (connect, read, write, disconnect) |
 |     | `SerialDummy`  | Dummy implementation of `Seriable` for Unit Testing  |
 | @   | `SerialReader` | Class to only read data from a `Serial` port         |

[Documentation](modules/serial/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#serial)

### [web](modules/web/README.md)
Includes : core, etc

> Create restful HTTP (GET, POST, PUT, DELETE, etc) or WebSocket application services. 
> Manage JSON data from and to the server easily. It is build on top of
> Spark-Java Web Framework, so it is very flexible and powerful, but designed
> to be elegant and easier to use. [read more...](modules/web/README.md)

|     | Class                    | Usage                                                                                           |
|-----|--------------------------|-------------------------------------------------------------------------------------------------|
 |     | `HTTPServer`             | Simple HTTP server implementation for static files. `WebService` offers much more than this one |
 | ★   | `Service`                | Defines a single service to be used in `WebService`                                             |
 | @   | `Serviciable`            | Generic interface for services                                                                  |
 |     | `ServiciableAuth`        | Interface to be used in services which requires sessions                                        |
 |     | `ServiciableHTTPS`       | Interface to be used to support SSL                                                             |
 |     | `ServiciableSingle`      | Defines a web service with a single `Service`                                                   |
 | ★   | `ServiciableMultiple`    | Defines a web service with multiple `Service`                                                   |
 | ★   | `ServiciableWebSocket`   | Defines a web service using websockets                                                          |
 | @   | `Session`                | Represents a user session                                                                       |
 | ★   | `WebService`             | Main class used to create a web server. It wraps Spark                                          |
 | ★   | `WebSocketService`       | Wrapper class to use with websockets                                                            |
 | ★   | `WebSocketServiceClient` | Client for websocket services                                                                   |
 |     | services/`LoginService`  | Implementation for login services                                                               |
 |     | services/`LogService`    | Implementation to browse logs in a browser                                                      |

[Documentation](modules/web/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#web)

### [crypt](modules/crypt/README.md)
Includes : core, etc    

> Offers methods to encode, decode, hash and encrypt information. It is built using 
> the amazing BouncyCastle library by simplifying its usage without reducing its safety.
> [read more...](modules/crypt/README.md)

|     | Class          | Usage                                                                                                                                  |
|-----|----------------|----------------------------------------------------------------------------------------------------------------------------------------|
 |     | `Crypt`        | Base class for all other classes in this module. It provides random generators and byte[] related methods                              |
 | ★   | `AES`          | Two way encryption (fixed key length required)                                                                                         |
 | ★   | `PGP`          | Two way encryption using OpenPGP (doesn't require fixed key length)                                                                    |
 | ★   | `Hash`         | Provides many simple hash algorithms like MD5, SHA .. SHA512, and others available through BountyCastle (like: TIGER, WHIRLPOOL, etc.) |
 | @   | `Hashable`     | Interface for `Hash` and `PasswordHash`                                                                                                |
 | ★   | `PasswordHash` | Provides strong hashing algorithms for passwords, like: BCRYPT, SCRYPT and PBKDF2                                                      |
|     | `LpCode`       | Provides String obfuscation                                                                                                            |

[Documentation](modules/crypt/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#crypt)

### [thread](modules/thread/README.md)
Includes : core

> Manage Tasks (Threads) with priority and watch its performance. You can create
> parallel processes easily, processes which are executed in an interval, as a 
> service or after a specified amount of time. This module is very useful to help you
> to identify bottlenecks and to manage your main threads in a single place.
> [read more...](modules/thread/README.md)

|     | Class          | Usage                                                                                |
|-----|----------------|--------------------------------------------------------------------------------------|
 |     | `Task`         | Base class for all Tasks. This is the simplest of all                                |
 | ★   | `BlockingTask` | Executes a Task in blocking state (not background)                                   |
 | ★   | `DelayedTask`  | Executes a Task after N milliseconds                                                 |
 | ★   | `IntervalTask` | Executes a Task every N milliseconds                                                 |
 | ★   | `ParallelTask` | Executes several Tasks in a parallel pool                                            |
 | ★   | `ServiceTask`  | Executes a Task in background that is intended to run forever                        |
 |     | `Killable`     | Interface to use to prepare task to be killed                                        |
 | @   | `TaskInfo`     | Keeps track of statistics of a Task                                                  |
 | @   | `TaskLoggable` | Interface to use to provide information to Log                                       |
 | @   | `TaskManager`  | Controls all tasks (start, stop, restart)                                            |
 | @   | `TaskPool`     | Group several Tasks and keep track of them                                           |
 | ★   | `Tasks`        | Generates report of all tasks and serves as entry point to add tasks to be monitored |
 | @   | `ThreadPool`   | Custom ThreadPoolExecutor object (Java management for threads)                       |

[Documentation](modules/thread/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#thread)

### [term](modules/term/README.md)         
Includes : core

> Anything related to terminal is in this module (except AnsiColor, which is in core). 
> It uses JLine to help you create interactive terminal (console) applications easily.
> It also contains some tools to display progress with colors.
> [read more...](modules/term/README.md)

|     | Class               | Usage                                                                           |
|-----|---------------------|---------------------------------------------------------------------------------|
 |     | `Consolable`        | Interface to implement for any console application                              |
 | ★   | `Console`           | Main console class which uses `Consolable` applications. It wraps jLine         |
 | ★   | `ConsoleDefault`    | Simple implementation of `Consolable` which provides common commands            |
 | @   | `MatchAnyCompleter` | Utility to find partial matches to commands                                     |
 | ★   | `Progress`          | Show a progress bar in a terminal (it has multiple implementations and options) |

[Documentation](modules/term/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#term)

### [img](modules/img/README.md)
Includes : core

> Classes for using Images (BufferedImage, File, FrameShot) and non-opencv related code, 
> trying to keep dependencies to a minimum. It also includes common geometric operations.
> [read more...](modules/img/README.md)

|     | Class          | Usage                                                                 |
|-----|----------------|-----------------------------------------------------------------------|
 | ★   | `BuffImgTools` | Do operations on BufferedImages (rotate, crop, etc)                   |
 | ★   | `FileImgTools` | Do operations on image files (convenient wrapper around BuffImgTools) |
 | ★   | `Converter`    | Convert from and to File, BufferedImage, byte array, etc.             |
 | ★   | `FrameShot`    | Represents a single frame (BufferedImage with name)                   |
 |     | `Metry`        | Performs geometric operations (e.g, Trigonometry)                     |

[Documentation](modules/img/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#img)

### [cv](modules/cv/README.md)
Includes : core, img

> Classes for Computer Vision (extension to OpenCV). Convert image formats, crop, rotate images
> or draw objects on top of them. It simplifies grabbing images from any video source.
> [read more...](modules/cv/README.md)

|     | Class               | Usage                                                                 |
|-----|---------------------|-----------------------------------------------------------------------|
 | ★   | `Converter`         | Convert from and to CV image formats (extends img.Converter)          |
 | ★   | `CvTools`           | Perform common operations in images : rotate, resize, etc             |
 | ★   | `FrameShot`         | Extends img.FrameShot to support CV image format                      |
 | @   | `JpegFormat`        | JPEG Format                                                           |
 | @   | `MjpegFormat`       | MJPEG Format                                                          |
 | @   | `MjpegFrame`        | MJPEG single frame                                                    |
 | @   | `MjpegInputStream`  | Provides MJPEG as InputStream                                         |
 | @   | `VideoGrab`         | Extract frames from most video formats                                |
 |     | `BufferedVideoGrab` | Get BufferedImages or FrameShot/CvFrameShot from a MJPEG stream       |
 |     | `FileVideoGrab`     | Get File or FrameShot/CvFrameShot images from a directory (as frames) |
 | ★   | `FrameVideoGrab`    | Get Frame or FrameShot/CvFrameShot objects from video files           |

[Documentation](modules/cv/README.md) / [JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/public/#cv)

## Dependencies

Starting from 2.8, this library no longer includes some dependencies, so you need to 
add them separately as you need. This was done to make this library more flexible by allowing
you to choose your library versions, reduce the compilation time and your project jar size. 

Below each module, I'm including the recommended version (the one used during compilation).

* `core` : Groovy version is now up to you (required by any module). 
    * `org.codehaus.groovy:groovy-all:3.0.7`
* `db`   : Database drivers need to be included (Choose the one(s) which match(es) your database(s)):
    * `com.microsoft.sqlserver:mssql-jdbc:9.4.1.jre11`
    * `com.oracle.database.jdbc:ojdbc11:21.4.0.0`
    * `mysql:mysql-connector-java:8.0.25`
    * `org.apache.derby:derby:10.15.2.0`
    * `org.firebirdsql.jdbc:jaybird:4.0.5.java11`
    * `org.mariadb.jdbc:mariadb-java-client:2.7.4`
    * `org.postgresql:postgresql:42.3.1`
    * `org.xerial:sqlite-jdbc:3.36.0.2`
* `etc`  :
  * Jedis (`redis.clients:jedis:3.6.3`)
  * BerkeleyDB (`com.sleepycat:je:18.3.12`)

The following modules are already included in the specified modules:

* `cv`     
  * JavaCV (`org.bytedeco:javacv-platform:1.5`)
* `crypt`  
  * Bounty Castle (`org.bouncycastle:bcprov-jdk15on:1.69`, `org.bouncycastle:bcpg-jdk15on:1.69`, `org.bouncycastle:bcprov-ext-jdk15on:1.69`)
* `net`    
  * Apache Common Net (`commons-net:commons-net:3.8.0`) 
  * JavaX Mail(`com.sun.mail:javax.mail:1.6.2`)
* `serial` 
  * JSSC library (`org.scream3r:jssc:2.8.0`) 
* `term`   
  * JLine library (`org.jline:jline:3.20.0`)
* `web`    
  * Spark Framework (`com.sparkjava:spark-core:2.9.3`)

## Recommendations

If you need a library to build your web-based user interfaces (using javascript), check [M2D2](https://gitlab.com/intellisrc/m2d2)

## Acknowledgments

Developed with IntelliJ Ultimate Edition. I would like to thank [JetBrains](https://jb.gg/OpenSource) for their support.