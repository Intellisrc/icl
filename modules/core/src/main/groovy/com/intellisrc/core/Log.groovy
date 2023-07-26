package com.intellisrc.core
/**
 * @since 2/11/17.
 */
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.core.AnsiColor.*

@CompileStatic
/**
 * @author Alberto Lepe <lepe@intellisrc.com>
 *
 */
class Log {
    static public final int MAX_LOG_LINE_LENGTH = Config.get("log.wrap.length", 500)
    static public List<String> ignoreFromStack = Config.get("log.stack.ignore", [
        "slf4j",
        "java.lang",
        "java.util",
        "jdk.internal",
        "com.sun.proxy",
        "groovy.lang",
        "org.codehaus.groovy",
        "org.spockframework",
        "org.junit",
        "org.gradle"
    ])
    /**
     * No instance
     */
    protected Log() {}

    /**
     * Main interface to use to send log messages to
     */
    static interface Printer {
        void print(Level level, Info stack, String msg, List<Object> args, Throwable throwable)
    }
    /**
     * Default printer: SLF4J
     */
    static protected Logger logger
    /**
     * Allow to attach other printers on runtime:
     */
    protected static synchronized final List<Printer> printers = []
    static void addPrinter(Printer toSet) {
        printers << toSet
    }

    /**
     * Initialize printers. It will look for SLF4J compatible logger. If it is not found,
     * will use a simple one.
     */
    static {
        boolean enabled = Config.get("log.enable", ! Config.get("log.disable", false))
        if(enabled) {
            logger = LoggerFactory.getLogger(Config.get("log.name", "app"))
            if (logger?.name && logger.name != "NOP") {
                printers << (Printer) {
                    Level level, Info stack, String msg, List<Object> args, Throwable throwable ->
                        // If we haven't specified any special logger, format strings here:
                        if(logger.name == "app" && args) {
                            msg = formatString(msg, args, throwable)
                            args.clear()
                        }
                        switch (level) {
                            case Level.TRACE:
                                if(args.size()) {
                                    logger.trace(msg, args.size() == 1 ? args.first() : args.toArray())
                                } else {
                                    logger.trace(msg)
                                }
                                break
                            case Level.DEBUG:
                                if(args.size()) {
                                    logger.debug(msg, args.size() == 1 ? args.first() : args.toArray())
                                } else {
                                    logger.debug(msg)
                                }
                                break
                            case Level.INFO:
                                if(args.size()) {
                                    logger.info(msg, args.size() == 1 ? args.first() : args.toArray(Object.class))
                                } else {
                                    logger.info(msg)
                                }
                                break
                            case Level.WARN:
                                if(throwable) {
                                    if(args.size()) {
                                        logger.warn(msg, args.size() == 1 ? args.first() : args.toArray(), throwable)
                                    } else {
                                        logger.warn(msg, throwable)
                                    }
                                } else {
                                    if(args.size()) {
                                        logger.warn(msg, args.size() == 1 ? args.first() : args.toArray())
                                    } else {
                                        logger.warn(msg)
                                    }
                                }
                                break
                            case Level.ERROR:
                                if(throwable) {
                                    if(args.size()) {
                                        logger.error(msg, args.size() == 1 ? args.first() : args.toArray(), throwable)
                                    } else {
                                        logger.error(msg, throwable)
                                    }
                                } else {
                                    if (args.size()) {
                                        logger.error(msg, args.size() == 1 ? args.first() : args.toArray())
                                    } else {
                                        logger.error(msg)
                                    }
                                }
                                break
                        }
                }
            } else {
                println "---------------------------------------------------------------------------------------------"
                println YELLOW + "You can use any 'slf4j' compatible logger like: " + RESET
                println YELLOW + "  * Log Module (Recommended)  : " + GREEN + Log.packageName.replace(".core", "") + ":log" + RESET
                println YELLOW + "  * SLF4J Simple              : " + CYAN + "org.slf4j:slf4j-simple " + RESET
                println YELLOW + "  * Log4J                     : " + CYAN + "org.slf4j:slf4j-log4j12" + RESET
                println YELLOW + "  * JDK Logging               : " + CYAN + "org.slf4j:slf4j-jdk14" + RESET
                println YELLOW + "  * Jakarta Commons           : " + CYAN + "org.slf4j:slf4j-jcl" + RESET
                println YELLOW + "  * LogBack                   : " + CYAN + "ch.qos.logback:logback-classic" + RESET
                println "---------------------------------------------------------------------------------------------"
                printers << (Printer) {
                    Level level, Info stack, String msg, List<Object> args, Throwable throwable ->
                        println getLogLine(level, stack, formatString(msg, args, throwable))
                        if (level == Level.ERROR) {
                            if (throwable) {
                                if (throwable.message) {
                                    println getLogLine(Level.ERROR, stack, "\t" + throwable.message)
                                }
                                StringWriter sw = new StringWriter()
                                PrintWriter pw = new PrintWriter(sw)
                                throwable.printStackTrace(pw)
                                sw.toString().eachLine {
                                    String line ->
                                        if (!line.startsWith("\t")) {
                                            println getLogLine(Level.INFO, stack, line)
                                        } else {
                                            println getLogLine(Level.TRACE, stack, line)
                                        }
                                }
                            }
                        }
                }
            }
        }
    }

    /**
     * Return a line of the Log (automatically adding color or not)
     * @return
     */
    protected static String getLogLine(Level level, Info stack, String msg) {
        String time = SysClock.dateTime.YMDHmsS
        String levelStr = level.name().charAt(0)
        if(levelStr == "T") {
            levelStr = "V" //Change it to verbose
        }
        return (time + "\t" + "[" + levelStr + "]\t" + stack.className + "\t" + stack.methodName + ":" + stack.lineNumber + "\t" + msg)
    }

    /**
     * Information about log location:
     */
    static class Info {
        String packageName
        String className
        String methodName
        String fileName
        int    lineNumber
    }

    /**
     * t is alias of v
     */
    static synchronized void t(String msg, Object... args) {
        v(msg, args)
    }
    /**
     * Log verbose messages
     * @param msg
     * @param args
     */
    static synchronized void v(String msg, Object... args) {
        log(Level.TRACE, msg, args)
    }
    /**
     * Log Debug messages
     * @param msg
     * @param args
     */
    static synchronized void d(String msg, Object... args) {
        log(Level.DEBUG, msg, args)
    }
    /**
     * Log info
     * @param msg
     * @param args
     */
    static synchronized void i(String msg, Object... args) {
        log(Level.INFO, msg, args)
    }
    /**
     * Log warnings
     * @param msg
     * @param args
     */
    static synchronized void w(String msg, Object... args) {
        log(Level.WARN, msg, args)
    }
    /**
     * Log Errors
     * @param msg
     * @param args
     */
    static synchronized void e(String msg, Object... args) {
        log(Level.ERROR, msg, args)
    }
    /**
     * Log stack trace
     * @param msg
     */
    static synchronized stackTrace(String msg = "Stacktrace is:") {
        log(Level.TRACE, String.format("%s%n%s", msg, new Exception().stackTrace.collect { it.toString() }.join(SysInfo.newLine)))
    }

    /**
     * Extracts Throwable from argument list, calls printWrap()
     * and any other listener
     * @param lvl
     * @param msg
     * @param args
     */
    protected static void log(Level lvl, String msg, Object... args) {
        Info stack = stack()
        Throwable throwable = null
        List<Object> listArgs = args.toList()
        if(! listArgs.empty && listArgs[args.length - 1] instanceof Throwable) {
            throwable = (Throwable) listArgs[args.length - 1]
            listArgs = listArgs.subList(0, args.length - 1)
        } else if(lvl == Level.ERROR) {
            throwable = new Exception("Generic Exception generated in Log")
        }
        printers.each {
            Printer printer ->
                printWrap(printer, lvl, stack, msg, throwable, listArgs)
        }
    }

    /**
     * This method will try to wrap messages in multiple lines when they are too long
     */
    protected static void printWrap(Printer printer, Level level, Info stack, String msg, Throwable throwable, List<Object> args) {
        msg.eachLine {
            String line ->
                while( line.length() > 0) {
                    int splitPos = Math.min(MAX_LOG_LINE_LENGTH, line.length())
                    for (int i = splitPos-1; line.length() > MAX_LOG_LINE_LENGTH && i >= 0; i--) {
                        if (" \t,.;:?!{}()[]/\\".indexOf(line.charAt(i) as String) != -1) {
                            splitPos = i
                            break
                        }
                    }
                    splitPos = Math.min(splitPos + 1, line.length())
                    msg = line.substring(0, splitPos)
                    line = line.substring(splitPos)
                    printer.print(level, stack, msg, args, throwable)
                }
        }
    }
    /**
     * Generate information about the log (class, line number, method, etc)
     * @param index : how many steps back in the stack trace we will skip
     * t              to identify the class
     */
    static Info stack(Collection<String> ignoreList = []) {
        ignoreList << Log.class.packageName
        ignoreList.addAll(ignoreFromStack)

        int STACK_DEPTH = 5 // How many to skip to reach this method
        StackTraceElement[] stackTrace = new Throwable().getStackTrace()
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException("Synthetic stacktrace didn't have enough elements") // are you using proguard?
        }

        StackTraceElement caller = stackTrace.find {
            StackTraceElement item ->
                boolean containsKeyword = ignoreList.any { item.className.toLowerCase().contains(it.toLowerCase()) }
                return ! containsKeyword
        }
        if(! caller) {
            caller = stackTrace[STACK_DEPTH - 1]
        }
        String className = caller.className

        //Remove closure information:
        if(className.contains('$')) {
            className -= ~/\$.*/
        }

        Info stack
        try {
            int dotIndex = className.lastIndexOf('.')
            stack = new Info(
                packageName : className.substring(0, dotIndex),
                className   : className.substring( dotIndex + 1),
                fileName    : caller.fileName,
                methodName  : caller.methodName,
                lineNumber  : caller.lineNumber > 0 ? caller.lineNumber : 0
            )
        } catch (ClassNotFoundException ignored) {
            stack = new Info(
                packageName : "not.found",
                className   : "Unknown",
                fileName    : "?",
                methodName  : "?",
                lineNumber  : 0
            )
        }
        return stack
    }
    /**
     * Convert Throwable to String
     * @param throwable
     * @return
     */
    static String getExceptionMessage(Throwable throwable) {
        String msg = throwable.localizedMessage ?: throwable.message
        if(msg) {
            if(throwable.cause) {
                msg += " (${throwable.cause})"
            }
        } else if(throwable.cause) {
            msg = throwable.cause
        } else {
            msg = throwable.toString()
        }
        return msg
    }
    /**
     * Format with %
     * @param msg
     * @param args
     * @return
     */
    static String formatString(String msg, List<Object> args, Throwable throwable = null) {
        if(throwable) {
            args << getExceptionMessage(throwable)
        }
        if(!args.empty) {
            LinkedList params = args as LinkedList
            List convParams = []
            String newMsg = msg
            msg.findAll(/(\{}|%[$0-9.(a-zA-Z+-])/).each {
                String found ->
                    if (found == "{}" || found == "%s") {
                        newMsg = newMsg.replaceFirst(/\{}/, "%s")
                        Object val = params.empty ? "" : params.pollFirst()
                        String converted
                        //noinspection GroovyFallthrough
                        switch (val) {
                            case float: case Float: case double: case Double: case BigDecimal:
                                converted = String.format("%.4f", val)
                                break
                            case LocalTime:
                                converted = (val as LocalTime).HHmmss
                                break
                            case LocalDate:
                                converted = (val as LocalDate).YMD
                                break
                            case LocalDateTime:
                                converted = (val as LocalDateTime).YMDHms
                                break
                            case InetAddress:
                                converted = (val as InetAddress).hostAddress
                                break
                            case File:
                                converted = (val as File).absolutePath
                                break
                            case byte[]:
                                converted = (val as byte[]).encodeHex()
                                break
                            case Throwable:
                                converted = getExceptionMessage(val as Throwable)
                                break
                            default:
                                converted = val.toString()
                        }
                        convParams << converted
                    } else {
                        convParams << params.pollFirst()
                    }
            }
            try {
                [   // Support for single '%' in messages, like: ("This is %d% supported", 100)
                    /%%/ : '%',
                    /%$/ : '%%',
                    /%([^0-9.a-zA-Z(%+-])/ : '%%$1'
                ].each {
                    newMsg = newMsg.replaceAll(it.key, it.value)
                }
                newMsg = String.format(newMsg, convParams.toArray())
                msg = newMsg
            } catch (Exception ignore) { // If something fails, try to execute it just like that:
                try {
                    newMsg = String.format(msg, args)
                    msg = newMsg
                } catch(Exception e) {
                    Log.e("Invalid format in message: [$msg]", e) //Set inline to prevent stack overflow
                }
            }
        }
        return msg
    }
}

