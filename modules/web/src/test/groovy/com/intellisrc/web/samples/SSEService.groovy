package com.intellisrc.web.samples

import com.intellisrc.core.Millis
import com.intellisrc.etc.Mime
import com.intellisrc.thread.IntervalTask
import com.intellisrc.thread.Tasks
import com.intellisrc.web.WebService
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.ServiciableSentEvents
import com.intellisrc.web.service.ServiciableSingle

/**
 * This class include the SSE client and the SSE server to be tested
 * on the browser.
 *
 * @since 2023/06/30.
 */
class SSEService {
    static int port = 9999
    /**
     * This class will launch an html+javascript client
     */
    static class SSETestClient implements ServiciableSingle {
        @Override
        Service getService() {
            return new Service(
                contentType: Mime.HTML,
                action: {
                    return """
<html>
    <body>
        <h1>SSE Test</h1>
        <ol id="list">
        </ol>
        <script>
            const evtSource = new EventSource("/sse/");
            evtSource.onopen = (ev) => {
                console.log("Opened")
                console.log(ev)
            };
            evtSource.onmessage = (ev) => { 
                console.log(ev) 
                const newElement = document.createElement("li");
                const eventList = document.getElementById("list");
                newElement.textContent = "message: " + event.data;
                eventList.appendChild(newElement);
            };
            evtSource.onerror = (err) => {
                console.error("EventSource failed:", err);
            };
        </script>
    </body>
</html>
"""
                }
            )
        }
    }
    /**
     * This class is a simple implementation of a SSE server using this library
     */
    static class SSETestServer implements ServiciableSentEvents {
        String path = "/sse/"
        int i = 0

        SSETestServer() {
            Tasks.add(IntervalTask.create({
                broadcast(i++, [
                    service: "Power",
                    action: "ON"
                ])
            }, "Sender", Millis.SECOND_10, Millis.SECOND_5))
        }
    }
    static void main(String[] args) {
        new WebService(port: port)
            .add(new SSETestClient())
            .add(new SSETestServer())
            .start()
    }
}
