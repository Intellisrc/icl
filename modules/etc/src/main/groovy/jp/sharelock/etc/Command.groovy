package jp.sharelock.etc

import groovy.transform.CompileStatic

/**
 * @since 1/9/17.
 *  exit     : Change this value if the normal exit code is different than 0
 *  secret   : Does the command includes sensitive information that it should not be logged?
 *  timeout  : After how many milliseconds does the command should assume it failed?
 *
 *  Note: This Class does not accept pipes. To use pipes, do:
 *
 *  def proc = "grep 'something'".execute() | "sed 's/hello/bye/'".execute() | 'awk "{ print $1 }"''.execute()
 *  println proc.text
 *
 *  It could be implemented passing the result of each command to the next one, however I'm not sure if
 *  would be exactly as expected (... how does arguments should be handled?)
 *  The recommended way is to process each command in nested configuration (more elegant and no dependencies) or
 *  to create an script and execute it only once (more performance efficient).
 *
 */
//@Immutable
@CompileStatic
class Command {
    // Properties:
    boolean secret = false
    int exit = 0
    int timeout = 10000
    interface Callback {
        void done(String out)
        void fail(String out, int code)
    }
    private Callback callback

    /**
     * Execute multiple commands one by one
     * example:
     * [ "ls", "top" ]
     * the output will be added
     * @param cmds
     * @param callback
     */
    void exec(List<String> cmds, Callback callback = null) {
        exec(wrapCmds(cmds), callback)
    }
    /**
     * Execute multiple commands one by one with arguments
     * example:
     * [
     *   "ls" : ["-a","-b"],
     *   "rm" : ["-r","-f"]
     * ]
     * the output will be added
     * @param cmds
     * @param callback
     */
    void exec(Map<String,List> cmds, Callback callback = null) {
        String totOut = ""
        String totErr = ""
        cmds.each {
            String cmd, List args ->
                exec(cmd, args, new Callback() {
                    @Override
                    void done(String out) {
                        totOut += (totOut.isEmpty() ? "" : "\n") + out
                    }

                    @Override
                    void fail(String out, int code) {
                        totErr += (totErr.isEmpty() ? "" : "\n") + out
                    }
                })
        }
        totOut = totOut.trim()
        totErr = totErr.trim()
        if(callback) {
            if(totOut) {
                callback.done(totOut)
            }
            if(totErr) {
                callback.fail(totErr, 1)
            }
        }

    }
    /**
     * Execute multiple commands one by one
     * example:
     * [ "ls", "top" ]
     * the output will be added
     * @param cmds
     * @param callback
     */
    void execAsync(List<String> cmds, Callback callback = null) {
        Thread.start {
            exec(wrapCmds(cmds), callback)
        }
    }
    /**
     * Execute multiple commands one by one with arguments
     * example:
     * [
     *   "ls" : ["-a","-b"],
     *   "rm" : ["-r","-f"]
     * ]
     * the output will be added
     * @param cmds
     * @param callback
     */
    void execAsync(Map<String,List> cmds, Callback callback = null) {
        Thread.start {
            exec(cmds, callback)
        }
    }
    /**
     * Wrapper from List to Map
     * @param cmds
     * @return
     */
    private static Map<String,List> wrapCmds(List<String> cmds) {
        def cmd = [:]
        cmds.each {
            cmd[it] = []
        }
        return cmd
    }
    /**
     * Execute a command Synchronously
     * @param cmd
     * @param args
     * @return
     */
    void exec(String cmd, Callback callback) {
        exec(cmd, [], callback)
    }
    void exec(String cmd, List args = [], Callback callback = null) {
        assert cmd: "Invalid Command"

        final std_out = new StringBuilder(), std_err = new StringBuilder()
        final prc
        int exitCode
        final arg_str = args.collect{ it.toString() }.join(" ")

        String command = "$cmd $arg_str"
        try {
            prc = command.execute()
            prc.consumeProcessOutput(std_out, std_err)
            prc.waitForOrKill(timeout)
            exitCode = prc.exitValue()
        } catch (Exception e) {
            exitCode = 1
            std_err << e.message
        }
        if (secret) {
            Log.v("> " + cmd + " ( " +args.size() + " hidden args )")
        } else {
            Log.v("> " + command)
        }
        if (std_err) {
            Log.e(std_err.toString())
        }
        if (exitCode == exit) {
            if(callback) {
                callback.done(std_out.toString().trim())
            } else if(this.callback) {
                this.callback.done(std_out.toString().trim())
            }
        } else {
            if(callback) {
                callback.fail(std_err.toString().trim(), exitCode)
            } else if(this.callback) {
                this.callback.fail(std_err.toString().trim(), exitCode)
            }
        }
    }
    /**
     * Executes a command Asynchronously
     * @param cmd
     * @param args
     * @param timeout
     * @return
     */
    void execAsync(String cmd, Callback callback) {
        execAsync(cmd,[],callback)
    }
    void execAsync(String cmd, List args = [], Callback callback = null) {
        Thread.start {
            exec(cmd, args, callback)
        }
    }

    /**
     * What to return when is used as boolean
     * @return
     */
    /*
    def asBoolean() {
        return exit = ret_code
    }*/
}
