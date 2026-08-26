---
name: log
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) and needs to customize logging output - adding file loggers with daily rotation and compression, colored console output, stderr splitting, custom printers, log level configuration via config.properties, or capturing logs to strings in tests. The log module is the SLF4J provider behind core.Log. Triggers on com.intellisrc.log imports, FileLogger, PrintLogger, CommonLogger, or log.* config keys in an ICL project.
---

# ICL log Module

The SLF4J provider behind `core.Log`. On the classpath it auto-registers via `META-INF/services` (`CommonLoggerService`) and replaces any other SLF4J binding. Included by: core (runtime), and transitively by most ICL modules.

```groovy
implementation 'com.intellisrc:log:2.10.5'   // + core + groovy
```

Package: `com.intellisrc.log`. Applications log through `com.intellisrc.core.Log` (`Log.v/d/i/w/e`); touch this module only to customize printers, output files, rotation or formats.

## Configuration (config.properties)

```properties
log.name=myapp                  # logger name shown
log.level=info                  # verbose|trace|debug|info|warn|error
log.level.snapshot=verbose      # level used for -SNAPSHOT versions (default verbose)
log.enable=true                 # false silences everything
log.color=true                  # ANSI colors (default true)
log.wrap.length=500             # wrap long lines
log.show.time.format=yyyy-MM-dd HH:mm:ss.SSS
log.show.thread.name=true       # also showThreadShort, showThreadHead/Tail
log.show.log.name=true
log.show.package=false          # also showClassName, showMethod, showLineNumber
log.print=false                 # disable console printer
log.print.split=true            # WARN/ERROR go to stderr (PrintStdErrLogger)
log.print.cache=true            # buffered stdout
log.ignore=[com.example.quiet]  # filter INFO-and-below for these packages
```

Printer-specific overrides use a prefix: `log.print.color=false`, `log.file.show.package=true`.

## Printers

| Class | Purpose | Key properties |
|---|---|---|
| `PrintLogger` | stdout | `useColor`, `colorInvert`, `cache` |
| `PrintStdErrLogger` | stderr for WARN/ERROR (auto-created by split) | same as PrintLogger |
| `FileLogger` | rotating log files | see below |
| `StringLogger` | capture to string/callback (tests, UI) | `getContent()`, `clear()` |
| `CommonLogger` | main SLF4J logger, manages printers | `addPrinter()`, `getFileLogger()`, `getPrintLogger()` |

`BaseLogger` is the shared config base (all fields above); every printer extends it.

### FileLogger

Rotates at midnight: current `logFileName`, dated `YYYY-MM-DD-name.log` archives, `last-name.log` symlink, retention cleanup task.

```groovy
import com.intellisrc.log.*
import org.slf4j.LoggerFactory

FileLogger fileLogger = new FileLogger(
    logFileName: "app.log",        // default: system.log
    logDir: new File("logs"),      // default: log/
    logDays: 30,                   // retention days (default 7)
    compress: true,                // gzip old logs (needs etc module)
    rotateOtherLogs: true          // also rotate other *.log in dir
)
(LoggerFactory.getLogger("default") as CommonLogger).addPrinter(fileLogger)
```

### StringLogger — capture logs (tests, custom sinks)

```groovy
StringLogger capturer = new StringLogger({ detail ->
    // detail: time, level, message, location, exception, formatted
    println "[capture] ${detail.level}: ${detail.message}"
})
capturer.level = Level.INFO
(LoggerFactory.getLogger("default") as CommonLogger).addPrinter(capturer)
// also capturer.content / capturer.bytes / capturer.clear()
```

## Customizing the console printer

```groovy
CommonLogger logger = LoggerFactory.getLogger("default") as CommonLogger
PrintLogger print = logger.printLogger   // existing stdout printer
print.useColor = true
print.colorInvert = true                 // swap WHITE/BLACK for readability on light bg
print.showThreadShort = true
```

## Gotchas

- Only ONE SLF4J provider per classpath: exclude `logback-classic`, `log4j-slf4j-impl`, `slf4j-jdk14`, `slf4j-simple` etc., or this module silently loses the binding race.
- Loggers initialize lazily on first use; set `log.*` keys before that (config.properties or env vars do this naturally).
- Rotation is date-based (midnight), not size-based.
- `compress: true` requires the `etc` module on the classpath.
- SNAPSHOT builds default to verbose logging (`log.level.snapshot`) — set it explicitly in dev builds if too noisy.
