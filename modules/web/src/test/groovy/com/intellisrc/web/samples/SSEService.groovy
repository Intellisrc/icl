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
 * @since 2023/06/30.
 */
class SSEService {
    static class SSEMain implements ServiciableSingle {
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
    static class SSETester implements ServiciableSentEvents {
        String path = "/sse"
        int i = 0

        SSETester() {
            Tasks.add(IntervalTask.create({
                sse.sendAll(i++, [
                    service: "Power",
                    action: "ON"
                ])
            }, "Sender", Millis.SECOND_10, Millis.SECOND_5))
        }
    }
    static void main(String[] args) {
        new WebService(port: 9999)
            .add(new SSEMain())
            .add(new SSETester())
            .start()
    }
}
