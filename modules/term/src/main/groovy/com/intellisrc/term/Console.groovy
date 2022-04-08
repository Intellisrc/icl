package com.intellisrc.term

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.impl.LineReaderImpl
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A wrapper around jLine
 *
 * Config settings:
 * -------------------------
 *  console.prompt  : What to place as default prompt (default: '> ' )
 *  console.mask    : What to use to cover passwords (one char, default: '*')
 *  console.timeout : If set, Trigger onTimeOut() after N seconds (default: 0)
 *  console.default : If true, will add the default commands (exit, clear, etc.) (default: true)
 *
 * @since 18/06/27.
 */
@CompileStatic
class Console {
    static public String prompt = Config.get("console.prompt") ?: "> "
    static public Character mask = (Config.get("console.mask") ?: "*")[0] as Character
    static public LineReaderImpl reader = new LineReaderImpl(TerminalBuilder.terminal())
    static public int timeout = Config.getInt("console.timeout") ?: 0
    static public final boolean addDefault = Config.getBool("console.default") ?: true
    static public final String ANSI_BACKLINE = '\033[1A'
    static public final LinkedList<String> commandBuffer = new LinkedList<>()

    static private ScheduledFuture timer
    static private boolean timerRunning = false
    static private final List<Consolable> consoles = []

    /**
     * Execute some code on the background while waiting for input
     */
    trait BackgroundTask {
        int getDelay() { return 0 }  // 0 = start immediately
        int getPeriod() { return 1 }  // 1 = 1 second period
        abstract boolean call()      // if false is returned, the interval is stopped
    }
    /**
     * Adds one Consolable to the list to be processed
     * @param consolable
     */
    static void add(Consolable consolable) {
        consoles << consolable
    }

    /**
     * Launches the console and loop indefinitely
     * @param line
     */
    static void start(final LinkedList<String> args = new LinkedList<>()) {
        reader.history = new DefaultHistory()
        // Add the default console if nothing has been specified
        if(addDefault) {
            if(!consoles) {
                Log.w("Consoles have not been added. Using only default.")
                Log.w("    Example to add:  Console << new MyConsole()")
                Log.w("    where MyConsole() implements Consolable")
            }
            consoles << new ConsoleDefault()
        }
        String line
        // Auto init
        consoles.each {
            Consolable console ->
                console.onInit(args.collect() as LinkedList<String>)
        }
        // Get the auto complete list
        updateAutoComplete()
        while(true) {
            line = commandBuffer.empty ? read() : commandBuffer.poll()
            if(timerRunning) {
                timer.cancel(true)
                timerRunning = false
            }
            final LinkedList<String> list = parse(line)
            if(list) {
                // It will loop until `onCommand` returns a 'false'
                consoles.any {
                    Consolable console ->
                        return !console.onCommand(list.collect() as LinkedList<String>)
                }
            }
            reader.terminal.flush()
            if(timeout) {
                timer = Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
                    // It will loop until `onCommand` returns a 'false'
                    consoles.any {
                        Consolable console ->
                            return !console.onTimeOut()
                    }
                    timerRunning = false
                    timer.cancel(true)
                }, timeout, 1, TimeUnit.SECONDS)
                timerRunning = true
            }
        }
    }

    /**
     * Println in console
     * The reason of wrapping "println" is so we can use a
     * console remotely in the future (like web service, etc)
     * @param output
     */
    static void out(String output, Object... params) {
        //For now, just print on screen
        //We are not using reader.terminal.writer().println as is not always displayed
        //We are using Log.formatString to support all what Log supports
        println Log.formatString(output, params.toList())
    }

    /**
     * Add a word to the autocomplete list
     * @param word
     */
    static void updateAutoComplete() {
        final List<String> completeList = []
        final List<Completer> completerList = []
        consoles.each {
            Consolable console ->
                completeList.addAll(console.autoCompleteList)
                Completer completer = console.completer
                if(completer) {
                    completerList << completer
                }
        }
        completerList << new StringsCompleter(completeList as String[])
        reader.completer = new AggregateCompleter(completerList)
    }

    /**
     * Exit Console and terminate application
     * @param code
     */
    static void exit(int code = 0) {
        consoles.each {
            Consolable console ->
                console.onExit()
        }
        System.exit(code)
    }

    /**
     * Read a line from user input. It also handles Ctrl+C / Ctrl+D as "exit"
     * @param tempPrompt
     * @return
     */
    static String read(final String tempPrompt = Console.prompt, final BackgroundTask backProcess = null) {
        String line
        ScheduledFuture process
        if(backProcess) {
            process = readBackProcess(backProcess)
        }
        try {
            line = reader.readLine(tempPrompt).trim()
        } catch (Exception uie) {
            line = "exit"
            if(process) {
                process.cancel(true)
            }
        }
        return line
    }
    /**
     * Read a line from user input with a interval of a process on the back
     * @param interval
     * @return
     */
    static String read(final BackgroundTask interval) {
        return read(prompt, interval)
    }

    /**
     * Cancel a read instruction
     */
    static void cancel() {
        reader.terminal.raise(Terminal.Signal.INT)
    }

    /**
     * Read a password and return it as char[]
     * (jLine internally uses String, so its not 100% secure)
     * @param tempPrompt
     * @param mask
     * @return
     */
    static char[] readPassword(final String tempPrompt = Console.prompt, final Character mask = Console.mask, final BackgroundTask backProcess = null) {
        char[] pass = null
        ScheduledFuture process
        if(backProcess) {
            process = readBackProcess(backProcess)
        }
        try {
            if(mask) {
                pass = reader.readLine(tempPrompt, mask).trim().toCharArray()
                reader.buffer.clear() //Try to clear buffer
                System.gc() // try to collect garbage here in case any String is still holding the value
            } else {
                // If we don't specify mask, we can use System.console() which is safer as it won't create Strings
                print tempPrompt
                pass = System.console().readPassword()
            }
        } catch (Exception uie) {
            if(process) {
                process.cancel(true)
            }
        }
        return pass
    }
    /**
     * Read a password with a backgroud process
     * @param interval
     * @return
     */
    static char[] readPassword(final BackgroundTask backProcess) {
        return readPassword(prompt, mask, backProcess)
    }

    /**
     * Executes a back process while reading
     * @param backProcess
     */
    static ScheduledFuture readBackProcess(final BackgroundTask backProcess) {
        ScheduledFuture intervalTimer
        intervalTimer = Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            if(!backProcess.call()) {
                intervalTimer.cancel(true)
            }
        }, backProcess.delay, backProcess.period, TimeUnit.SECONDS)
        return intervalTimer
    }

    /**
     * Parse a input line
     * @param line
     * @return
     */
    static LinkedList<String> parse(final String line) {
        LinkedList<String> parts = new LinkedList<>()
        parts.addAll(reader.parser.parse(line, 0).words())
        return parts
    }

    /**
     * Shortcut to use String instead of ParsedLine
     * @param command
     */
    static void execCommand(final String command) {
        execCommandList(parse(command))
    }

    /**
     * Execute a command.
     * Override this method to add more commands
     * @param command
     */
    static void execCommandList(final LinkedList<String> command) {
        if(!command.empty) {
            consoles.any {
                Consolable console ->
                    return !console.onCommand(command) //continue if "true" is returned
            }
        }
    }

    /**
     * It Redraws the prompt while reading.
     * Its similar to clearScreen but it doesn't clear it.
     * @param message
     */
    static void resetRead(String message = "") {
        try {
            reader.setPrompt(prompt)
            reader.callWidget(LineReader.KILL_WHOLE_LINE)
            if(message) {
                reader.terminal.writer().println(message)
            }
            reader.callWidget(LineReader.REDISPLAY)
            reader.terminal.flush()
        } catch (Exception e) {}
    }

    /**
     * Will repaint previous line (after hitting Enter key)
     * @param withStr
     */
    static void resetPreviousPrompt(String withStr = "") {
        int prevLen = reader.parsedLine.line().length()
        int diff = prevLen > withStr.length() ? prevLen - withStr.length() + 1 : 1
        out(ANSI_BACKLINE + prompt + withStr + (" " * diff))
    }

    /**
     * Clears the screen and reset prompt
     * @param message
     */
    static void clearScreen(String message = "") {
        reader.setPrompt(prompt)
        try {
            reader.callWidget(LineReader.KILL_WHOLE_LINE)   //Remove any text in progress
            reader.callWidget(LineReader.CLEAR_SCREEN)      //Clear screen buffer
            if(message) {
                reader.terminal.writer().println(message)
            }
            reader.callWidget(LineReader.REDRAW_LINE)       //Seems it has no effect
            reader.callWidget(LineReader.REDISPLAY)         //show "old" prompt
            reader.terminal.writer().flush()
        } catch (Exception e) { // In case we can't call Widgets, clear it normally
            reader.terminal.puts(InfoCmp.Capability.clear_screen)
            if(message) {
                println message
            }
            reader.terminal.flush()
        }
    }
}