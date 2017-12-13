package jp.sharelock.web.samples

import jp.sharelock.web.JSON
import jp.sharelock.web.WebSocketServiceClient

import java.util.zip.GZIPInputStream

/**
 * Example of a simple Echo Client.
 * Using: StackOverflow API
 * https://api.stackexchange.com/docs
 */
class StackOverflowChatClient {
    static void Connect() {
        String uname = "StackBOT"
        int timeout = 0
        def wssc = new WebSocketServiceClient(
                //hostname: "localhost",
                //port : 8888,
                path : "chat?user=$uname"
        )
        wssc.connect({
            HashMap msg ->
                String message = msg.message
                String reply = "<ul>"
                if(msg.type == "txt" && msg.user != uname && message.startsWith('$')) {
                    message = message.replace('$','')
                    String toSend = URLEncoder.encode(message, "UTF-8")
                    byte[] gziped = "https://api.stackexchange.com/2.2/search/advanced?order=desc&sort=relevance&q=$toSend&accepted=True&site=stackoverflow".toURL().getBytes()
                    GZIPInputStream gzip = new GZIPInputStream (new ByteArrayInputStream (gziped))
                    def response = JSON.toMap(gzip.getText("UTF-8"))
                    def firstItem = response.items.each {
                        String title = it.title
                        String link = it.link
                        reply += "<li><a href='$link' target='_blank'>$title</a>"
                    }
                    reply += "</ul>"
                    /*int question_id = firstItem.question_id as Integer
                    if(question_id > 0) {

                        Object res_answers = jsonSlurper.parseText("https://api.stackexchange.com/2.2/questions/$question_id/answers?order=desc&sort=votes&site=stackoverflow".toURL().text)
                        int answer_id = Integer.parseInt(res_answers.items.first().answer_id)
                        if (answer_id > 0) {
                            Object res_answers = jsonSlurper.parseText("https://api.stackexchange.com/2.2/questions/$question_id/answers?order=desc&sort=votes&site=stackoverflow".toURL().text)
                            reply += ""
                        }
                    } else {
                        reply = "I couldn't find any question about that."
                    }*/
                    wssc.sendMessage(reply)
                }
        })
        wssc.sendMessage("How can I help you? write: '\$ Your question'")
        while(timeout == 0) {
            Thread.sleep(1000L)
        }
    }
}
