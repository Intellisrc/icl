package com.intellisrc.net

import com.intellisrc.core.SysInfo
import spock.lang.Specification

/**
 * @since 17/10/24.
 */
class SmtpTest extends Specification {
    Smtp smtp
    File attachFile
    def recipient1 = "me@example.com"
    def recipient2 = "you@example.com"
    def setup() {
        /* Example on how to set it locally
        smtp = new Smtp(
            from: "someone@example.com",
            host: "example.com",
            username: "example123",
            password: "top_secret",
            simulate: true //Option normally not used
        )
        */
        // If config is loaded, no need to specify connection settings
        smtp = new Smtp(
            from: "unit_test@example.com",
            simulate: true
        )
        attachFile = new File("src/test/resources/star.png")
    }
    /**
     * Send a single mail (just text)
     */
    def "SendTest Email"() {
        expect:
            assert smtp.send(recipient1,"Testing SMTP", "This is the body")
    }
    /**
     * Send a single mail to default recipient (in config file)
     */
    def "SendTest Email to Default"() {
        setup:
            smtp.defaultTo = "default@example.com"
        expect:
            assert smtp.sendDefault("Testing SMTP", "This is the body")
    }
    /**
     * Testing if attachment exists
     */
    def "attach Exists"() {
        expect:
            println attachFile.absolutePath
            assert attachFile.exists()
    }
    /**
     * This test will send 2 attachments in one message
     */
    def "SendAttachment"() {
        setup:
            def file = new File(File.tempDir, "example.txt")
            file << "Hello this is just a test"
            smtp.addAttachment(file)
            smtp.addAttachment(attachFile)
        expect:
            assert smtp.send(recipient1,"Testing SMTP with Attachment", "This is the body")
        cleanup:
            file.delete()
    }
    def "Two recipients"() {
        setup:
            def recipients = [recipient1, recipient2]
        expect:
            assert smtp.send(recipients,"Testing multiple recipients","This is the body")
    }
    def "HTML and text"() {
        expect: assert smtp.send(recipient1,"Testing HTML and text", "<h1>Hello! this is <i>H1</i></h1>", "This is TXT format")
    }
    def "HTML and text with Attachments"() {
        setup:
        def file = new File(File.tempDir,"example.txt")
        file << "Hello this is just a test"
        smtp.addAttachment(file)
        expect: assert smtp.send(recipient1,"Testing HTML and text", "<h1>Hello! this is <i>H1</i></h1>", "This is TXT format")
        cleanup:
        file.delete()
    }
    def "Specifying recipient type"() {
        def sendTo = [:] as Map<String, Smtp.Mode>
        sendTo[recipient1] = Smtp.Mode.CC
        sendTo[recipient2] = Smtp.Mode.BCC
        expect: assert smtp.send(sendTo,"Testing recipient Type", "This is body")
    }
    def "Mail magazine"() {
        setup:
            def to = [ "test@example.com" ]
            def html = new File(File.tempDir,  "magazine.html")
            def plain = new File(File.tempDir,  "magazine.txt")
            html.text = "<h1>Hello</h1>"
            plain.text = "Hello"
        expect:
            assert html.exists()
            assert plain.exists()
            to.each {
                println it
                assert smtp.send(
                        it,
                        "Magazine",
                        html.text,
                        plain.text
                )
            }
        cleanup:
            html.delete()
            plain.delete()
    }
}
