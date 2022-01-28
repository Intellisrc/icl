# LOG Module (ICL.log)

SLF4J colorful logger with many options and easy to use. 
You can add customized loggers and personalize  
the way your logs look.

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/log)

### Example

```groovy
Log.v("Not so important message") // same as: Log.t(...)
Log.i("This is information. Today is: %s", SysClock.now.YMD)
Log.w("CPU is too high: {}% usage", 99)
Log.e("There was a horrible error: ", exception)
```

This logger provides mainly 2 different kind of printers:
1. print to Standard OUT/ERR
2. print to File

## Common Settings

Regardless of which printer you use (or if you use both 
at the same time), these are the general settings:

```properties
# file: config.properties

# Enable/Disable logs:
log.enable=true

# Set domain to highlight (automatically detected if you are using SysMain or SysService)
log.domain=com.example
log.domains=[com.example, net.example]
# Log level. Possible values: verbose/trace, debug, info, warn, error
log.level=info
# Log level when version includes 'SNAPSHOT'
log.level.snapshot=verbose
# Enable colors:
log.color=true
# Switch WHITE/BLACK colors
log.color.invert=false
# Ignore packages or classes
log.ignore=[org.eclipse.jetty, spark, LoudyClass]
# Lower or equal to this level will be ignored
log.ignore.level=debug
# What to show on logs:
log.show.time=true
log.show.thread=true
log.show.thread.short=true
log.show.logger=false
log.show.level.brackets=true
log.show.level.short=true
log.show.package=false
log.show.class=true
log.show.method=true
log.show.line.number=true
# Format used in logs: (anything supported by SysClock / LocalDateTime)
log.show.time.format=yyyy-MM-dd HH:mm:ss.SSS
# If `log.show.thread.short` is `true`, how to simplify name:
# `head`, means N characters from the beginning. `tail`, from the end.
log.show.thread.head=2
log.show.thread.tail=2
```

## Standard OUT/ERR printer

By default, this printer is enabled, if you want to turn it off, use:
```properties
log.print=false
```

If you want to split the logs into `stdout` and `stderr`, set:
```properties
log.print.split=true
```

If you want to print using `cache` (it will use a buffer before printing), set:
```properties
log.print.cache=true
```

All the `Common Settings` can be overwritten for this printer, just start with `log.print`, 
for example:

```properties
log.color=true
# To deactivate colors ONLY in print:
log.print.color=false
# To deactivate colors ONLY in stderr:
log.print.stderr.color=false

log.show.thread=false
# To show thread name ONLY in print:
log.print.show.thread=true
# To show thread name ONLY in stderr:
log.print.show.stderr.thread=true
```

## File printer

This printer will log into a file. There are some properties that you can set:

```properties
# Name of log file
log.file.name=system.log
# Directory in which logs will be stored
log.file.dir=log
# Compress logs (requires 'etc' module)
log.file.compress=true
# How many days to keep
log.file.days=7
# Also rotate other logs in the same directory
log.file.rotate.other=true
```
Additionally, you can override any of the general settings explained above,
to make them specific to file logging by prefixing the properties with `log.file`, 
for example:

```properties
log.file.color=false
log.file.show.package=true
log.file.ignore=[com.example.com]
```

If you need to access the loggers directly:

```groovy
FileLogger fileLogger = CommonLogger.default.fileLogger
PrintLogger printLogger = CommonLogger.default.printLogger
```

You can add your own printer, for example:

```groovy
class MyPrinter implements LoggableOutputLevels {
    // `Output` can also be initialized with any `PrintStream` of your choice.
    Output output             = new Output(SysInfo.getFile("test.log"))
    Level level               = Level.TRACE
    boolean enabled           = true
    boolean useColor          = false
    boolean colorInvert       = false
    boolean showDateTime      = true
    boolean showThreadName    = true
    boolean showThreadShort   = true
    boolean showLogName       = false
    boolean levelInBrackets   = false
    boolean levelAbbreviated  = false
    boolean showPackage       = false
    boolean showClassName     = true
    boolean showMethod        = true
    boolean showLineNumber    = true
    String dateFormatter      = "HH:mm:ss.SSSS"
    int showThreadHead        = 3
    int showThreadTail        = 3
    List<String> ignoreList   = []
    Level ignoreLevel         = Level.INFO //Will ignore from INFO to TRACE
}
MyPrinter myPrinter = new MyPrinter()
(LoggerFactory.getLogger("default") as CommonLogger).addPrinter(myPrinter)
```

If this module doesn't satisfy your logging needs, you can use any of the following SLF4J loggers:

* Log4J            (org.slf4j:slf4j-log4j12)
* JDK Logging      (org.slf4j:slf4j-jdk14)
* Jakarta Commons  (org.slf4j:slf4j-jcl)
* LogBack          (ch.qos.logback:logback-classic)
