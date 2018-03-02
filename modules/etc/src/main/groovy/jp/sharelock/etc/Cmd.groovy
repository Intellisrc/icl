package jp.sharelock.etc

import groovy.transform.CompileStatic

/**
 * @since 1/9/17.
 *
 * Options:
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
@CompileStatic
class Cmd {
    interface Done {
        void done(String out)
    }
    interface Fail {
        void fail(String out, int code)
    }
    static class Command {
        boolean secret = false
        int exit = 0
        int timeout = 10000
        /**
         * Execute multiple commands one by one
         * example:
         * [ "ls", "top" ]
         * the output will be added
         * @param cmds
         * @param done
         */
        void exec(final List<String> cmds, final Done onDone = null, final Fail onFail = null) {
            exec(wrapCmds(cmds), onDone, onFail)
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
         * @param done
         */
        void exec(final Map<String,List> cmds, final Done onDone = null, final Fail onFail = null) {
            String totOut = ""
            String totErr = ""
            cmds.each {
                String cmd, List args ->
                    exec(cmd, args, {
                        String out ->
                            totOut += (totOut.isEmpty() ? "" : "\n") + out
                    }, {
                        String out, int code ->
                            totErr += (totErr.isEmpty() ? "" : "\n") + out
                    })
            }
            totOut = totOut.trim()
            totErr = totErr.trim()
            if(onDone) {
                if(totOut) {
                    onDone.done(totOut)
                }
                if (totErr) {
                    if(onFail) {
                        onFail.fail(totErr, 1)
                    } else {
                        Log.e("Commandzzz failed with error: %s",totErr)
                    }
                }
            }

        }
        /**
         * Execute multiple commands one by one
         * example:
         * [ "ls", "top" ]
         * the output will be added
         * @param cmds
         * @param done
         */
        void async(final List<String> cmds, final Done onDone = null, final Fail onFail = null) {
            Thread.start {
                exec(wrapCmds(cmds), onDone, onFail)
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
         * @param done
         */
        void async(final Map<String,List> cmds, final  Done onDone = null, final Fail onFail = null) {
            Thread.start {
                exec(cmds, onDone, onFail)
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
        void exec(final String cmd, final Done onDone, final Fail onFail = null) {
            exec(cmd, [], onDone, onFail)
        }
        void exec(final String cmd, final List args = [], final Done onDone = null, final Fail onFail = null) {
            assert cmd: "Invalid Commandzzz"

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
                if(onDone) {
                    onDone.done(std_out.toString().trim())
                }
            } else {
                if (onFail) {
                    onFail.fail(std_err.toString().trim(), exitCode)
                } else {
                    Log.e("Commandzzz failed with exit code %d and message: %s", exitCode, std_err.toString())
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
        void async(final String cmd, final Done onDone, final Fail onFail = null) {
            async(cmd, [], onDone, onFail)
        }
        void async(final String cmd, final List args = [], final Done onDone = null, final Fail onFail = null) {
            Thread.start {
                exec(cmd, args, onDone, onFail)
            }
        }
    }

    /**
     * Set the options for the command
     * @param opts
     * @return
     */
    static Command options(Map opts) {
        def comm = new Command()
        if(opts) {
            comm.with {
                if(opts.secret) {
                    secret = opts.secret as Boolean
                }
                if(opts.exit) {
                    exit = opts.exit as Integer
                }
                if(opts.timeout) {
                    timeout = opts.timeout as Integer
                }
            }
        }
        return comm
    }
    ////// STATIC METHODS
    /**
     * execute command with no args
     * @param cmd
     * @param onDone
     * @param onFail
     */
    static void exec(final String cmd, final Done onDone, final Fail onFail = null) {
        new Command().exec(cmd, [], onDone, onFail)
    }
    /**
     * execute command with no other options or with args
     * @param cmd
     * @param args
     * @param onDone
     * @param onFail
     */
    static void exec(final String cmd, final List args = [], final Done onDone = null, final Fail onFail = null) {
        new Command().exec(cmd, args, onDone, onFail)
    }
    /**
     * execute a list of commands without args
     * @param commands
     * @param onDone
     * @param onFail
     */
    static void exec(final List<String> commands, final Done onDone = null, final Fail onFail = null) {
        new Command().exec(commands, onDone, onFail)
    }
    /**
     * execute a list of commands with args
     * @param commands
     * @param onDone
     * @param onFail
     */
    static void exec(final Map<String,List> commands, final Done onDone = null, final Fail onFail = null) {
        new Command().exec(commands, onDone, onFail)
    }
    /**
     * execute command without args asynchronously
     * @param cmd
     * @param onDone
     * @param onFail
     */
    static void async(final String cmd, final Done onDone, final Fail onFail = null) {
        new Command().async(cmd, onDone, onFail)
    }
    /**
     * execute command with args asynchronously
     * @param cmd
     * @param args
     * @param onDone
     * @param onFail
     */
    static void async(final String cmd, final List args = [], final Done onDone = null, final Fail onFail = null) {
        new Command().async(cmd, args, onDone, onFail)
    }
    /**
     * execute commands without args asynchronously
     * @param commands
     * @param onDone
     * @param onFail
     */
    static void async(final List<String> commands, final Done onDone = null, final Fail onFail = null) {
        new Command().async(commands, onDone, onFail)
    }
    /**
     * execute commands with args asynchronously
     * @param commands
     * @param onDone
     * @param onFail
     */
    static void async(final Map<String,List> commands, final Done onDone = null, final Fail onFail = null) {
        new Command().async(commands, onDone, onFail)
    }
}