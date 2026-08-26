---
name: core
description: Use when a Groovy/Java/Kotlin project depends on the intelliSource ICL common library (com.intellisrc) and needs configuration files (Config, config.properties, environment variables), logging (Log, SLF4J), app or daemon entry points (SysMain, SysService with start/stop/restart/status), executing system commands (Cmd), time constants (Millis, Secs), a testable clock (SysClock), OS detection (SysInfo), terminal colors (AnsiColor), or version handling (Version). Triggers on com.intellisrc.core imports or ICL usage. Base module required by all other ICL modules.
---

# ICL core Module

Foundation of the intelliSource Common Library (ICL) 2.10.x. Every other ICL module includes it. Groovy-first, usable from Java/Kotlin (examples in Groovy). Requires Java 21+; the client picks the Groovy version.

```groovy
dependencies {
    implementation 'com.intellisrc:core:2.10.5'
    implementation 'org.codehaus.groovy:groovy-all:5.0.8' // required, version up to you
}
```

Package: `com.intellisrc.core`.

## Config — typed access to config.properties, env vars, system properties

Static accessors; typed getters take a default value:

```groovy
String name  = Config.get("system.name", "default")
int threads  = Config.getInt("system.threads", 4)
boolean dbg  = Config.getBool("debug")              // default false
double fps   = Config.getDbl("fps", 24.5d)
// also: getLong, getFloat, getBigInt, getBigDec

// Optional-wrapped getters: getFile, getURL, getURI, getTime, getDate,
// getDateTime, getInet4, getInet6, getBytes, getEnum(key, Class or defaultEnum)

// Collections (stored as YAML flow style: flags=[a,b]  options={k:v})
List flags = Config.getList("system.flags")
Map opts   = Config.getMap("system.options")
Set sets   = Config.getSet("system.sets")

// Setters persist to config.properties (overloads: String, Number, boolean,
// byte[], InetAddress, File, URI, URL, LocalTime/Date/DateTime, Collection, Map, Enum)
Config.set("system.name", "prod")

Config.exists("key") ; Config.delete("key") ; Config.getKeys() ; Config.reload() ; Config.clear()
```

Pick a source explicitly when needed:

```groovy
Config.env.get("java.home")     // environment vars (UNDER_SCORE auto-maps to dot.case)
Config.system.get("os.version") // System properties
Config.any.get("database.url")  // env -> config.properties -> system (recommended)
```

## Log — static SLF4J logging

```groovy
Log.v("trace")                          // v=TRACE, d=DEBUG, i=INFO, w=WARN, e=ERROR
Log.i("Loaded %d items in %s", n, file) // printf style
Log.d("value: {}", obj)                 // SLF4J {} style also works
Log.e("Failed: %s", e.message, e)       // pass exception last to log stack trace
Log.w(e)                                // exception alone is fine
Log.stackTrace()                        // dump current stack
Log.addPrinter(printer)                 // custom output (see log module skill)
```

Auto-formats LocalDateTime/LocalDate/LocalTime, InetAddress, File (absolute path), byte[] (hex). Config keys: `log.level` (verbose|trace|debug|info|warn|error), `log.name`, `log.enable=false` to silence.

## SysMain / SysService — entry points

`SysMain`: run-and-exit apps. Assign the instance in a static block. Custom `onXxx()` methods are invoked by the first CLI argument (`java -jar app.jar custom` calls `onCustom()`).

```groovy
class Main extends SysMain {
    static { main = new Main() }
    @Override void onStart() { Log.i("args: %s", args) }  // args: Queue<String>
    @Override void onStop()  { }
}
```

`SysService`: long-running daemon, only one per project, same static-block pattern with `service`:

```groovy
class App extends SysService {
    static { service = new App() }
    @Override void onInit()   { }              // pre-start
    @Override void onStart()  { }              // required
    @Override void onSleep()  { }              // called every second while running
    @Override void onStop()   { }
    @Override void onStatus(boolean running) { }
}
// CLI: java -jar app.jar start|stop|restart|status
// Lock file: service.lockFile = "custom.lock" (cwd if writable, else tmp dir)
```

## Millis / Secs — readable time constants

`Millis.SECOND, SECOND_2, SECOND_5, MINUTE, MIN_5, HALF_HOUR, HOUR, HALF_DAY, DAY, WEEK, MONTH` (30 days), `MONTH_31D, YEAR` (365d), `YEAR_LEAP`, plus `HALF_SECOND, MILLIS_100` and similar. `Secs` mirrors the names in seconds (`Secs.MINUTE == 60`). Prefer these over raw numbers.

## SysClock — testable clock

Use instead of `LocalDateTime.now()` so unit tests can shift time:

```groovy
LocalDateTime now = SysClock.now                       // also getDate(), getTime()
SysClock.setClockAt("2000-01-01 00:00:00".toDateTime()) // clock keeps ticking from there
SysClock.setTimeZone("Asia/Tokyo")
SysClock.seconds(from) ; SysClock.minutes(from)         // elapsed until now
SysClock.getTimeSince(from)                             // human readable
SysClock.millisToString(millis)
```

## Cmd — execute system commands

```groovy
String out = Cmd.exec("ls", ["-la", "/tmp"])           // static; returns stdout
Cmd.async("ffmpeg", ["-i", input, output], onDone, onFail)
boolean ok = Cmd.succeed("which", ["git"])

// Fluent instance form
new Cmd("curl", url).secret(true)                      // hide from logs
    .getText { String response -> Log.i("%s", response) }
    .onFail { String msg, int code -> Log.e("fail %d", code) }
    .exec(10_000)                                       // timeout ms; 0 = none
// also: .arg(...), .eachLine{...}, .eachError{...}, .getLines{ List<String> l -> }, .cancel()
```

Pipes, `&&` and `;` are auto-wrapped with `cmd /C` on Windows.

## Other classes

- `SysInfo`: `SysInfo.OS` (enum `LINUX, WINDOWS, ANDROID, IOS, UNKNOWN`), `isWindows()`, `isLinux()` (excludes Android), `isAnyLinux()`, `isMac()`, `OSVersion`, `OSArch`, `newLine`.
- `AnsiColor`: ANSI constants `RED, GREEN, L_BLUE, BACK_BLACK, BOLD, RESET`, ...; `AnsiColor.decolor(str)`, `hasColor(str)`. Linux/mac terminals only.
- `Version`: `Version.get()` auto-detects from JAR manifest, then config.properties, then gradle.properties; parses `2.3.4-SNAPSHOT`; fields `mayor, minor, build, revision, suffix`.
- `Triplet<X,Y,Z>`: like Pair, with `first, middle, last`.
- `ToMap` (`core.ext`): implement `toMap()` on a class for automatic Map/JSON conversion.

## Gotchas

- `Config` is not thread-safe for concurrent writes; for runtime-mutable config use `AutoConfig` from the `etc` module.
- Property values with wrapping quotes are auto-stripped with a warning.
- `SysMain`/`SysService` need the static-block singleton assignment (or set `main.class` in config.properties).
- `Log.e("msg")` without an exception still emits a synthetic stack entry.
- For file logging, rotation, colors and custom printers, load the ICL `log` module skill.
