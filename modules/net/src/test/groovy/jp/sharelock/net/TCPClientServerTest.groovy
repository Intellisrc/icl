package jp.sharelock.net

import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

/**
 * Test client and server at the same time
 * @since 17/03/03.
 */
class TCPClientServerTest extends Specification {
    def port = NetworkInterface.getFreePort()
    TCPClient client
    TCPServer server
    String sendHello = "Hello Server"
    String sendDate = "What's the date?"
    String sendInfo = "Info Please"
    String sendExit = "exit"
    String replyHello = "Hi client!"
    String replyDate = "Date: "+(new Date().toYMD())
    String replyInfo = "Info:"+"\n"+replyHello+"\n"+replyDate
    String replyExit = "OK"
    def setup() {
        server = new TCPServer(port, {
            String received ->
                String reply = ""
                switch (received) {
                    case sendHello: reply = replyHello; break
                    case sendDate:  reply = replyDate; break
                    case sendInfo:  reply = replyInfo; break
                    case sendExit:  reply = replyExit; break
                    default:
                        assert false
                }
                return reply
        })
        assert server
        client = new TCPClient(port)
        assert client
    }
    def cleanup() {
        server.quit()
        //FIXME: quit doesn't exit if connection is open...
    }
    def "Listen and connect to port send Hello"() {
        setup:
            def async = new AsyncConditions()
            TCPClient.Request req = new TCPClient.Request(sendHello, {
                TCPClient.Response resp -> async.evaluate {
                    assert resp.toString() == replyHello
                }
            })
        when:
            client.sendRequest(req)
        then:
            async.await(10)
            noExceptionThrown()
    }
    def "Listen and connect to port send Date"() {
        setup:
        def async = new AsyncConditions()
        TCPClient.Request req = new TCPClient.Request(sendDate, {
            TCPClient.Response resp -> async.evaluate {
                assert resp.toString() == replyDate
            }
        })
        when:
        client.sendRequest(req)
        then:
        async.await(3)
        noExceptionThrown()
    }
    def "Sending multiple commands"() {
        setup:
            def async1 = new AsyncConditions()
            def async2 = new AsyncConditions()
            TCPClient.Request req1 = new TCPClient.Request(sendHello, {
                TCPClient.Response resp -> async1.evaluate {
                    assert resp.toString() == replyHello
                }
            })
            TCPClient.Request req2 = new TCPClient.Request(sendDate, {
                TCPClient.Response resp -> async2.evaluate {
                    assert resp.toString() == replyDate
                }
            })
        when:
            client.addRequest(req1)
            client.addRequest(req2)
            client.send()
        then:
            async1.await(300)
            async2.await(300)
            noExceptionThrown()
    }
    def "Sending multiple commands using short annotation"() {
        setup:
            def async1 = new AsyncConditions()
            def async2 = new AsyncConditions()
            TCPClient.Request req1 = new TCPClient.Request(sendHello, {
                TCPClient.Response resp -> async1.evaluate {
                    assert resp.toString() == replyHello
                }
            })
            TCPClient.Request req2 = new TCPClient.Request(sendDate, {
                TCPClient.Response resp -> async2.evaluate {
                    assert resp.toString() == replyDate
                }
            })
        when:
            client.sendRequest([req1, req2])
        then:
            async1.await(300)
            async2.await(300)
            notThrown Exception
    }
}