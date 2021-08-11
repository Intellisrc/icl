package com.intellisrc.core
/**
 * @since 2/11/17.
 */
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

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
        void print(Level level, Info stack, String msg, Throwable throwable, Object[] args)
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
                    Level level, Info stack, String msg, Throwable throwable, Object[] args ->
                        switch (level) {
                            case Level.TRACE:
                                logger.trace(msg, args)
                                break
                            case Level.DEBUG:
                                logger.debug(msg, args)
                                break
                            case Level.INFO:
                                logger.info(msg, args)
                                break
                            case Level.WARN:
                                logger.warn(formatString(msg, args), throwable)
                                break
                            case Level.ERROR:
                                logger.error(formatString(msg, args), throwable)
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
                    Level level, Info stack, String msg, Throwable throwable, Object[] args ->
                        println getLogLine(level, stack, formatString(msg, args))
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
    static synchronized void v(String msg, Object... args) {
        log(Level.TRACE, msg, args)
    }
    static synchronized void d(String msg, Object... args) {
        log(Level.DEBUG, msg, args)
    }
    static synchronized void i(String msg, Object... args) {
        log(Level.INFO, msg, args)
    }
    static synchronized void w(String msg, Object... args) {
        log(Level.WARN, msg, args)
    }
    static synchronized void e(String msg, Object... args) {
        log(Level.ERROR, msg, args)
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
        LinkedList listArgs = args.toList() as LinkedList
        Throwable throwable = null
        if(!listArgs.isEmpty() && listArgs.last() instanceof Throwable) {
            throwable = (Throwable) listArgs.pollLast()
        } else if(lvl == Level.ERROR) {
            throwable = new Exception("Generic Exception generated in Log")
        }
        printers.each {
            Printer printer ->
                printWrap(printer, lvl, stack, msg, throwable, listArgs.toArray())
        }
    }

    /**
     * This method will try to wrap messages in multiple lines when they are too long
     */
    protected static void printWrap(Printer printer, Level level, Info stack, String msg, Throwable throwable, Object... args) {
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
                    printer.print(level, stack, msg, throwable, args)
                }
        }
    }
    /**
     * Generate information about the log (class, line number, method, etc)
     * @param index : how many steps back in the stack trace we will skip
     * t              to identify the class
     */
    static Info stack(List<String> ignoreList = []) {
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
     * Format with %
     * @param msg
     * @param args
     * @return
     */
    static String formatString(String msg, Object[] args) {
        String formatted = msg
        if (msg =~ /%[$0-9.,a-zA-Z()+-]+/ && args.size()) {
            try {
                formatted = String.format(formatted, args)
            } catch(Exception e) {
                Log.e("Invalid format in message: [$msg]", e) //Set inline to prevent stack overflow
            }
        }
        return formatted
    }
}

