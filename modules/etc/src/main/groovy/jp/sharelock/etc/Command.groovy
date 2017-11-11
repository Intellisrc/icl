package jp.sharelock.etc

import groovy.transform.CompileStatic

/**
 * @since 1/9/17.
 *  secret: if true, it will not log arguments
 */
//@Immutable
@CompileStatic
class Command {
    // Properties:
    boolean secret = false // Does the command includes sensitive information that it should not be logged?
    int exit = 0 //Change this value if the normal exit code is different than 0
    int timeout = 10000
    interface Callback {
        void done(String out)
        void fail(String out, int code)
    }
    Callback callback

    /**
     * Execute a command Synchronously
     * @param cmd
     * @param args
     * @return
     */
    void exec(String cmd, Callback callback) {
        exec(cmd, [], callback)
    }
    void exec(String cmd, Collection<String> args = [], Callback callback = null) {
        assert cmd: "Invalid Command"

        final std_out = new StringBuilder(), std_err = new StringBuilder()
        final prc
        int exitCode
        //TODO: iterate if cmd is a collection
        final arg_str = args.join(" ")

        String command = "$cmd $arg_str" //Allow glob operations //TODO: its different in Windows
        prc = command.execute()
        prc.consumeProcessOutput(std_out, std_err)
        prc.waitForOrKill(timeout)
        exitCode = prc.exitValue()

        if(!std_err.toString().isEmpty()) {
            Log.e(cmd, std_err.toString())
        }
        if (secret) {
            Log.v("> " + cmd + " ( " +args.size() + "hidden args )")
        } else {
            Log.v("> " + command)
        }
        if (std_err) {
            Log.w(std_err)
        }
        if (exitCode == exit) {
            if(callback) {
                callback.done(std_out.toString())
            } else if(this.callback) {
                this.callback.done(std_out.toString())
            }
        } else {
            if(callback) {
                callback.fail(std_err.toString(), exitCode)
            } else if(this.callback) {
                this.callback.fail(std_err.toString(), exitCode)
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
    void execAsync(String cmd, Collection<String> args = [], Callback callback = null) {
        Thread.start({
            exec(cmd, args, callback)
        })
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
