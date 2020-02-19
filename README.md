This project is a series of modules which makes system developement a lot more easier and faster.

# Modules 

Note: classes marked with `@`, are unlikely to be used directly. 

## core

Basic functionality that is usually needed in any project.

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
 Depends on : core
 
 Extra functionality which is usually very useful in any project.
 
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
Depends on : etc
 
 Manage databases, such as MySQL, SQLite, Berkeley, Postgresql
 
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
Depends on : etc, crypt

 Classes related to networking
 
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
Depends on : etc

 Manage serial communication
 
 * @ `Seriable`     : Interface for common ports
 * `Serial`         : Use a serial port (connect, read, write, disconnect)
 * `SerialDummy`    : Dummy implementation of `Seriable` for Unit Testing
 * @ `SerialReader` : Class to only read data from a `Serial` port
 
## web          
Depends on : etc

 Classes related to HTTP or alike protocols
 
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
Depends on : etc    

 Offers methods to encode, decode and encrypt information

 * `Crypt` : Base class for all other classes in this module. It provides random generators and byte[] related methods
 * `AES` : Two way encryption (required fixed key length)
 * `PGP` : Two way encryption using OpenPGP (doesn't require fixed key length)
 * `Hash` : Provides many simple hash algorithms like MD5, SHA .. SHA512, and other availables through BountyCastle (like: TIGER, WHIRLPOOL, etc.)
 * @ `Hashable` : Interface for `Hash` and `PasswordHash`
 * `PasswordHash` : Provides strong hashing algorithms for passwords, like: BCRYPT, SCRYPT and PBKDF2

## thread
Depends on : core

 Manage Tasks (Threads) with priority and watches its performance
 
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
Depends on : core

 Anything related to terminal is in this module (except AnsiColor, which is in core)
 
 * `Consolable` : Interface to implement for any console application
 * `Console` : Main console class which uses `Consolable` applications. It wraps jLine
 * `ConsoleDefault` : Simple implementation of `Consolable` which provides common commands
 * @ `MatchAnyCompleter` : Utility to find partial matches to commands
 * `Progress` : Show a progress bar in a terminal

## img
Depends on : core

 Classes for using Images (BufferedImage, File, FrameShot) and non-opencv related code, trying to keep dependencies to a minimum. 
 It also include common geometric operations

 * `BuffImgTools` : Do operations on BufferedImages (rotate, crop, etc)
 * `FileImgTools` : Do operations on image files (convenient wrapper around BuffImgTools)
 * `Converter` : Convert from and to File, BufferedImage, byte array, etc.
 * `FrameShot` : Represents a single frame (BufferedImage with name)
 * `Metry` : Performs geometric operations (e.g, Trignonometry)

## cv           
Depends on : core, img

Classes for Computer Vision (extension to OpenCV)

 * `Converter` : Convert from and to CV image formats (extends img.Converter)
 * `CvTools` : Perform common operations in images : rotate, resize, etc
 * `FrameShot` : Extends img.FrameShot to support CV image format
 * @ `JpegFormat` : JPEG Format
 * @ `MjpegFormat` : MJPEG Format
 * @ `MjpegFrame` : MJPEG single frame
 * @ `MjpegInputStream` : Provides MJPEG as InputStream
 * @ `VideoGrab` : Extract frames from most of video formats
 * `BufferedVideoGrab` : Get BufferedImages or FrameShot/CvFrameShot from a MJPEG stream
 * `FileVideoGrab` : Get File or FrameShot/CvFrameShot images from a directory (as frames)
 * `FrameVideoGrab` : Get Frame or FrameShot/CvFrameShot objects from video files
