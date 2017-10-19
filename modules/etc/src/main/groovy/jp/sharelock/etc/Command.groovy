package jp.sharelock.etc

import jp.sharelock.etc.Log

/**
 * @since 1/9/17.
 *  secret: if true, it will not log arguments
 */
//@Immutable
@groovy.transform.CompileStatic
class Command {
    // Properties:
    boolean secret = false // Does the command includes sensitive information that it should not be logged?
    int exit = 0 //Change this value if the normal exit code is different than 0
    int timeout = 10000
    Closure onDone = { String out -> out }
    Closure onFail = { String out, Integer code -> out }

    /**
     * Execute a command Synchronously
     * @param cmd
     * @param args
     * @return
     */
    void execSync(String cmd, Collection<String> args = []) {
        assert cmd: "Invalid Command"

        final std_out = new StringBuilder(), std_err = new StringBuilder()
        final prc
        final timeout = 60000
        int exitCode
        //TODO: iterate if cmd is a collection
        final arg_str = args.join(" ")

        String command = "$cmd $arg_str" //Allow glob operations //TODO: its different in Windows
        println "CMD: [$command]"
        prc = command.execute()
        prc.consumeProcessOutput(std_out, std_err)
        prc.waitForOrKill(timeout)
        exitCode = prc.exitValue()

        if(!std_err.toString().isEmpty()) {
            println(std_err.toString())
        }
        if (secret) {
            Log.v(cmd, "(hidden args) : " + args.size())
        } else {
            Log.v(cmd, " : " + args.join(","))
        }
        if (std_err) {
            Log.w(std_err)
        }
        if (exitCode == exit) {
            onDone(std_out.toString())
        } else {
            onFail(std_err.toString(), exitCode)
        }
    }
    /**
     * Executes a command Asynchronously
     * @param cmd
     * @param args
     * @param timeout
     * @return
     */
    void execAsync(String cmd, Collection<String> args = []) {
        Thread.start({
            execSync(cmd, args)
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
