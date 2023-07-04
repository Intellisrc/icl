package com.intellisrc.web.samples


import com.intellisrc.web.WebService
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.sse.SseEventSource
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

import static com.intellisrc.web.samples.SSEService.SSETester

/**
 * @since 2023/06/30.
 */
class ServiciableSentEventsTest extends Specification {

    def "It should connect and receive messages"() {
        setup:
            def conds = new AsyncConditions()
            SSETester sseTester = new SSETester()
            WebService webService = new WebService(port: 9999).add(sseTester)
            webService.start(true)
        when:
            Client client = ClientBuilder.newClient()
            WebTarget target = client.target("http://localhost:9999/sse/")
            try (SseEventSource source = SseEventSource.target(target).build()) {
                source.register {
                    inboundSseEvent ->
                        conds.evaluate {
                            String data = inboundSseEvent.readData()
                            println data
                            assert true
                        }
                }
                source.open()
            }
            conds.await()
        then:
            assert true
        cleanup:
            webService.stop()
    }
}
