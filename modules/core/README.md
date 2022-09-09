# CORE Module (ICL.core)

Basic functionality that is usually needed in any project. For example, configuration, 
logging, executing commands, controlling services and displaying colors in console.

[JavaDoc](https://intellisrc.gitlab.io/common/#core)

The following is an overview of this module:

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/core)

## SysMain and SysService

A more elegant way to start your applications. It is an alternative to `public static void main(String[] args)`.

### Example:

```groovy
class Main extends SysMain {
    // The following line is required:
    static { main = new Main() }

    @Override
    void onStart() {
        // Your code goes here
        String firstArg = args.first()
        Log.i("The first argument was: %s", firstArg)
    }
}
```
```groovy
class Main extends SysService {
  // The following line is required:
  static { service = new Main() }

  @Override
  void onStart() {
    // Your code goes here
    String firstArg = args.first()
    Log.i("The first argument was: %s", firstArg)
  }

  @Override
  void onSleep() {
    Log.i("Service is running...")
  }

  @Override
  void onStop() {
    Log.i("The service has stopped.")
  }
}
```

The main difference between `SysMain` and `SysService` is that the later
will not exit after the last line of execution, it contains the method
`onSleep()` which is executed every second, and it is ready to be used
as a service.

You can add custom methods, for example:

```groovy
class Main extends SysMain {
  // The following line is required:
  static { main = new Main() }

  @Override
  void onStart() {
    // Your code goes here
    String firstArg = args.first()
    Log.i("The first argument was: %s", firstArg)
  }
  
  void onCustom() {
    args.each {
      println it
    }
  }
}
```
To call `onCustom` method, you need to pass as argument `custom` to your 
execution command line:

```bash
java -jar my-app-1.0.jar custom firstArg secondArg
```
if there is no `onCustom` method, then the arguments will be sent to `onStart` method.

## Config

The easiest way to read and write settings from a file (read on initialization).
By default, loads the configuration from `config.properties` file. It can handle 
multiple types besides primitive types, like: Lists, Maps, URL/URI, Files, InetAddress,
LocalDate, LocalTime, LocalDateTime, byte array and custom objects through `toString()` method.

This class is not thread safe, if you need to read and write your system
configuration from multiple threads, use `AutoConfig` in the `etc` module.

### Example:

```groovy
enum Continent {
  AFRICA, ANTARCTICA, ASIA, EUROPE, NORTH_AMERICA, OCEANIA, SOUTH_AMERICA 
}
class Test {
    String systemName               = Config.get("system.name", "Default Name")
    int threads                     = Config.get("system.threads", 4)
    double fps                      = Config.get("system.fps", 24.5d)
    Optional<LocalTime> cleanTime   = Config.getTime("system.clean.time")
    Optional<Inet4Address> ip       = Config.getInet4("system.ip")
    Optional<URL> siteUrl           = Config.getURL("system.url")
    Optional<File> dataFile         = Config.getFile("system.data.file")
    List flags                      = Config.getList("system.flags")
    Map options                     = Config.getMap("system.options")
    Continent continent1            = Config.getEnum("system.continent", Continent)  //For Enum, you need to pass the class
    Continent continent2            = Config.getEnum("system.continent", Continent.ASIA)  //In case of default value
}
```

Example of `config.properties`:

```properties
system.name=My Cool System
system.threads=8
system.fps=133.333
system.clean.time=23:54
system.ip=192.168.0.1
system.url=http://localhost:9999/
system.data.file=~/data.file
system.flags=[readonly, superadmin]
system.options={ debug: false, export: true }
```

To access `System.properties` in the same way, you can use:

```groovy
String linuxVersion = Config.system.get("os.version")
```

To access `Environment variables` as well, you can use:

```groovy
String javaVersion = Config.env.get("JAVA_VERSION")
```
or (keys automatically translated):
```groovy
String javaVersion = Config.env.get("java.version")
```

To search in any configuration (config file, system or environment), you can use:
```groovy
String version = Config.any.get("version")
```
This is particularly useful if you are using docker. You can set configuration either
in the config file or pass it by environment.

## Log 

Provides a simpler interface to `SLF4J`. If no SLF4J logger is present, it provides a very simple
printer to stdout. We recommend you to use the `log` package, which provides many options, colorful
outputs (optional) and log file management (rotation, compression, etc)

### Settings:

Without any `SLF4J` logger, only stdout will be used. For extended options,
you need to use `log` module.

```properties
# file: config.properties

# If you want to turn OFF the logs:
log.enable=false
```

### Example:

```groovy
// Verbose level, for maximum output details (gray color):
Log.v("This is very verbose output: %.2f", 99.9999d)
// Debug level, for details - SLF4J parametrization supported - (white color): 
Log.d("When you want to debug something: {}", 1000)
// Logging some information (blue color):
Log.i("This is {}% about %s", 90, "Log")
// Displaying a warning (yellow color):
Log.w("Beware! this is a warning")

try {
    int err = 100 / 0
} catch(Exception e) {
    // Exception is passed to Log, so it can print all the detail (red color):
    Log.e("You can't do that", e)
}
// by default `Log.e` will print the stack trace, if you want to print it on demand:
Log.stackTrace("Custom message is optional")
```

Strings and SLF4J parameters do automatic conversion from some objects:

**NOTE** : SLF4J parameters `{}` are the same as `%s`:
```groovy
Log.i("float, doubles, etc are displayed with 4 digits: %s", 100/3d) 
Log.i("LocalTime, LocalDate and LocalDateTime are converted automatically: {}", SysClock.now)
Log.i("IpAddress are printed correctly: %s", someInet4Address)
Log.i("Files print absolute path: %s", new File("my.file"))
Log.i("Byte arrays are printed as HEX string: {}", "hello".bytes)
Log.i("Any other object is printed using 'toString()' method: %s", myObject)
```

## AnsiColor

Add colors to your terminal (Linux and Mac only). Used by `Log` class and `term` module.

### Example:

I recommend you to import static `core.AnsiColor.*` to simplify code:

```groovy
println RED + "Alert! " + YELLOW + "You are about to do something risky... " + CYAN + "Please proceed with caution" + RESET
```

You can use `AnsiColor.decolor(string)` to remove colors from a string.

## Cmd 

Execute system commands easily, asynchronously or synchronously.

There are mainly two ways to use this class, using its static methods or
creating an instance. 

Using the static methods is much simple, but it lacks of many options which
the instance supports.

### Using static methods:

Synchronously:
```groovy
String output = Cmd.exec("echo", [arg1, arg2])
println output
```

Asynchronously:
```groovy
Cmd.async("echo", [arg1, arg2], {
  String output ->
     println output
})
```

The `Fail` (on Fail) interface is optional in both methods:

```groovy
Cmd.exec("unknown command", { /* on Fail */
    String err, int code ->
        Log.w("There was an error code [%d]: %s", code, err)
})
```

Both methods accept different ways to specify your command:

```groovy
// All are the same:
Cmd.exec("echo $arg1 $arg2")
Cmd.exec("echo", [arg1, arg2])
Cmd.exec(["echo", arg1, arg2])
```

### Using instance

Synchronously: This process will run in the foreground and will wait until
it finishes to continue (default)
```groovy
// Additionally to the ways to set a command explained above,
// the constructor supports variable arguments (String...)
new Cmd("echo", arg1, arg2).getText {
  String output ->
    println output
}.exec()
```

Asynchronously: Just set `true` in `exec` to send the process to the background.
```groovy
new Cmd("echo", arg1, arg2).getText {
  String output ->
    println output
}.exec(/* background */ true)
```

You can add arguments on the go:
```groovy
new Cmd("echo")
     .arg(arg1)
     .arg([arg2, arg3])
     .exec()
```

You can also return a `List` with all the lines:
```groovy
new Cmd("echo", arg1, arg2).getLines {
  List<String> output ->
        output.each { println it }
}.exec()
```

Or process Line by Line:
```groovy
new Cmd("echo", arg1, arg2).eachLine {
  String line ->
    println line
}.exec()
```

Or get the `stderr`:

```groovy
new Cmd("echo", arg1, arg2).eachError {
  String errLine ->
    println errLine
}.exec()
```

Or execute something in case it fails:
```groovy
new Cmd("echo", arg1, arg2).onFail {
  String msg, int code ->
    Log.e("Command failed: {} with error {}", msg, code)
}.exec()
```

You can also set a timeout:

```groovy
new Cmd("echo", arg1, arg2).eachLine {
  String line ->
    println line
}.exec(/* timeout */ Millis.SECONDS_10)
```

Or cancel processes:
```groovy
Cmd cmd = new Cmd("sleep 50").exec(/*background*/ true)
sleep(Millis.SECOND)
cmd.cancel()
```

If you want to disable logs for a command (in case it might contain
sensitive or private information):

```groovy
new Cmd("echo", arg1, arg2).secret(true).exec()
```

Change the default expected `exitCode` (which is 0):

```groovy
new Cmd("echo", arg1, arg2).exitCode(100).exec()
```

Pipes and multiple commands are supported (Windows too!):

```groovy
new Cmd("tail -f /var/log/syslog | grep 'auth' | sed 's/root/****/'")
    .eachLine {  Log.i("Found line: %s", it) }
    .onFail { String msg, int code -> Log.w("Something failed: %s", msg) }
    .secret(true)
    .exitCode(143)
    .exec(/*background*/ true, /*timeout*/ Millis.HOUR)
```

## SysClock

Emulate clock and manage time in an easy way. If you use this clock,
you can execute unit tests easily by setting the time manually without
actually affecting your computer's clock. For example:

```groovy
/**
 * In a distance Class, far far away...
 * @return
 */
boolean somethingToDo() {
    // This code does something which requires time
    // Normally if you use LocalDateTime.now() it will
    // inevitable get the time from the device in which
    // is running. If you use SysClock, it will use the
    // one specified for you, or if its not specified,
    // the one from your device, making it good for testing.
    LocalDateTime timeNow = SysClock.now 
    // ...
    return ok
}

def "Changing year should not be a problem"() {
    setup :
        SysClock.setTimeZone("Asia/Tokyo") //UTC+9
        // Set a time that you want
        def time = "1999-12-31 23:59:59".toDateTime()
        SysClock.setClockAt(time)
    when :
        // Execute some code
        assert somethingToDo()
        // Wait some time
        sleep(Millis.SECONDS_5)
    then :
        // Execute the code again
        assert somethingToDo()
        // Get the new time
        LocalDateTime timeNow = SysClock.now //Will return 2000-01-01 00:04:59
        println timeNow.YMDHms
}
```

## Millis

Useful constants to use instead of numeric representation of milliseconds:
> Note : not all times are available, but most of the common times from 1 second up to 1 year.

```groovy
sleep(MINUTE)
sleep(SECONDS_10)
// If one desired time is not available, you can easily use:
int days6 = DAY * 6
```

## SysInfo

Information about the OS

### Most common fields/methods:
* getOS()      : get System OS
  * isLinux()
  * isWindows()
  * isMac()
  * isAndroid()
* newLine      : New line in the running OS

## SysMain and SysService

A more elegant way to start your applications. It is an alternative to `public static void main(String[] args)`.

### Example:

```groovy
class Main extends SysMain {
    // The following line is required:
    static { main = new Main() }

    @Override
    void onStart() {
        // Your code goes here
        String firstArg = args.first()
        Log.i("The first argument was: %s", firstArg)
    }
}
```
```groovy
class Main extends SysService {
  // The following line is required:
  static { service = new Main() }

  @Override
  void onStart() {
    // Your code goes here
    String firstArg = args.first()
    Log.i("The first argument was: %s", firstArg)
  }

  @Override
  void onSleep() {
    Log.i("Service is running...")
  }

  @Override
  void onStop() {
    Log.i("The service has stopped.")
  }
}
```


The main difference between `SysMain` and `SysService` is that the later 
will not exit after the last line of execution, it contains the method 
`onSleep()` which is executed every second and it is ready to be used 
as a service.

## Version

This class is used in two ways:

1. As instance, it can help you to manage versions in an easy way:

**NOTE**: It follows the format: `mayor`.`minor`.`build`.`revision`-`suffix` (besides `mayor` the rest are optional and
suffix can be any alphanumeric including `-`)
```groovy
Version version = new Version("1.0.3-beta4")
if(version.minor == 0) {
  version.build++
}
println version.toString() // 1.0.4-beta4
```
Other examples of versions that can be handled are:
 * 1b3
 * 1.0b3
 * 2.0-beta1
 * 3.11.293
 * 4.1.1.14
 * 5.0.1.2-SNAPSHOT
 * 20100430

2. The static implementation is mainly used to try to find the system or application version in different sources:
* JAR : Add `Implementation-Version` property into `META-INF/MANIFEST.MF`. You can add in `build.gradle`:
```groovy
jar {
  manifest {
    attributes(
            'Implementation-Version': project.version.toString()
    )
  }
}
```
* Config : Add `version=1.0` in your config.properties
* Gradle : (Only during development) It will try to get any variable with `version` from `gradle.properties`

### Example:

```groovy
  println "Using version: " + Version.get()
```