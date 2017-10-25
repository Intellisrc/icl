package jp.sharelock.net

import jp.sharelock.net.TCPClient
import jp.sharelock.net.TCPServer
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
    String tosend = "Hello Server"
    String reply = "Hi client!"
    def setup() {
        server = new TCPServer(port, {
            String received ->
                println "Received $received"
                assert received == tosend
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
    def "Listen and connect to port"() {
        setup:
            def async = new AsyncConditions()
            TCPClient.Request req = new TCPClient.Request(tosend)
        when:
            client.sendRequest(req,{
                TCPClient.Response resp -> async.evaluate {
                    println "Replied: " + resp.toString()
                    assert resp.toString() == reply
                }
            })
        then:
            async.await()
    }
}