package com.intellisrc.core

import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

import java.nio.file.Files

/**
 * @since 17/11/17.
 */
class CmdTest extends Specification {
    def "Each line and all text should have the same length" () {
        setup:
            List<String> lines = []
            String res = Cmd.exec("ls /dev/")
            new Cmd("ls /dev/").eachLine {
                lines << it
            }.exec()
        expect:
            assert res.readLines().size() == lines.size()
    }
    def "Command as List"() {
        setup:
            String val = Cmd.exec(["echo","Hello"])
        expect:
            assert val == "Hello"
    }
    def "Command as String"() {
        expect:
            new Cmd(["date"]).getLines({
                List<String> out ->
                    assert out.first().contains(":")
            }).onFail({
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }).exec()
    }
    def "Command as String with spaces"() {
        setup:
            String out = ""
            new Cmd("echo Hello").getText({
                out = it
            }).onFail({
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }).exec()
        expect:
            assert out == "Hello"
    }
    def "Command as String array"() {
        expect:
            new Cmd("echo", "Hello").getText({
                assert it.trim() == "Hello"
            }).onFail({
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }).exec()
    }
    def "Command as String and params as List"() {
        setup:
            String out = ""
            new Cmd("echo",["Hello","World"]).eachLine {
                out = it
            }.onFail {
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }.exec()
        expect:
            assert out == "Hello World"
    }
    def "Command as String with pipe"() {
        setup:
            String out = ""
            new Cmd("echo Hello | sed 's/He/Po/'").getText({
                out = it
                println it
            }).onFail({
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }).exec()
        expect:
            assert out == "Pollo"
    }
    def "Command List line by line"() {
        setup:
            File tmp = Files.createTempFile("test",".test").toFile()
            tmp << """URL Found: http://www.w3.org/1999/02
URL Found: http://purl.org/dc/elements/1.1
URL Found: http://ns.adobe.com/xap/1.0
"""
        expect:
            assert tmp.exists()
            assert tmp.text.contains("URL Found")
            new Cmd("cat", tmp.absolutePath).eachLine({
                String line ->
                    println "---- $line ----"
                    assert line.startsWith("URL Found:")
            }).getText({
                println "**********"
                println it
                println "**********"
            }).exec()
        cleanup:
            tmp.delete()

    }
    def "Command in the background static"() {
        setup:
            def async = new AsyncConditions()
            def cmd = ["sleep", 1, "&&", "echo", "done"]
        expect:
            Cmd.async(cmd, {
                String out ->
                    async.evaluate({ assert out == "done" })
            },
                {
                    String out, int code ->
                        assert false //Should not end up here
                })
            println "Waiting for command to finish..."
            async.await(2)
    }

    def "Command in the background, semicolon not separated"() {
        setup:
            def async = new AsyncConditions()
            def cmd = "sleep 1; echo done"
        expect:
            new Cmd(cmd).getText({
                String out ->
                    async.evaluate({ assert out == "done" })
            }).onFail({
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }).exec(true)
            println "Waiting for command to finish..."
            async.await(2)
    }

    def "Building with args"() {
        setup:
            String out = ""
            new Cmd("echo")
                .arg("hello")
                .arg(["this","world"])
                .arg("not","other")
                .secret(false).getText({
                    out = it
                }).exec()
        expect:
            assert out == "hello this world not other"
    }

    def "Timeout"() {
        setup:
            def async = new AsyncConditions()
            new Cmd("sleep 15").onFail({
                String out, int code ->
                    async.evaluate({ assert ! out.empty })
            }).exec(Millis.HALF_SECOND,true)
        expect:
            async.await(50)
    }

    def "Testing exit code"() {
        setup:
            new Cmd("echo 'me'; exit 3").exitCode(3).onFail({
                String msg, int code ->
                    assert false : "[$code] Command failed: " + msg
            }).getText({
                assert true
            }).exec()
    }

    def "Command not found"() {
        setup:
            new Cmd("unknown_command blabla").onFail({
                String msg, int code ->
                    println "[$code] OK: " + msg
                    assert true
            }).exec()
    }

    def "Cancel command"() {
        when:
            Cmd cmd = new Cmd("sleep 60").exec(true)
            sleep(1000)
            String pgrep = Cmd.exec("pgrep sleep")
            println pgrep
        then:
            assert cmd.process.pid()
            assert cmd.process.alive
            assert ! pgrep.empty
        when:
            cmd.cancel()
        then:
            assert Cmd.exec("pgrep sleep").empty
    }
}