package jp.sharelock.etc

/**
 * This is a simplified way to use Command
 * see Command for references
 * @since 17/12/01.
 */
class Cmd {
    static void exec(final String cmd, final Command.Done done, final Command.Fail fail = null) {
        new Command().exec(cmd, done, fail)
    }
    static void exec(final String cmd, List args = [], final Command.Done done = null, final Command.Fail fail = null) {
        new Command().exec(cmd, args, done, fail)
    }
    static void exec(final List<String> commands, final Command.Done done = null, final Command.Fail fail = null) {
        new Command().exec(commands, done, fail)
    }
    static void exec(final Map<String,List> commands, final Command.Done done = null, final Command.Fail fail = null) {
        new Command().exec(commands, done, fail)
    }
    static void async(final String cmd, final Command.Done done, final Command.Fail fail = null) {
        new Command().execAsync(cmd, done, fail)
    }
    static void async(final String cmd, List args = [], final Command.Done done = null, final Command.Fail fail = null) {
        new Command().execAsync(cmd, args, done, fail)
    }
    static void async(final List<String> commands, final Command.Done done = null, final Command.Fail fail = null) {
        new Command().execAsync(commands, done, fail)
    }
    static void async(final Map<String,List> commands, final Command.Done done = null, final Command.Fail fail = null) {
        new Command().execAsync(commands, done, fail)
    }
}
