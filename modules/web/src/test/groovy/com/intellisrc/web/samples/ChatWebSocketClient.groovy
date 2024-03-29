package com.intellisrc.web.samples


import com.intellisrc.etc.JSON
import com.intellisrc.web.WebSocketServiceClient
import com.intellisrc.web.WebSocketServiceClient.Callable

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
class ChatWebSocketClient {
    final int QUIT_TIMEOUT = 600
    final int MAX_REPEAT = 30 //Prevent repeating itself
    final String uname
    final WebSocketServiceClient wssc
    def last_responses = []

    Callable handler = {
        Map msg ->
            String message = msg.message.toString()
            if(msg.type == "txt" && msg.user != uname) {
                String botsay = ""
                if(new Random().nextInt(5) < 2) {
                    if (message.toLowerCase().contains("hi") || message.toLowerCase().contains("hello")) {
                        botsay = "Do you really think I care about you?"
                    } else if (message.contains(":)") || message.contains(":D")) {
                        botsay = "Please don't smile here, nobody is watching!"
                    } else if (message.contains(":(")) {
                        botsay = "Poor little thing!"
                    } else if (message.contains(":|")) {
                        botsay = "No comments..."
                    } else if (message.contains(":/")) {
                        botsay = "So?"
                    } else if (message.contains(":P")) {
                        botsay = "Your tongue is dirty, can't you see?"
                    }
                }
                if(botsay.isEmpty() || last_responses.contains(botsay)) {
                    String toSend = URLEncoder.encode(message.toString(), "UTF-8")
                    def response = JSON.decode("http://api.program-o.com/v2/chatbot/?bot_id=12&say=$toSend&convo_id=robocup_9999&format=json".toURL().text) as Map
                    botsay = response.botsay
                }
                if (botsay.isEmpty() || last_responses.contains(botsay)) {
                    if(message.toString().contains("?")) {
                        botsay = getAnswer()
                    } else {
                        botsay = getStatement()
                    }
                    //Last resource to prevent repetition:
                    if(last_responses.contains(botsay)) {
                        def response = JSON.decode("http://quotesondesign.com/wp-json/posts?filter[orderby]=rand&filter[posts_per_page]=1".toURL().text) as List
                        Map res = (Map) response.first()
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
            }
    }

    ChatWebSocketClient(int port, String userName) {
        this.uname = userName
        wssc = new WebSocketServiceClient(
            hostname: "localhost",
            port : port,
            path : "/ws/chat?user=$uname"
        )
    }

    boolean connect() {
        boolean connected = true
        wssc.connect(handler, {
            Map it -> connected = false
        })
        return connected
    }

    boolean sendLoginMessage() {
        wssc.sendMessage("Yoo Humans!")
        return true
    }

    boolean sendLogoutMessage() {
        wssc.sendMessage("Hmm.. Boring you are. Quiting I am.")
        return true
    }

    boolean disconnect() {
        wssc.disconnect()
        return true
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
