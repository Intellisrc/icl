package jp.sharelock.etc
/**
 * @since 2/11/17.
 */
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@groovy.transform.CompileStatic
/**
 * Based in: trikita.log
 * @author Alberto Lepe <lepe@sharelock.jp>
 *
 */
final class Log {
    //Execute on load
    static {
        if(Config.hasKey("log.level")) {
            def sLevel = Config.get("log.level").toUpperCase()
            level = sLevel.take(1) as Level
        }
        if(Config.hasKey("log.path")) {
            logPath = Config.get("log.path")
        }
        if(Config.hasKey("log.file")) {
            logFile = Config.get("log.file")
        }
        if (ANDROID.mLoaded) {
            usePrinter(ANDROID, true)
        } else {
            usePrinter(SYSTEM, true)
            usePrinter(LOGFILE, true)
        }
    }

    //When logFile is not empty, it will export log to that file
    static String logFile = ""
    static String logPath = ""
    static boolean color = true //When true, it will automatically set color. If false, it will disabled it
    static Level level = Version.get().contains("SNAPSHOT") ? Level.VERBOSE : Level.INFO

    static final SystemOutPrinter SYSTEM = new SystemOutPrinter()
    static final AndroidPrinter ANDROID = new AndroidPrinter()
    static final FilePrinter LOGFILE = new FilePrinter()

    private static final Set<Printer> mPrinters = []
    private static final int STACK_DEPTH = 4

    static final String ANSI_RESET   = "\u001B[0m"
    static final String ANSI_BLACK   = "\u001B[30m"
    static final String ANSI_RED     = "\u001B[31m"
    static final String ANSI_GREEN   = "\u001B[32m"
    static final String ANSI_YELLOW  = "\u001B[33m"
    static final String ANSI_BLUE    = "\u001B[34m"
    static final String ANSI_PURPLE  = "\u001B[35m"
    static final String ANSI_CYAN    = "\u001B[36m"
    static final String ANSI_WHITE   = "\u001B[37m"

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

    private static class FilePrinter implements Printer {
        @Override
        void print(Level level, Info stack, String msg) {
            if(logFile) {
                String time = new Date().toString("yyyy-MM-dd HH:mm:ss.SSS")
                if(!(logPath && new File(logPath).canWrite())) {
                    logPath = SysInfo.getWritablePath()
                } else {
                    if(!logPath.endsWith(File.separator)) {
                        logPath += File.separator
                    }
                }
                def file = new File(logPath + logFile)
                file << (time + "\t" + "[" + level + "]\t" + stack.className + "\t" + stack.methodName + ":" + stack.lineNumber + "\t" + msg + "\n")
            }
        }
    }

    private static class SystemOutPrinter implements Printer {
        private static getColor(Level level) {
            def color
            switch(level) {
                case Level.VERBOSE: color = ANSI_WHITE; break
                case Level.DEBUG: color = ANSI_BLACK; break
                case Level.INFO: color = ANSI_CYAN; break
                case Level.WARN: color = ANSI_YELLOW; break
                case Level.SECURITY: color = ANSI_PURPLE; break
                case Level.ERROR: color = ANSI_RED; break
            }
            return color
        }
        @Override
        void print(Level level, Info stack, String msg) {
            String time = new Date().toString("yyyy-MM-dd HH:mm:ss.SSS")
            if(SysInfo.isLinux() && color) {
                println(time+" [" + getColor(level) + level + ANSI_RESET + "] " +
                        ANSI_GREEN + stack.className + ANSI_RESET +
                        " (" + ANSI_BLUE + stack.methodName + ANSI_RESET +
                        ":" + ANSI_CYAN + stack.lineNumber + ANSI_RESET + ") " +
                        getColor(level) + msg + ANSI_RESET)
            } else {
                println(time+" [" + level + "] " + stack.className + " (" + stack.methodName + ":" + stack.lineNumber + ") " + msg)
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
                } catch (NoSuchMethodException|ClassNotFoundException e) {
                    // Ignore
                }
            }
            mLogClass = logClass
            mLoaded = loaded
        }

        void print(Level level, Info stack, String msg) {
            try {
                if (mLoaded) {
                    mLogMethods[level.toString()].invoke(null, stack, msg)
                }
            } catch (InvocationTargetException|IllegalAccessException e) {
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

    private static void log(Level level, String msg, Object... args) {
        if (level < Log.level) {
            return
        }
        Info stack = stack()
        Queue listArgs = args.toList() as Queue
        Throwable throwable = null
        if(!listArgs.isEmpty() && listArgs.last() instanceof Throwable) {
            throwable = (Throwable) listArgs.poll()
        } else if(level == Level.ERROR) {
            throwable = new Exception("Generic Exception generated in Log")
        }
        print(level, stack, format(msg, listArgs))
        if(throwable) {
            print(Level.VERBOSE, stack, printStack(throwable))
        }
        if(!onLogList.isEmpty()) {
            onLogList.each {
                OnLog onLog ->
                    onLog.call(level, format(msg, listArgs), stack)
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

    private static String printStack(Throwable throwable) {
        StringBuilder sb = new StringBuilder()
        if(throwable) {
            sb.append("STACK START ------------------------------------------------------------------------------\n")
            sb.append("\t")
            if(throwable.message) {
                sb.append(throwable.message ?: "")
                sb.append("\n")
                sb.append("\t")
            }
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            throwable.printStackTrace(pw)
            sb.append(sw.toString())
            sb.append("STACK END ------------------------------------------------------------------------------\n")
        }
        return sb.toString()
    }

    static final int MAX_LOG_LINE_LENGTH = 4000

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

                    mPrinters.each {
                        it.print(level, stack, msg)
                    }
                }
        }
    }

    private static Info stack() {
        Info stack = null
        def stackTrace = new Throwable().getStackTrace()
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException
                    ("Synthetic stacktrace didn't have enough elements: are you using proguard?")
        }
        def caller = stackTrace[STACK_DEPTH-1]
        String className = caller.className

        //Remove closure information:
        if(className.contains('$')) {
            className -= ~/\$.*/
        }
        try {
            Class<?> c = Class.forName(className)
            stack = new Info(
                className : className.substring(className.lastIndexOf('.') + 1),
                fileName  : caller.fileName,
                methodName: caller.methodName,
                lineNumber: caller.lineNumber > 0 ? caller.lineNumber : 0
            )
        } catch (ClassNotFoundException e) { /* Ignore */ }
        return stack
    }
}

