package jp.sharelock.etc
/**
 * @since 2/11/17.
 */
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
@groovy.transform.CompileStatic
/**
 * Originally imported from: trikita.log
 * @author Alberto Lepe <lepe@sharelock.jp>
 *
 * Note: this code still needs a lot of simplification aka Groovy.
 */
final class Log {
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

    static final int V = 0
    static final int D = 1
    static final int I = 2
    static final int W = 3
    static final int E = 4

    private Log() {}

    interface Printer {
        void print(int level, String tag, String msg)
    }

    private static class FilePrinter implements Printer {
        private static final List<String> LEVELS = ['V', 'D', 'I', 'W', 'E']
        @Override
        void print(int level, String tag, String msg) {
            if(logFile) {
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                if(!(logPath && new File(logPath).canWrite())) {
                    logPath = SysInfo.getWritablePath()
                } else {
                    if(!logPath.endsWith(File.separator)) {
                        logPath += File.separator
                    }
                }
                def file = new File(logPath + logFile)
                file << (time + "\t" + "[" + LEVELS[level] + "]\t" + tag + "\t" + msg + "\n")
            }
        }
    }

    private static class SystemOutPrinter implements Printer {
        private static final List<String> LEVELS = ['V', 'D', 'I', 'W', 'E']
        private getColor(int level) {
            def color = ANSI_WHITE
            switch(level) {
                case V: color = ANSI_WHITE; break
                case D: color = ANSI_WHITE; break
                case I: color = ANSI_CYAN; break
                case W: color = ANSI_YELLOW; break
                case E: color = ANSI_RED; break
            }
            return color
        }
        @Override
        void print(int level, String tag, String msg) {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
            if(SysInfo.isAnyLinux()) {
                System.out.println(time+" [" + getColor(level) + LEVELS[level] + ANSI_RESET + "] " + ANSI_GREEN + tag + " " + getColor(level) + msg + ANSI_RESET)
            } else {
                System.out.println(time+" [" + LEVELS[level] + "] " + tag + " " + msg)
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

        void print(int level, String tag, String msg) {
            try {
                if (mLoaded) {
                    mLogMethods[level].invoke(null, tag, msg)
                }
            } catch (InvocationTargetException|IllegalAccessException e) {
                // Ignore
            }
        }
    }

    static final SystemOutPrinter SYSTEM = new SystemOutPrinter()
    static final AndroidPrinter ANDROID = new AndroidPrinter()
    static final FilePrinter LOGFILE = new FilePrinter()

    private static final Map<String, String> mTags = new HashMap<>()

    private static List<String> mUseTags = ['tag', 'TAG']
    private static boolean mUseFormat = false
    private static int mMinLevel = V

    private static Set<Printer> mPrinters = new HashSet<>()

    static {
        if (ANDROID.mLoaded) {
            usePrinter(ANDROID, true)
        } else {
            usePrinter(SYSTEM, true)
            usePrinter(LOGFILE, true)
        }
    }

    static synchronized Log useTags(ArrayList<String> tags) {
        mUseTags = tags
        return null
    }

    static synchronized Log level(int minLevel) {
        mMinLevel = minLevel
        return null
    }

    static synchronized Log useFormat(boolean yes) {
        mUseFormat = yes
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

    static synchronized Log v(Object msg, Object... args) {
        log(V, mUseFormat, msg, args)
        return null
    }
    static synchronized Log d(Object msg, Object... args) {
        log(D, mUseFormat, msg, args)
        return null
    }
    static synchronized Log i(Object msg, Object... args) {
        log(I, mUseFormat, msg, args)
        return null
    }
    static synchronized Log w(Object msg, Object... args) {
        log(W, mUseFormat, msg, args)
        return null
    }
    static synchronized Log e(Object msg, Object... args) {
        log(E, mUseFormat, msg, args)
        return null
    }

    private static void log(int level, boolean fmt, Object msg, Object... args) {
        if (level < mMinLevel) {
            return
        }
        String tag = tag()
        if (!mUseTags.isEmpty() && tag == msg) {
            if (args.length > 1) {
                print(level, tag, format(fmt, args[0], Arrays.copyOfRange(args, 1, args.length)))
            } else {
                print(level, tag, format(fmt, (args.length > 0 ? args[0] : "")))
            }
        } else {
            print(level, tag, format(fmt, msg, args))
        }
    }

    private static String format(boolean fmt, Object msg, Object... args) {
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
        if (fmt && msg instanceof String) {
            String head = (String) msg
            if (head.indexOf('%') != -1) {
                return String.format(head, args)
            }
        }
        StringBuilder sb = new StringBuilder()
        sb.append(msg == null ? "null" : msg.toString())
        for (Object arg : args) {
            sb.append("\t")
            sb.append(arg == null ? "null" : arg.toString())
        }
        if (t != null) {
            sb.append("\n")
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            t.printStackTrace(pw)
            sb.append(sw.toString())
        }
        return sb.toString()
    }

    static final int MAX_LOG_LINE_LENGTH = 4000

    private static void print(int level, String tag, String msg) {
        for (String line : msg.split("\\n")) {
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

                if(msg.contains("\t")) {
                    def parts = msg.split("\t").toList()
                    def new_tag = parts.first()
                    parts.remove(0)
                    msg = tag + " : " + parts.join(" ")
                    tag = new_tag
                }

                for (Printer p : mPrinters) {
                    p.print(level, tag, msg)
                }
                line
            }
        }
    }

    private static final int STACK_DEPTH = 4
    private static String tag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace()
        if (stackTrace.length < STACK_DEPTH) {
            throw new IllegalStateException
                    ("Synthetic stacktrace didn't have enough elements: are you using proguard?")
        }
        String className = stackTrace[STACK_DEPTH-1].getClassName()
        String tag = mTags.get(className)
        if (tag != null) {
            return tag
        }

        try {
            Class<?> c = Class.forName(className)
            for (String f : mUseTags) {
                try {
                    Field field = c.getDeclaredField(f)
                    if (field != null) {
                        field.setAccessible(true)
                        Object value = field.get(null)
                        if (value instanceof String) {
                            mTags.put(className, (String) value)
                            return (String) value
                        }
                    }
                } catch (NoSuchFieldException|IllegalAccessException|
                IllegalStateException|NullPointerException e) {
                    //Ignore
                }
            }
        } catch (ClassNotFoundException e) { /* Ignore */ }

        // Check class field useTag, if exists - return it, otherwise - return the generated tag
        className =~ /\\$\\d+$"/ //<--- LEPE: Not sure if it works the same. before it was like /.../.replaceAll("")
        return className.substring(className.lastIndexOf('.') + 1)
    }
}

