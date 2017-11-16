package jp.sharelock.etc
/**
 * @since 2/11/17.
 */
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@groovy.transform.CompileStatic
/**
 * Originally imported from: trikita.log
 * @author Alberto Lepe <lepe@sharelock.jp>
 *
 */
final class Log {
    //Execute on load
    static {
        if(Config.hasKey("log.level")) {
            def sLevel = Config.get("log.level").toUpperCase()
            mMinLevel = sLevel.take(1) as LEVEL
        }
        if(Config.hasKey("log.path")) {
            logPath = Config.get("log.path")
        }
        if(Config.hasKey("log.file")) {
            logFile = Config.get("log.file")
        }
    }
    //When logFile is not empty, it will export log to that file
    static String logFile = ""
    static String logPath = ""

    static final String ANSI_RESET   = "\u001B[0m"
    static final String ANSI_BLACK   = "\u001B[30m"
    static final String ANSI_RED     = "\u001B[31m"
    static final String ANSI_GREEN   = "\u001B[32m"
    static final String ANSI_YELLOW  = "\u001B[33m"
    static final String ANSI_BLUE    = "\u001B[34m"
    static final String ANSI_PURPLE  = "\u001B[35m"
    static final String ANSI_CYAN    = "\u001B[36m"
    static final String ANSI_WHITE   = "\u001B[37m"

    private static final enum LEVEL { V, D, I, W, E }
    private Log() {}

    interface Printer {
        void print(LEVEL level, Stack stack, String msg)
    }

    private static class FilePrinter implements Printer {
        @Override
        void print(LEVEL level, Stack stack, String msg) {
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
                file << (time + "\t" + "[" + level + "]\t" + stack + "\t" + msg + "\n")
            }
        }
    }

    private static class SystemOutPrinter implements Printer {
        private static getColor(LEVEL level) {
            def color
            switch(level) {
                case LEVEL.V: color = ANSI_WHITE; break
                case LEVEL.D: color = ANSI_WHITE; break
                case LEVEL.I: color = ANSI_CYAN; break
                case LEVEL.W: color = ANSI_YELLOW; break
                case LEVEL.E: color = ANSI_RED; break
            }
            return color
        }
        @Override
        void print(LEVEL level, Stack stack, String msg) {
            String time = new Date().toString("yyyy-MM-dd HH:mm:ss.SSS")
            if(SysInfo.isLinux()) {
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

        private static final List<String> METHOD_NAMES = ["v", "d", "i", "w", "e"]

        private final Class<?> mLogClass
        private final Method[] mLogMethods
        private final boolean mLoaded

        AndroidPrinter() {
            Class logClass
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

        void print(LEVEL level, Stack stack, String msg) {
            try {
                if (mLoaded) {
                    mLogMethods[level.toString()].invoke(null, stack, msg)
                }
            } catch (InvocationTargetException|IllegalAccessException e) {
                // Ignore
            }
        }
    }

    private static class Stack {
        String className
        String methodName
        String fileName
        int    lineNumber
    }

    static final SystemOutPrinter SYSTEM = new SystemOutPrinter()
    static final AndroidPrinter ANDROID = new AndroidPrinter()
    static final FilePrinter LOGFILE = new FilePrinter()

    private static LEVEL mMinLevel = LEVEL.V
    private static Set<Printer> mPrinters = []

    static {
        if (ANDROID.mLoaded) {
            usePrinter(ANDROID, true)
        } else {
            usePrinter(SYSTEM, true)
            usePrinter(LOGFILE, true)
        }
    }

    static synchronized Log level(int minLevel) {
        mMinLevel = LEVEL.values()[minLevel]
        return null
    }

    static synchronized Log usePrinter(Printer p, boolean on) {
        if (on) {
            mPrinters.add(p)
        } else {
            mPrinters.remove(p)
        }
        return null
    }

    static synchronized Log v(String msg, Object... args) {
        log(LEVEL.V, msg, args)
        return null
    }
    static synchronized Log d(String msg, Object... args) {
        log(LEVEL.D, msg, args)
        return null
    }
    static synchronized Log i(String msg, Object... args) {
        log(LEVEL.I, msg, args)
        return null
    }
    static synchronized Log w(String msg, Object... args) {
        log(LEVEL.W, msg, args)
        return null
    }
    static synchronized Log e(String msg, Object... args) {
        log(LEVEL.E, msg, args)
        return null
    }

    private static void log(LEVEL level, String msg, Object... args) {
        if (level < mMinLevel) {
            return
        }
        Stack stack = stack()
        print(level, stack, format(msg, args))
    }

    private static String format(String msg, Object... args) {
        Throwable t = null
        if (args == null) {
            // Null array is not supposed to be passed into this method, so it must
            // be a single null argument
            args = [null]
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            t = (Throwable) args[args.length - 1]
            args = Arrays.copyOfRange(args, 0, args.length - 1)
        }
        if (msg.indexOf('%') != -1 && args.length > 0) {
            return String.format(msg, args)
        }
        StringBuilder sb = new StringBuilder()
        sb.append(msg.toString())
        for (Object arg : args) {
            sb.append("\t")
            sb.append(arg == null ? "<null>" : arg.toString())
        }
        if(t) {
            sb.append("\t")
            sb.append(t.message ?: "")
            sb.append("\n")
            sb.append("STACK START ------------------------------------------------------------------------------\n")
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            t.printStackTrace(pw)
            sb.append(sw.toString())
            sb.append("STACK END ------------------------------------------------------------------------------\n")
        }
        return sb.toString()
    }

    static final int MAX_LOG_LINE_LENGTH = 4000

    private static void print(LEVEL level, Stack stack, String msg) {
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

    private static final int STACK_DEPTH = 4
    private static Stack stack() {
        Stack stack = null
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
            stack = new Stack(
                className : className.substring(className.lastIndexOf('.') + 1),
                fileName  : caller.fileName,
                methodName: caller.methodName,
                lineNumber: caller.lineNumber > 0 ? caller.lineNumber : 0
            )
        } catch (ClassNotFoundException e) { /* Ignore */ }
        return stack
    }
}

