package com.intellisrc.web.samples

import com.intellisrc.net.LocalHost
import com.intellisrc.web.WebService
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

import static com.intellisrc.web.samples.SSEService.SSETestServer

/**
 * @since 2023/06/30.
 */
class ServiciableSentEventsTest extends Specification {

    def "It should connect and receive messages"() {
        setup:
            def conds = new AsyncConditions()
            def port = LocalHost.freePort
            SSETestServer sseTester = new SSETestServer()
            WebService webService = new WebService(port: port).add(sseTester)
            webService.start(true)
        when:
            def sseUrl = "http://localhost:${port}/sse/".toURL()

            def connection = sseUrl.openConnection() as HttpURLConnection
            connection.setRequestMethod('GET')
            connection.setRequestProperty('Accept', 'text/event-stream')

            def reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            try {
                conds.evaluate {
                    // we can loop indefinitely as far as the server send messages
                    // if we use reader.eachLine { ... }
                    // but for this test, we just need to confirm one complete response

                    // First line should be the header:
                    assert reader.readLine().startsWith("event:")

                    // Second line should contain the data:
                    def eventData = reader.readLine().trim()
                    assert eventData.startsWith("data:")
                    println("Received event: ${eventData.substring(5)}")
                    boolean dataReceived = eventData.contains("Power")
                    assert dataReceived

                    // Third line should contain the id:
                    assert reader.readLine().startsWith("id")

                    // Finally the last line should be empty
                    // Next line should be empty:
                    assert reader.readLine().isEmpty()
                }
            } finally {
                reader.close()
                connection.disconnect()
            }
            conds.await()
        then:
            assert true
        cleanup:
            webService.stop()
    }
}
