package com.intellisrc.core
/**
 * @since 2/11/17.
 */
import groovy.transform.CompileStatic
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.Files
import java.time.LocalDateTime

@CompileStatic
/**
 * Based in: trikita.log
 * @author Alberto Lepe <lepe@intellisrc.com>
 *
 */
final class Log {
    //When logFile is not empty, it will export log to that file
    static String logFileName = ""
    static File directory

    static String mainClass = ""
    static List<String> domains = [] //Highlight one or more domains in logs (it will try to get it automatically)
    static LocalDateTime logDate = LocalDateTime.now()
    static boolean color = true //When true, it will automatically set color. If false, it will disabled it
    static boolean colorInvert = false //When true BLACK/WHITE will be inverted <VERBOSE vs DEBUG> (depends on terminal)
    static boolean colorAlways = false //When true it will output log always in color
    static boolean printAlways = false //When true it will always output to screen (it won't print if Log is disabled)
    static boolean isSnapShot
    static synchronized boolean initialized = false
    static boolean enabled = true
    static Level level = Level.INFO
    static int logDays = 0
    static final int MAX_LOG_LINE_LENGTH = 4000
    static private final long ONE_DAY = 24*60*60*1000L

    static final SystemOutPrinter SYSTEM = new SystemOutPrinter()
    static final AndroidPrinter ANDROID = new AndroidPrinter()
    static final FilePrinter LOGFILE = new FilePrinter()

    private static final Set<Printer> mPrinters = []
    private static final int STACK_DEPTH = 4

    static final enum Level {
        VERBOSE, DEBUG, INFO, WARN, SECURITY, ERROR
        String toString() {
            return super.toString().substring(0,1)
        }
    }
    private Log() {}
    private static synchronized final List<OnLog> onLogList = []
    static void setOnLog(OnLog toSet) {
        onLogList << toSet
    }

    /**
     * Used to hook something on any event
     */
    interface OnLog {
        void call(Level level, String message, Info stack)
    }

    interface Printer {
        void print(Level level, Info stack, String msg)
    }

    //Execute on load
    static void init() {
        isSnapShot = Version.get().contains("SNAPSHOT")
        if(isSnapShot) {
            level = Level.VERBOSE
        } else {
            def sLevel = Config.get("log.level", "verbose").toUpperCase()
            level = sLevel as Level
        }

        logFileName = Config.get("log.file", "system.log")

        if (Config.hasKey("log.dir")) {
            directory = Config.getFile("log.dir")
        } else if (Config.hasKey("log.path")) { //Support for old config
            directory = Config.getFile("log.path")
        }
        if(!directory) {
            directory = new File(SysInfo.userDir)
        }

        logDays = Config.getInt("log.days", 30)
        enabled = !Config.getBool("log.disable")
        printAlways = Config.getBool("log.print")

        if (Config.hasKey("log.domain")) {
            domains << Config.get("log.domain")
        }
        if (Config.hasKey("log.domains")) {
            domains = Config.getList("log.domains")
        }
        if (Config.hasKey("log.color")) {
            color = Config.getBool("log.color")
            if(color) {
                colorAlways = true //We set it true if we have explicitly in the configuration
            }
        }
        if (Config.hasKey("log.color.invert")) {
            colorInvert = Config.getBool("log.color.invert")
        }

        if(domains.empty) {
            mainClass = Version.mainClass
            if (mainClass) {
                def parts = mainClass.tokenize('.')
                parts.pop()
                if(parts) {
                    domains << parts.join('.')
                }
            }
        }

        setPrinter()
        cleanLogs()
        linkLast()
        initialized = true
    }

    private static class FilePrinter implements Printer {
        @Override
        void print(Level lvl, Info stack, String msg) {
            if(logFileName) {
                LocalDateTime newDate = LocalDateTime.now()
                // Change file and compress if date changed
                if(newDate.toLocalDate().YMD != logDate.toLocalDate().YMD) {
                    compressLog()
                    logDate = newDate
                    linkLast()
                    cleanLogs()
                }
                logFile << getLogLine(lvl, stack, msg, true)
            }
        }
    }

    /**
     * Return a line of the Log (automatically adding color or not)
     * @return
     */
    private static String getLogLine(Level lvl, Info stack, String msg, boolean toFile) {
        String time = LocalDateTime.now().YMDHmsS
        String line
        if(colorAlways || SysInfo.isLinux() && color &&! toFile) {
            line = time +" [" + getLevelColor(lvl) + lvl + AnsiColor.RESET + "] " +
                    AnsiColor.GREEN + stack.className + AnsiColor.RESET +
                    " (" + AnsiColor.BLUE + stack.methodName + AnsiColor.RESET +
                    ":" + AnsiColor.CYAN + stack.lineNumber + AnsiColor.RESET + ") " +
                    getLevelColor(lvl) + msg + AnsiColor.RESET + "\n"
        } else {
            line = (time + "\t" + "[" + lvl + "]\t" + stack.className + "\t" + stack.methodName + ":" + stack.lineNumber + "\t" + msg + "\n")
        }
        return line
    }
    /**
     * Decide which printer to use
     */
    private static void setPrinter() {
        if(enabled) {
            if (ANDROID.mLoaded) {
                usePrinter(ANDROID, true)
            } else {
                if (logFileName) {
                    usePrinter(LOGFILE, true)
                    if(isSnapShot || printAlways) {
                        usePrinter(SYSTEM, true)
                    }
                } else {
                    usePrinter(SYSTEM, true)
                }
            }
        }
    }

    /**
     * Get current Log File
     * @return
     */
    static File getLogFile() {
        return new File(directory, logDate.toLocalDate().YMD + "-" + logFileName)
    }

    /**
     * Compress Log file if Zip is present
     */
    static boolean compressLog(File logToCompress = logFile) {
        boolean done = false
        try {
            Class[] parameters = [ File.class ]
            Class zip = Class.forName(Log.class.package.name.replace('core','etc') + ".Zip")
            Method method = zip.getMethod("gzip", parameters)
            Object[] callParams = [ logToCompress ]
            method.invoke(null, callParams)
            done = true
        } catch (Exception ignored) {
            //Ignore... Zip class doesn't exists, so we don't compress them
            //We can't use print here or it will loop forever
        }
        return done
    }

    /**
     * Delete old logs and compress as needed
     * @param logsDir : Logs directory
     * @param logName : Name of log, e.g : system.log
     * @param days : number of logs to keep (usually 1 per day)
     */
    static void cleanLogs(final File logsDir = directory, String logName = logFileName, int days = logDays) {
        List<File> logs = logsDir.listFiles({
            File file ->
                file.name.contains(logName) &&!Files.isSymbolicLink(file.toPath())
        } as FileFilter).toList()
        if(logs && logs.size() > days) {
            logs.sort().toList().reverse().subList(0, days).each {
                it.delete()
            }
        }
        // Compress old not compressed logs in directory:
        logsDir.eachFileMatchAsync("*-${logName}") {
            File log ->
                if(System.currentTimeMillis() - log.lastModified() > ONE_DAY) {
                    compressLog(log)
                }
        }
    }

    /**
     * Link last log
     */
    static void linkLast() {
        File link = new File(directory, "last-" + logFileName)
        link.delete()
        logFile.linkTo(link)
    }

    /**
     * Return color depending on level
     */
    private static getLevelColor(Level lvl) {
        def color
        switch(lvl) {
            case Level.VERBOSE: color = colorInvert ? AnsiColor.WHITE : AnsiColor.BLACK; break
            case Level.DEBUG: color = colorInvert ? AnsiColor.BLACK : AnsiColor.WHITE; break
            case Level.INFO: color = AnsiColor.CYAN; break
            case Level.WARN: color = AnsiColor.YELLOW; break
            case Level.SECURITY: color = AnsiColor.PURPLE; break
            case Level.ERROR: color = AnsiColor.RED; break
        }
        return color
    }

    private static class SystemOutPrinter implements Printer {
        @Override
        void print(Level lvl, Info stack, String msg) {
            print(getLogLine(lvl, stack, msg, false))
        }
    }

    private static class AndroidPrinter implements Printer {

        private static final List<String> METHOD_NAMES = ["v", "d", "i", "w", "s", "e"]
        private final Class<?> mLogClass
        private final Method[] mLogMethods
        private final boolean mLoaded

        AndroidPrinter() {
            Class logClass = null
            boolean loaded = false
            mLogMethods = new Method[METHOD_NAMES.size()]
            if(SysInfo.isAndroid()) {
                try {
                    logClass = Class.forName("android.util.Log")
                    for (int i = 0; i < METHOD_NAMES.size(); i++) {
                        mLogMethods[i] = logClass.getMethod(METHOD_NAMES[i].toString(), String, String)
                    }
                    loaded = true
                } catch (NoSuchMethodException|ClassNotFoundException ignored) {
                    // Ignore
                }
            }
            mLogClass = logClass
            mLoaded = loaded
        }

        void print(Level level, Info stack, String msg) {
            try {
                if (mLoaded) {
                    (mLogMethods[level.toString()] as Method).invoke(null, stack, msg)
                }
            } catch (InvocationTargetException|IllegalAccessException ignored) {
                // Ignore
            }
        }
    }

    static class Info {
        String className
        String methodName
        String fileName
        int    lineNumber
    }

    static synchronized void usePrinter(Printer p, boolean on) {
        if (on) {
            mPrinters.add(p)
        } else {
            mPrinters.remove(p)
        }
    }

    static synchronized void v(String msg, Object... args) {
        log(Level.VERBOSE, msg, args)
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
    static synchronized void s(String msg, Object... args) {
        log(Level.SECURITY, msg, args)
    }
    static synchronized void e(String msg, Object... args) {
        log(Level.ERROR, msg, args)
    }

    private static isPrintable(Level lvl) {
        return enabled && lvl >= level
    }

    private static void log(Level lvl, String msg, Object... args) {
        if(!initialized) {
            init()
        }
        if (!isPrintable(lvl)) {
            return
        }
        Info stack = stack()
        Queue listArgs = args.toList() as Queue
        Throwable throwable = null
        if(!listArgs.isEmpty() && listArgs.last() instanceof Throwable) {
            throwable = (Throwable) listArgs.poll()
        } else if(lvl == Level.ERROR) {
            throwable = new Exception("Generic Exception generated in Log")
        }
        print(lvl, stack, format(msg, listArgs))
        if(throwable) {
            printStack(stack, throwable)
        }
        if(!onLogList.isEmpty()) {
            onLogList.each {
                OnLog onLog ->
                    onLog.call(lvl, format(msg, listArgs), stack)
            }
        }
    }

    private static String format(String msg, Queue args) {
        if (msg.indexOf('%') != -1 &&! args.isEmpty()) {
            return String.format(msg, args.toArray())
        }
        StringBuilder sb = new StringBuilder()
        sb.append(msg.toString())
        args.each {
            sb.append("\t")
            sb.append(it == null ? "<null>" : it.toString())
        }
        return sb.toString()
    }

    private static void printStack(Info stack, Throwable throwable) {
        if(throwable) {
            boolean verboseOk = isPrintable(Level.VERBOSE)
            print(Level.VERBOSE, stack, "STACK START ------------------------------------------------------------------------------")
            if(throwable.message) {
                print(Level.ERROR, stack, "\t" + throwable.message)
            }
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            throwable.printStackTrace(pw)
            sw.toString().eachLine {
                String line ->
                    if(!line.startsWith("\t")) {
                        print(Level.INFO, stack, line)
                    } else if (!domains.empty) {
                        // Search if any of the domains are contained in line
                        if (domains.any { line.contains(it) }) {
                            print(Level.DEBUG, stack, line)
                        } else if(verboseOk) {
                            print(Level.VERBOSE, stack, line)
                        }
                    } else {
                        print(Level.VERBOSE, stack, line)
                    }
            }
            print(Level.VERBOSE, stack, "STACK END ------------------------------------------------------------------------------")
        }
    }

    private static void print(Level level, Info stack, String msg) {
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

                    if(!mPrinters) {
                        println "No printers registered... check config file"
                        println msg
                    } else {
                        mPrinters.each {
                            it.print(level, stack, msg)
                        }
                    }
                }
        }
    }

    private static Info stack() {
        Info stack
        def stackTrace = new Throwable().getStackTrace()
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException("Synthetic stacktrace didn't have enough elements") // are you using proguard?
        }
        def caller = stackTrace[STACK_DEPTH-1]
        String className = caller.className

        //Remove closure information:
        if(className.contains('$')) {
            className -= ~/\$.*/
        }
        try {
            stack = new Info(
                className : className.substring(className.lastIndexOf('.') + 1),
                fileName  : caller.fileName,
                methodName: caller.methodName,
                lineNumber: caller.lineNumber > 0 ? caller.lineNumber : 0
            )
        } catch (ClassNotFoundException ignored) {
            stack = new Info(
                className : "Unknown",
                fileName  : "?",
                methodName: "?",
                lineNumber: 0
            )
        }
        return stack
    }
}

