package com.intellisrc.core
/**
 * @since 2/11/17.
 */
import groovy.transform.CompileStatic

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@CompileStatic
/**
 * Based in: trikita.log
 * @author Alberto Lepe <lepe@intellisrc.com>
 *
 */
final class Log {
    //When logFile is not empty, it will export log to that file
    static public String logFileName = "system.log"
    static public File directory = SysInfo.getFile("log")

    static public String mainClass = ""
    static public List<String> domains = [] //Highlight one or more domains in logs (it will try to get it automatically)
    static public LocalDateTime logDate = SysClock.dateTime
    static public boolean color = true //When true, it will automatically set color. If false, it will disabled it
    static public boolean colorInvert = false //When true BLACK/WHITE will be inverted <VERBOSE vs DEBUG> (depends on terminal)
    static public boolean colorAlways = false //When true it will output log always in color
    static public boolean printAlways = true  //When true it will always output to screen (it won't print if Log is disabled)
    static public boolean isSnapShot
    static public synchronized boolean initialized = false
    static public boolean enabled = true
    static public Level level = Level.INFO //Global level. It will be used to print (unless printLevel is set) and to store
    static public Level printLevel = level //Level to print on screen
    static public Level fileLevel = level //Level to export to file
    static public int logDays = 7 // Days to keep as backup (doesn't include today)
    static public final int MAX_LOG_LINE_LENGTH = 4000
    static public final int maxTaskExecTimeMs = 60000 // 1 minute

    static public final SystemOutPrinter SYSTEM = new SystemOutPrinter()
    static public final AndroidPrinter ANDROID = new AndroidPrinter()
    static public final FilePrinter LOGFILE = new FilePrinter()

    private static final List<Printer> mPrinters = []
    private static final int STACK_DEPTH = 4

    static final enum Level {
        VERBOSE, DEBUG, INFO, WARN, SPECIAL, ERROR
        String toChar() {
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
    static interface OnLog {
        void call(Level level, String message, Info stack)
    }

    static interface Printer {
        void print(Level level, Info stack, String msg)
    }

    //Execute on load
    static void init() {
        isSnapShot = Version.get().contains("SNAPSHOT")
        if(Config.hasKey("log.level")) {
            level = Config.get("log.level").toUpperCase() as Level
        } else if(isSnapShot) {
            level = Config.get("log.level.snapshot", "verbose").toUpperCase() as Level
        }

        if(Config.hasKey("log.file")) {
            logFileName = Config.get("log.file")
        }
        if(Config.hasKey("log.days")) {
            logDays = Config.getInt("log.days")
        }
        if(Config.hasKey("log.disable")) {
            enabled = !Config.getBool("log.disable")
        }
        if(Config.hasKey("log.print")) {
            printAlways = Config.getBool("log.print")
        }
        if(Config.hasKey("log.print.level")) {
            printLevel = Config.get("log.print.level").toUpperCase() as Level
        }
        if(Config.hasKey("log.file.level")) {
            fileLevel = Config.get("log.file.level").toUpperCase() as Level
        }
        //Fix level in case is set incorrectly:
        if(printLevel < level || fileLevel < level) {
            level = [printLevel, fileLevel].min()
        }

        if (Config.hasKey("log.dir")) {
            directory = Config.getFile("log.dir")
        } else if (Config.hasKey("log.path")) { //Support for old config
            directory = Config.getFile("log.path")
        }
        if(directory) {
            if(!directory.exists()) {
                if(!directory.mkdirs()) {
                    logFileName = ""
                    printAlways = true
                    println AnsiColor.RED + "ERROR: Unable to create directory: " + directory.absolutePath + AnsiColor.RESET
                }
            }
            if(!directory.canWrite()) {
                println AnsiColor.RED + "ERROR: Log directory is not writable: " + directory.absolutePath + AnsiColor.RESET
            }
        } else {
            logFileName = ""
            printAlways = true
        }

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
            if(lvl >= fileLevel) {
                File logToFile = logFile
                if (logToFile) {
                    LocalDateTime newDate = SysClock.dateTime
                    boolean dateChanged = false
                    // Change file and compress if date changed
                    if (newDate.toLocalDate().YMD != logDate.toLocalDate().YMD) {
                        compressLog(logToFile) //compress previous date log
                        logDate = newDate
                        cleanLogs()
                        dateChanged = true
                    }
                    logFile << getLogLine(lvl, stack, msg, true) //log to last date
                    linkLast(dateChanged)
                }
            }
        }
    }

    /**
     * Return a line of the Log (automatically adding color or not)
     * @return
     */
    private static String getLogLine(Level lvl, Info stack, String msg, boolean toFile) {
        String time = SysClock.dateTime.YMDHmsS
        String line
        if(colorAlways || SysInfo.isLinux() && color &&! toFile) {
            line = time +" [" + getLevelColor(lvl) + lvl.toChar() + AnsiColor.RESET + "] " +
                    AnsiColor.GREEN + stack.className + AnsiColor.RESET +
                    " (" + AnsiColor.BLUE + stack.methodName + AnsiColor.RESET +
                    ":" + AnsiColor.CYAN + stack.lineNumber + AnsiColor.RESET + ") " +
                    getLevelColor(lvl) + msg + AnsiColor.RESET + "\n"
        } else {
            line = (time + "\t" + "[" + lvl.toChar() + "]\t" + stack.className + "\t" + stack.methodName + ":" + stack.lineNumber + "\t" + msg + "\n")
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
        return directory && logFileName ? new File(directory, logDate.toLocalDate().YMD + "-" + logFileName) : null
    }

    /**
     * Compress Log file if Zip is present
     */
    static boolean compressLog(final File logToCompress) {
        boolean done = false
        if(logToCompress?.exists()) {
            try {
                Class[] parameters = [File.class]
                Class zip = Class.forName(Log.class.package.name.replace('core', 'etc') + ".Zip")
                Method method = zip.getMethod("gzip", parameters)
                Object[] callParams = [logToCompress]
                method.invoke(null, callParams)
                done = true
            } catch (Exception ignored) {
                //Ignore... Zip class doesn't exists, so we don't compress them
                //We can't use print here or it will loop forever
            }
        }
        return done
    }

    /**
     * Delete old logs and compress as needed
     * @param logsDir : Logs directory
     * @param logName : Name of log, e.g : system.log
     * @param days : number of logs to keep (usually 1 per day)
     */
    static void cleanLogs(final File logsDir = directory, int days = logDays) {
        if(logsDir?.exists()) {
            Runnable runnable = {
                // Compress old not compressed logs in directory:
                logsDir.eachFileMatchAsync("*.log") {
                    File log ->
                        if (!Files.isSymbolicLink(log.toPath())) { //Skip links
                            LocalDateTime lastMod = LocalDateTime.fromMillis(log.lastModified())
                            if (ChronoUnit.DAYS.between(lastMod, SysClock.dateTime) > days) {
                                log.delete()
                            } else if (ChronoUnit.DAYS.between(lastMod, SysClock.dateTime) > 0) {
                                compressLog(log)
                            }
                        }
                }
            }
            try {
                Class tasks = Class.forName(Log.class.package.name.replace('core', 'thread') + ".Tasks")
                Class priority = Class.forName(Log.class.package.name.replace('core', 'thread') + '.Task$Priority')
                Class[] parameters = [Runnable.class, String.class, priority, Integer.TYPE]
                Method method = tasks.getMethod("add", parameters)
                Object low = Enum.valueOf(priority, "LOW")
                Object[] callParams = [runnable, "Log.clean", low, maxTaskExecTimeMs]
                method.invoke(null, callParams)
            } catch (Exception ignored) {
                //Ignore... Tasks class doesn't exists, so we use a normal Thread:
                //We can't use print here or it will loop forever
                new Thread(runnable).start()
            }
        }
    }

    /**
     * Link last log
     */
    static void linkLast(boolean update = true) {
        if(logFile?.exists()) {
            File link = new File(directory, "last-" + logFileName)
            if (link.exists()) {
                if(update) {
                    link.delete()
                    logFile.linkTo(link)
                }
            } else {
                try {
                    link.toPath().toRealPath().toFile().absolutePath
                } catch(Exception ignored) {
                    link.delete() // link.exists will fail if link is broken. In that case is better to be sure to remove it
                }
                logFile.linkTo(link)
            }
        }
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
            case Level.SPECIAL: color = AnsiColor.PURPLE; break
            case Level.ERROR: color = AnsiColor.RED; break
        }
        return color
    }

    private static class SystemOutPrinter implements Printer {
        @Override
        void print(Level lvl, Info stack, String msg) {
            if(lvl >= printLevel) {
                print(getLogLine(lvl, stack, msg, false))
            }
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
                    (mLogMethods[level.toChar()] as Method).invoke(null, stack, msg)
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
        log(Level.SPECIAL, msg, args)
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
        LinkedList listArgs = args.toList() as LinkedList
        Throwable throwable = null
        if(!listArgs.isEmpty() && listArgs.last() instanceof Throwable) {
            throwable = (Throwable) listArgs.pollLast()
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

