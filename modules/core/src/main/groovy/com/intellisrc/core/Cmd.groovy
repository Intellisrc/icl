package com.intellisrc.core

import groovy.transform.CompileStatic

/**
 * Static methods are very simple ways to execute commands.
 * It is recommended to use an instance as it is more flexible
 *
 * Options:
 *  exitCode : Change this value if the normal exit code is different than 0
 *  secret   : Does the command includes sensitive information that it should not be logged?
 *
 * Methods:
 *  getLines  : Get all output lines after completion
 *  getText   : Get all text after completion
 *  eachLine  : Get each stdout line
 *  eachError : Get each stderr line
 *  onFail    : Callback executed when command fails (during initialization or on exit)
 *
 * exec() options:
 *  timeout  : After how many milliseconds does the command should assume it failed? (default: wait until it finishes)
 *  background : Execute command on the background
 *
 * @since 1/9/17
 */
@CompileStatic
class Cmd {
    static interface Output {
        void call(String out)
    }
    static interface Lines {
        void call(List<String> lines)
    }
    static interface Fail {
        void call(String out, int code)
    }

    protected List<String> cmd
    protected boolean secret  = false     // if command contains sensitive information
    protected int exitCode    = 0         // expected exit code
    protected List<String> lines = []
    protected Output lineProcessor = null
    protected Output stdOut  = { String line ->
        lines << line
        if(lineProcessor) {
            lineProcessor.call(line)
        }
        if(!secret) {
            Log.v(line)
        }
    } as Output
    protected Output stdErr  = { String line -> if(!secret) { Log.v(line) } } as Output
    protected Lines onDone    = null // Optional
    protected Output onText   = null // Optional
    protected Fail onFail     = { String msg, int code -> Log.w("(Exit code: %d) %s", code, msg) } as Fail
    Process process = null

    Cmd(Collection cmd) {
        this.cmd = cmd.collect { it.toString() }
    }
    Cmd(String cmd, Collection args = []) {
        this.cmd = cmd.tokenize(" ") + args.collect { it.toString() }
    }
    Cmd(String... cmd) {
        this.cmd = cmd.toList()
    }
    //------------------ FLUID ---------------------
    Cmd arg(String args) {
        this.cmd = this.cmd + args.tokenize(" ")
        return this
    }
    Cmd arg(String... args) {
        this.cmd = this.cmd + args.toList()
        return this
    }
    Cmd arg(Collection args) {
        this.cmd = this.cmd + args.collect { it.toString() }
        return this
    }
    Cmd secret(boolean secret) {
        this.secret = secret
        return this
    }
    Cmd exitCode(int exitCode) {
        this.exitCode = exitCode
        return this
    }
    Cmd eachLine(Output stdOut) {
        if(stdOut) {
            this.lineProcessor = stdOut
        }
        return this
    }
    Cmd eachError(Output stdErr) {
        if(stdErr) {
            this.stdErr = stdErr
        }
        return this
    }
    Cmd getLines(Lines onDone) {
        if(onDone) {
            this.onDone = onDone
        }
        return this
    }
    Cmd getText(Output onText) {
        if(onText) {
            this.onText = onText
        }
        return this
    }
    Cmd onFail(Fail onFail) {
        this.onFail = onFail ?: { String line, int code -> } as Fail
        return this
    }
    //------------------ MAIN ------------------------
    /**
     * Same as 'exec' without timeout
     * @param background
     */
    Cmd exec(boolean background) {
        return exec(0, background)
    }
    /**
     * Execute command and get results as they are being produced.
     * example: exec(["tail", "-f", "syslog.log", { String line -> Log.i(line) })
     * @param cmd
     * @param out
     */
    Cmd exec(int timeout = 0, boolean background = false) {
        if(!secret) {
            Log.v("> " + cmd.join(" "))
        }
        // If the command ends in '&' its a background process
        if(cmd.last() == "&") {
            background = true
            cmd.removeLast()
        }
        // In order to use pipe, we need to translate it:
        String text = cmd.join("") // in case it is not correctly separated
        if(text.contains('|') || text.contains(";") || text.contains("&&")) {
            if(SysInfo.windows) {
                cmd = ["cmd", "/C", cmd.join(" ")]
            } else {
                cmd = ["sh", "-c", cmd.join(" ")]
            }
        }
        if(background) {
            Thread.start {
                processCommand(timeout)
            }
        } else {
            processCommand(timeout)
        }
        return this
    }

    /**
     * Cancel a process
     */
    void cancel() {
        if(process) {
            try {
                process?.closeStreams()
                process?.destroy()
            } catch(Exception ignore) {}
        }
    }
    /**
     * Process the command
     */
    protected void processCommand(int timeout) {
        try {
            ProcessBuilder pb = new ProcessBuilder()
            // Import PATH
            pb.environment().put("path", Config.env.get("path"))
            process = pb.command(cmd).start()
            boolean done = false
            Thread.start {
                try {
                    process.in.eachLine { stdOut.call(it) }
                    process.err.eachLine { stdErr.call(it) }
                } catch(Exception e) {
                    Log.v("Process ended before expected: %s", e)
                } finally {
                    done = true
                }
            }
            if(timeout > 0) {
                process.waitForOrKill(timeout)
            } else {
                process.waitFor()
            }
            int code = process ? process.exitValue() : -1
            if(code == exitCode) {
                if(onDone || onText || lineProcessor) {
                    // Wait for all text if we expect it
                    while (!done) {
                        sleep(Millis.MILLIS_10)
                    }
                    if (onDone) {
                        onDone.call(lines)
                    }
                    if (onText) {
                        onText.call(lines.join(SysInfo.newLine))
                    }
                }
            } else {
                if(code == 143) {
                    if (secret) {
                        onFail.call("Command timed out", code)
                    } else {
                        onFail.call("[${cmd.join(" ")}] Command timed out", code)
                    }
                } else {
                    if (secret) {
                        onFail.call("Exit code was not $exitCode", code)
                    } else {
                        onFail.call("[${cmd.join(" ")}] Exit code was not $exitCode", code)
                    }
                }
            }
        } catch(IOException e) {
            onFail.call(e.message, 2)
        }
        process = null
    }
    //----------- STATIC -------------
    /**
     * Returns true if command succeeded
     * @param cmd
     * @param args
     * @return
     */
    static boolean succeed(String cmd, Collection args = []) {
        return succeed(cmd.tokenize(" ") + args)
    }
    /**
     * Returns true if command succeeded
     * @param cmd
     * @return
     */
    static boolean succeed(Collection cmd) {
        boolean ok = true
        exec(cmd, {
            String out, int code ->
                ok = false
        })
        return ok
    }
    /**
     * Easiest way to execute a command using String
     * @param cmd
     * @param onFail
     * @return
     */
    static String exec(String cmd, Fail onFail = null) {
        return exec(cmd.tokenize(" "), onFail)
    }
    /**
     * Easiest way to execute a command using arguments as List
     * @param cmd
     * @param onFail
     * @return
     */
    static String exec(String cmd, Collection args, Fail onFail = null) {
        return exec(cmd.tokenize(" ") + args, onFail)
    }
    /**
     * Easiest way to execute a command using a List
     * @param cmd
     * @param onFail
     * @return
     */
    static String exec(Collection cmd, Fail onFail = null) {
        String out = ""
        new Cmd(cmd).onFail(onFail).getText({
            out = it
        }).exec()
        return out
    }
    /**
     * Easiest way to execute a command using String on the background
     * @param cmd
     * @param onFail
     * @return
     */
    static void async(String cmd, Output onDone, Fail onFail = null) {
        async(cmd.tokenize(" "), onDone, onFail)
    }
    /**
     * Easiest way to execute a command using arguments as List on the background
     * @param cmd
     * @param onFail
     * @return
     */
    static void async(String cmd, Collection args, Output onDone, Fail onFail = null) {
        async(cmd.tokenize(" ") + args, onDone, onFail)
    }
    /**
     * Easiest way to execute a command using a List on the background
     * @param cmd
     * @param onFail
     * @return
     */
    static void async(Collection cmd, Output onDone, Fail onFail = null) {
        new Cmd(cmd).onFail(onFail).getText({
            onDone.call(it)
        }).exec(true)
    }
}
