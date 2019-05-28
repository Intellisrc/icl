package com.intellisrc.core

import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

/**
 * @since 17/11/17.
 */
class CmdTest extends Specification {
    def "Command Test As parameter"() {
        setup:
            def arg = "hello"
            def cmd = Cmd.options(timeout: 2000, secret: true)
        when:
            cmd.exec("echo",[arg], {
                String output ->
                    output.eachLine {
                        println "> " + it
                    }
                    assert output == "hello"
            })
        then:
            notThrown Exception
    }
    def "Command Test Done"() {
        setup:
            def cmd = "found.not"
        expect:
            Cmd.exec(cmd, {
                    assert false //If comes here, fail
                },
         {
                    String out, int code ->
                    assert code > 0
                    println "Exit Code: $code"
            })
    }
    def "Command Exec Multiple"() {
        setup:
        def cmds = ["sleep 3","echo done"]
        expect:
        Cmd.options(timeout: 5000).exec(cmds, {
            String out ->
                assert out == "done"
        },
 {
            assert false //Should not endup here
        })
    }
    def "Command Exec Multiple Using Map"() {
        setup:
        def cmds = [ sleep : [3], echo  : ["done"] ]
        expect:
        Cmd.options(timeout: 5000).exec(cmds, {
            String out ->
                assert out == "done"
            },
     {
            assert false //Should not endup here
            })
    }
    def "Command Exec Async"() {
        setup:
            def async = new AsyncConditions()
            def cmds = [ sleep : [3], echo : ["done"] ]
        expect:
            Cmd.options(timeout: 5000).async(cmds, {
                String out ->
                    async.evaluate({ assert out == "done" })
                },
         {
                String out, int code ->
                    assert false //Should not endup here
                })
            println "Waiting for command to finish..."
            async.await(5)
    }
}