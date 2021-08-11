package com.intellisrc.log

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.core.SysInfo
import com.intellisrc.core.Version
import groovy.transform.CompileStatic
import org.slf4j.event.Level
import org.slf4j.helpers.FormattingTuple
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentLinkedQueue

import static com.intellisrc.core.AnsiColor.*

/**
 * @since 2021/08/02.
 */
@CompileStatic
class CommonLogger extends MarkerIgnoringBase {
    private static final long serialVersionUID = 1L

    protected LocalDateTime startTime = SysClock.now
    protected synchronized ConcurrentLinkedQueue<LoggableOutputLevels> printers = new ConcurrentLinkedQueue<>()

    //-------------- to be assigned later ---------
    protected boolean initialized = false
    protected List<String> domains = []

    //-------------- instance fields -------------

    // external software might be invoking this method directly. Do not rename
    // or change its semantics.
    static void init() {}

    void initialize() {
        if (initialized) {
            return
        }
        initialized = true

        // Set domains to highlight
        setDomains()

        BaseLogger basePrinter = new BaseLogger()
        // Global settings:
        if (basePrinter.enabled) {
            // File Log
            FileLogger fileLogger = new FileLogger()
            if (fileLogger.enabled) {
                fileLogger.initialize(basePrinter)
                printers << fileLogger
            }

            //Print Log
            PrintLogger printLogger = new PrintLogger()
            if (printLogger.enabled) {
                printLogger.initialize(basePrinter)
                printers << printLogger
                if (printLogger.split) {
                    PrintStdErrLogger printStdErrLogger = new PrintStdErrLogger()
                    printStdErrLogger.initialize(printLogger)
                    printers << printStdErrLogger
                }
            }
        }
    }

    /**
     * Read domains from config or guess it
     */
    protected void setDomains() {
        if (Config.exists("log.domain")) {
            domains << Config.get("log.domain")
        }
        if (Config.exists("log.domains")) {
            domains = Config.getList("log.domains")
        }
        if(domains.empty) {
            if(Version.mainClass !== Version.class) {
                domains << Version.mainClass.packageName
            }
        }
    }

    protected String getClassName() {
        return this.class.simpleName
    }

    /**
     * Package access allows only {@link CommonLoggerFactory} to instantiate
     * CommonLogger instances.
     */
    CommonLogger() {
        this.name = className
    }

    /**
     * Print stack trace
     * @param t
     * @param targetStream
     */
    protected void writeThrowable(LoggableOutputLevels printer, Throwable throwable, PrintStream targetStream) {
        if (throwable != null) {
            String nl = SysInfo.newLine
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            throwable.printStackTrace(pw)
            sw.toString().eachLine {
                String line ->
                    if(printer.useColor && throwable.message && line.contains(throwable.message)) {
                        line = line.replace(throwable.message, RED + throwable.message + RESET)
                    }
                    if(!line.startsWith("\t")) {
                        targetStream.print(line + nl)
                    } else if (!domains.empty) {
                        // Search if any of the domains are contained in line
                        if (domains.any { line.contains(it) }) {
                            if(printer.useColor) {
                                targetStream.print(YELLOW + line + RESET + nl)
                            } else {
                                targetStream.print(line.replace("\t","-->\t") + line + nl)
                            }
                        } else {
                            targetStream.print(line + nl)
                        }
                    } else {
                        targetStream.print(line + nl)
                    }
            }
        }
    }

    /**
     * Is the given log level currently enabled?
     *
     * @param logLevel is this level enabled?
     * @return whether the logger is enabled for the given level
     */
    protected boolean isLevelEnabled(Level logLevel) {
        // log level are numerically ordered so can use simple numeric
        // comparison
        return printers.any { it.levels.contains(logLevel) }
    }

    /** Are {@code trace} messages currently enabled? */
    boolean isTraceEnabled() {
        return isLevelEnabled(Level.TRACE)
    }

    @Override
    void trace(String msg) {
        formatAndLog(Level.TRACE, msg)
    }

    @Override
    void trace(String format, Object arg) {
        formatAndLog(Level.TRACE, format, arg)
    }

    @Override
    void trace(String format, Object arg1, Object arg2) {
        formatAndLog(Level.TRACE, format, arg1, arg2)
    }

    @Override
    void trace(String format, Object... arguments) {
        formatAndLog(Level.TRACE, format, arguments)
    }

    @Override
    void trace(String msg, Throwable t) {
        formatAndLog(Level.TRACE, msg, t)
    }
    /** Are {@code debug} messages currently enabled? */
    boolean isDebugEnabled() {
        return isLevelEnabled(Level.DEBUG)
    }

    @Override
    void debug(String msg) {
        formatAndLog(Level.DEBUG, msg)
    }

    @Override
    void debug(String format, Object arg) {
        formatAndLog(Level.DEBUG, format, arg)
    }

    @Override
    void debug(String format, Object arg1, Object arg2) {
        formatAndLog(Level.DEBUG, format, arg1, arg2)
    }

    @Override
    void debug(String format, Object... arguments) {
        formatAndLog(Level.DEBUG, format, arguments)
    }

    @Override
    void debug(String msg, Throwable t) {
        formatAndLog(Level.DEBUG, msg, t)
    }
    /** Are {@code info} messages currently enabled? */
    boolean isInfoEnabled() {
        return isLevelEnabled(Level.INFO)
    }

    @Override
    void info(String msg) {
        formatAndLog(Level.INFO, msg)
    }

    @Override
    void info(String format, Object arg) {
        formatAndLog(Level.INFO, format, arg)
    }

    @Override
    void info(String format, Object arg1, Object arg2) {
        formatAndLog(Level.INFO, format, arg1, arg2)
    }

    @Override
    void info(String format, Object... arguments) {
        formatAndLog(Level.INFO, format, arguments)
    }

    @Override
    void info(String msg, Throwable t) {
        formatAndLog(Level.INFO, msg, t)
    }

    /** Are {@code warn} messages currently enabled? */
    boolean isWarnEnabled() {
        return isLevelEnabled(Level.WARN)
    }

    @Override
    void warn(String msg) {
        formatAndLog(Level.WARN, msg)
    }

    @Override
    void warn(String format, Object arg) {
        formatAndLog(Level.WARN, format, arg)
    }

    @Override
    void warn(String format, Object... arguments) {
        formatAndLog(Level.WARN, format, arguments)
    }

    @Override
    void warn(String format, Object arg1, Object arg2) {
        formatAndLog(Level.WARN, format, arg1, arg2)
    }

    @Override
    void warn(String msg, Throwable t) {
        formatAndLog(Level.WARN, msg, t)

    }
    /** Are {@code error} messages currently enabled? */
    boolean isErrorEnabled() {
        return isLevelEnabled(Level.ERROR)
    }

    @Override
    void error(String msg) {
        formatAndLog(Level.ERROR, msg)
    }

    @Override
    void error(String format, Object arg) {
        formatAndLog(Level.ERROR, format, arg)
    }

    @Override
    void error(String format, Object arg1, Object arg2) {
        formatAndLog(Level.ERROR, format, arg1, arg2)
    }

    @Override
    void error(String format, Object... arguments) {
        formatAndLog(Level.ERROR, format, arguments)
    }

    @Override
    void error(String msg, Throwable t) {
        formatAndLog(Level.ERROR, msg, t)
    }

    /**
     * For formatted messages, first substitute arguments and then log.
     *
     * @param level
     * @param format
     * @param arguments
     *            a list of 3 ore more arguments
     */
    protected void formatAndLog(Level level, String format, Object... arguments) {
        LinkedList args = List.of(arguments) as LinkedList
        if(args.first instanceof Object[]) {
            args = args.first as LinkedList
        }
        Throwable t = null
        if(args.last instanceof Throwable) {
            t = args.pollLast() as Throwable
        }
        String formatted = Log.formatString(format, args)
        log(level, formatted, t)
    }

    /**
     * Log a message
     *
     * @param levelInt
     * @param message
     * @param t
     */
    protected void log(Level level, String message, Throwable t) {
        if(!initialized) { initialize() }
        if (!isLevelEnabled(level)) {
            return
        }
        Log.Info info = Log.stack([this.class.packageName, this.class.simpleName])
        printers.each {
            LoggableOutputLevels printer ->
                if(printer.enabled && printer.hasLevel(level)) {
                    boolean ignore = !printer.ignoreList.empty &&
                            level >= printer.ignoreLevel &&
                            printer.ignoreList.any {
                                info.packageName.contains(it) || info.className == it
                            }
                    if (!ignore) {
                        StringBuilder buf = new StringBuilder(32)

                        // Append date-time if so configured
                        if (printer.showDateTime) {
                            if (printer.dateFormatter) {
                                buf.append(SysClock.now.format(printer.dateFormatter)).append(' ')
                            }
                        } else {
                            buf.append(ChronoUnit.MILLIS.between(startTime, SysClock.now)).append(' ')
                        }

                        // Append current thread name if so configured
                        if (printer.showThreadName) {
                            buf.append('[')
                            if (printer.useColor) {
                                buf.append(YELLOW)
                            }
                            String tName = Thread.currentThread().name
                            if (printer.showThreadShort) {
                                int total = printer.showThreadHead + printer.showThreadTail
                                tName = tName.contains("-") ? tName.substring(0, [printer.showThreadHead, tName.length()].min())
                                        .toUpperCase().padRight(printer.showThreadHead) +
                                        tName.tokenize("-").last().padLeft(printer.showThreadTail, "_")
                                        : tName.toUpperCase().substring(0, [total, tName.length()].min()).padRight(total, "_")
                            }
                            buf.append(tName)
                            if (printer.useColor) {
                                buf.append(RESET)
                            }
                            buf.append("] ")
                        }

                        if (printer.levelInBrackets)
                            buf.append('[')

                        // Append a readable representation of the log level
                        String levelStr = printer.levelAbbreviated ? level.name().charAt(0) : level.name().padRight(5, " ")
                        if (levelStr == "T") {
                            levelStr = "V" //Change it to verbose
                        }
                        if (printer.useColor) {
                            buf.append(getLevelColor(level, printer))
                        }
                        buf.append(levelStr)
                        if (printer.useColor) {
                            buf.append(RESET)
                        }
                        if (printer.levelInBrackets) {
                            buf.append(']')
                        }
                        buf.append(' ')

                        // Append the name of the log instance if so configured
                        if (printer.showLogName) {
                            buf.append(name).append(" - ")
                        }

                        if (printer.useColor) {
                            buf.append(GREEN)
                        }
                        if (printer.showPackage) {
                            buf.append(info.packageName).append(".")
                        }
                        if (printer.showClassName) {
                            buf.append(info.className).append(' ')
                        }
                        if (printer.useColor) {
                            buf.append(RESET)
                        }

                        if (printer.showMethod) {
                            buf.append("(")
                            if (printer.useColor) {
                                buf.append(BLUE)
                            }
                            buf.append(info.methodName)
                            if (printer.useColor) {
                                buf.append(RESET)
                            }
                            if (printer.showLineNumber) {
                                buf.append(":")
                                if (printer.useColor) {
                                    buf.append(CYAN)
                                }
                                buf.append(info.lineNumber)
                                if (printer.useColor) {
                                    buf.append(RESET)
                                }
                            }
                            buf.append(") ")
                        }

                        /* When Markers are used...
                    if (markers != null) {
                        buf.append(SP)
                        for (Marker marker : markers) {
                            buf.append(marker.getName()).append(SP)
                        }
                    }
                    String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments)
                    */

                        // Append the message
                        if (printer.useColor) {
                            buf.append(getLevelColor(level, printer))
                        }
                        buf.append(message)
                        if (printer.useColor) {
                            buf.append(RESET)
                        }

                        PrintStream targetStream = printer.output.targetPrintStream
                        String output = buf.toString()
                        targetStream.println(output)
                        if(printer.output.onPrint) {
                            printer.output.onPrint.call(output)
                        }
                        if(printer.showStackTrace) {
                            writeThrowable(printer, t, targetStream)
                        }
                        targetStream.flush()
                    }
                }
        }
    }

    /**
     * Add custom printer (LoggableOutputLevels)
     * @param printer
     */
    void addPrinter(LoggableOutputLevels printer) {
        printers << printer
    }

    /**
     * Return color depending on level
     */
    protected static getLevelColor(Level lvl, Loggable printer) {
        def color
        switch(lvl) {
            case Level.TRACE: color = printer.colorInvert ? WHITE : BLACK; break
            case Level.DEBUG: color = printer.colorInvert ? BLACK : WHITE; break
            case Level.INFO: color = CYAN; break
            case Level.WARN: color = YELLOW; break
            case Level.ERROR: color = RED; break
        }
        return color
    }
}
