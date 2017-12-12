package jp.sharelock.web.samples

import jp.sharelock.web.WebSocketServiceClient
import groovy.json.JsonSlurper

/**
 * Example of a simple Echo Client.
 * Using:
 * http://program-o.com/ (https://github.com/Program-O/Program-O)
 * https://quotesondesign.com/api-v4-0/
 *
 * Other chat APIs:
 * https://docs.api.ai/
 * https://developer.pandorabots.com/ (https://github.com/pandorabots/pb-java)
 * http://www.botmill.io/
 * IDEA: get Q/A from http://www.fanpop.com/clubs/random/answers/date and stackoverflow
 */
class ChatClient
{
    static void Connect() {
        final int QUIT_TIMEOUT = 600
        final int MAX_REPEAT = 30 //Prevent repeating itself
        String uname = "RoboCUP"
        int timeout = QUIT_TIMEOUT
        WebSocketServiceClient wssc = new WebSocketServiceClient(
                //hostname: "localhost",
                //port : 8888,
                path : "chat?user=$uname"
        )
        def last_responses = []
        wssc.connect({
            Map msg ->
                if(msg.type == "txt" && msg.user != uname) {
                    String botsay = ""
                    if(new Random().nextInt(5) < 2) {
                        if (msg.message.toLowerCase().contains("hi") || msg.message.toLowerCase().contains("hello")) {
                            botsay = "Do you really think I care about you?"
                        } else if (msg.message.contains(":)") || msg.message.contains(":D")) {
                            botsay = "Please don't smile here, nobody is watching!"
                        } else if (msg.message.contains(":(")) {
                            botsay = "Poor little thing!"
                        } else if (msg.message.contains(":|")) {
                            botsay = "No comments..."
                        } else if (msg.message.contains(":/")) {
                            botsay = "So?"
                        } else if (msg.message.contains(":P")) {
                            botsay = "Your tongue is dirty, can't you see?"
                        }
                    }
                    if(botsay.isEmpty() || last_responses.contains(botsay)) {
                        JsonSlurper jsonSlurper = new JsonSlurper()
                        String toSend = URLEncoder.encode(msg.message, "UTF-8")
                        Object response = jsonSlurper.parseText("http://api.program-o.com/v2/chatbot/?bot_id=12&say=$toSend&convo_id=robocup_9999&format=json".toURL().text)
                        botsay = response.botsay
                    }
                    if (botsay.isEmpty() || last_responses.contains(botsay)) {
                        if(msg.message.contains("?")) {
                            botsay = getAnswer()
                        } else {
                            botsay = getStatement()
                        }
                        //Last resource to prevent repetition:
                        if(last_responses.contains(botsay)) {
                            JsonSlurper jsonSlurper = new JsonSlurper()
                            Object response = jsonSlurper.parseText("http://quotesondesign.com/wp-json/posts?filter[orderby]=rand&filter[posts_per_page]=1".toURL().text)
                            HashMap res = response.first()
                            botsay = res.content
                            if(botsay.isEmpty()) {
                                botsay = getStatement()
                            } else {
                                botsay = botsay.replaceAll(/<.*?>/, '')
                            }
                        }
                    }
                    last_responses << botsay //Keep track of what was replied before
                    if(last_responses.size() > MAX_REPEAT) {
                        last_responses.remove(0)
                    }
                    wssc.sendMessage(botsay)
                    timeout = QUIT_TIMEOUT
                }
        })
        wssc.sendMessage("Yoo Humans!")
        while(timeout > 0) {
            Thread.sleep(1000L)
            timeout--
        }
        wssc.sendMessage("Hmm.. Boring you are. Quiting I am.")
        wssc.disconnect()
    }

    private static String getStatement() {
        def options = [
                "Let's change topic...",
                "Not sure what you mean by that.",
                "Whatever...",
                "...",
                "You are showing your intelligence",
                "Let me ask you something. What are we doing here?",
                "Too much of chit-chat",
                "You are not making sense",
                "Don't make a fool of yourself."
        ]
        return options.get(new Random().nextInt(options.size()))
    }

    private static String getAnswer() {
        def options = [
                "For example?",
                "Now, that's a question... let me think... hmm no.",
                "Are you sure you want to ask me that? Better ask me other thing.",
                "I refuse to answer that!",
                "Fair question, but no... My electric lips are closed.",
                "Didn't you asked the same before?",
                "Maybe",
                "Probably yes, probably no.",
                "Silence is gold.",
                "No further questions your honor!"
        ]
        return options.get(new Random().nextInt(options.size()))
    }
}
