# CORE MODULE

Basic functionality that is usually needed in any project. For example, configuration, 
logging, executing commands, controlling services and displaying colors in console.

The following is an overview of this module:

## Config

The easiest way to read and write settings from a file (read on initialization).
By default, loads the configuration from `config.properties` file. It can handle 
multiple types besides primitive types, like: Lists, Maps, URL/URI, Files, InetAddress,
LocalDate, LocalTime, LocalDateTime, byte array and custom objects through `toString()` method.

This class is not thread safe, if you need to read and write your system
configuration from multiple threads, use `AutoConfig` in the `etc` module.

### Example:

```groovy
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

## Log 

A simple colorful logger which is very flexible an easy to use. Automatically compress 
and rotate logs, filter verbosity, and more. By default, logs are stored in `log` directory.

### Settings:

The most common settings are: (Please see Log class for full documentation)

```properties
# config.properties:
# Choose from which level you want to log:
# verbose, debug, info, warning, error, special  (default: info)
log.level=verbose
# Always print to screen (default: true)
log.print=true
# Enable log color output (default: true)
log.color=true
# When true, white/black will be inverted (default: false)
log.color.invert=true
# You can specify your domain to highlight it:
log.domain=com.example
```

### Example:

```groovy
// Verbose level, for maximum output details (gray color):
Log.v("This is very verbose output: %.2f", 99.9999d)
// Debug level, for details (white color):
Log.d("When you want to debug something: %d", 1000)
// Logging some information (blue color):
Log.i("This is some information about %s", "Log")
// Displaying a warning (yellow color):
Log.w("Beware! this is a warning")

try {
    int err = 100 / 0
} catch(Exception e) {
    // Exception is passed to Log, so it can print all the detail (red color):
    Log.e("You can't do that", e)
}
// This is a special Log which will be displayed in purple:
Log.s("Hey!")
```

## AnsiColor

Add colors to your terminal (Linux and Mac only). Used by `Log` class and `term` module.

### Example:

I recommend you to import static `core.AnsiColor.*` to simplify code:

```groovy
println RED + "Alert! " + YELLOW + "You are about to do something risky... " + CYAN + "Please proceed with caution" + RESET
```

## Cmd 

Execute system commands easily. Asynchronously or synchronously.

### Examples:

```groovy
Cmd.exec("echo",[arg1, arg2], {
    String output ->
        output.eachLine {
            println "> " + it
        }
}, {
    String err, int code ->
        Log.w("There was an error code [%d]: %s", code, err)
})
```

You can set special options like:
```groovy
Cmd.options(timeout: 2000, secret: true).exec(/*...*/)
```

Or execute instructions asynchronously in the same way as `exec`:
```groovy
Cmd.async(/*...*/)
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
        sleep(5000)
    then :
        // Execute the code again
        assert somethingToDo()
        // Get the new time
        LocalDateTime timeNow = SysClock.now //Will return 2000-01-01 00:04:59
        println timeNow.YMDHms
}
```

## SysInfo

Get files, paths or identify your OS.

### Most common fields/methods:
* getOS()      : get System OS
  * isLinux()
  * isWindows()
  * isMac()
  * isAndroid()
* getFile()    : get a file
* getHomeDir() : Home directory
* getUserDir() : Directory in which the system is running
* getTempDir() : Get the temporal directory
* newLine      : New line in the running OS

The most used methods is `getFile()`, which
is similar to `new File()`, but with the difference
that accepts multiple strings and other ways to
create a File.

### Example:

```groovy
File file1 = SysInfo.getFile("~/home.cfg")
File file2 = SysInfo.getFile("relative/file.txt")
File file3 = SysInfo.getFile(SysInfo.tempDir, "dir1", "dir2", "file.txt")
```

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

This class is mainly used to try to find the package version in different sources:
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