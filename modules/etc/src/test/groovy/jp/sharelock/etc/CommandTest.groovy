package jp.sharelock.etc

import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

/**
 * @since 17/11/17.
 */
class CommandTest extends Specification {
    def "Command Test As parameter"() {
        setup:
            def arg = "hello"
            def cmd = new Command(timeout: 2000, secret: true)
        when:
            cmd.exec("echo",[arg], {
                String output ->
                    output.eachLine {
                        println "> " + it
                    }
                    assert output == "hello"
            } as Command.Callback)
        then:
            notThrown Exception
    }
    def "Command Test Done"() {
        setup:
            def cmd = "found.not"
        expect:
            new Command().exec(cmd, new Command.Callback() {
                void done(String out) {
                    assert false //If comes here, fail
                }
                void fail(String out, int code) {
                    assert code > 0
                    println "Exit Code: $code"
                }
            })
    }
    def "Command Exec Multiple"() {
        setup:
        def cmds = ["sleep 3","echo done"]
        expect:
        new Command(timeout: 5000).exec(cmds, new Command.Callback() {
            @Override
            void done(String out) {
                assert out == "done"
            }
            @Override
            void fail(String out, int code) {
                assert false //Should not endup here
            }
        })
    }
    def "Command Exec Multiple Using Map"() {
        setup:
        def cmds = [ sleep : [3], echo  : ["done"] ]
        expect:
        new Command(timeout: 5000).exec(cmds, new Command.Callback() {
            @Override
            void done(String out) {
                assert out == "done"
            }
            @Override
            void fail(String out, int code) {
                assert false //Should not endup here
            }
        })
    }
    def "Command Exec Async"() {
        setup:
            def async = new AsyncConditions()
            def cmds = [ sleep : [3], echo : ["done"] ]
        expect:
            new Command(timeout: 5000).execAsync(cmds, new Command.Callback() {
                @Override
                void done(String out) {
                    async.evaluate({ assert out == "done" })
                }
                @Override
                void fail(String out, int code) {
                    assert false //Should not endup here
                }
            })
            println "Waiting for command to finish..."
            async.await(5)
    }
}