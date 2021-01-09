package com.intellisrc.net

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import groovy.transform.CompileStatic

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.FileDataSource
import javax.mail.Address
import javax.mail.Header
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * References:
 *  https://blog.jedox.com/send-email-using-javamail-groovy-script/
 *  https://stackoverflow.com/questions/16117365/
 *  https://stackoverflow.com/questions/6756162/
 *
 * @since 17/10/24.
 */
@CompileStatic
class Smtp {
    public String username     = ""
    public String password     = ""
    public String host         = "localhost"
    public String from         = ""
    public String fromName     = ""
    public String defaultTo    = ""
    public String replyTo      = ""
    public boolean startTLS    = false
    public boolean simulate    = false //If true, it won't send any email
    public int port            = 25
    public Map<String,String> headers = [:] //custom headers

    static enum Mode {
        TO, CC, BCC
        Message.RecipientType toRecipientType() {
            switch (this) {
                case CC: return Message.RecipientType.CC
                case BCC: return Message.RecipientType.BCC
                case TO:
                default: return Message.RecipientType.TO
            }
        }
    }

    private List<File> attachments = []
    private static enum TransportType {
        SMTP, SMTPS
    }
    private TransportType transportType = TransportType.SMTP

    Smtp(String cfgKey = "mail.smtp") {
        Map<String,String> settings = [
            simulate : "simulate",
            username : "user",
            fromName : "name",
            password : "password",
            host     : "host",
            port     : "port",
            defaultTo: "to",
            from     : "from",
            replyTo  : "reply"
        ]
        // Load first defaults if found
        String defaultKey = "mail.smtp"
        boolean fileSettingsDefault = Config.matchKey(defaultKey)
        boolean fileSettings = defaultKey != cfgKey && Config.matchKey(cfgKey)
        settings.each {
            String key, String rule ->
                switch (rule) {
                    case "simulate":
                        if(fileSettings && Config.hasKey("${cfgKey}.$rule")) {
                            this.setProperty(key, Config.getBool("${cfgKey}.$rule"))
                        } else if(fileSettingsDefault && Config.hasKey("${defaultKey}.$rule")) {
                            this.setProperty(key, Config.getBool("${defaultKey}.$rule"))
                        }
                        break
                    case "port":
                        if(fileSettings && Config.hasKey("${cfgKey}.$rule")) {
                            this.setProperty(key, Config.getInt("${cfgKey}.$rule"))
                        } else if(fileSettingsDefault && Config.hasKey("${defaultKey}.$rule")) {
                            this.setProperty(key, Config.getInt("${defaultKey}.$rule"))
                        }
                        break
                    default:
                        if(fileSettings && Config.hasKey("${cfgKey}.$rule")) {
                            this.setProperty(key, Config.get("${cfgKey}.$rule"))
                        } else if(fileSettingsDefault && Config.hasKey("${defaultKey}.$rule")) {
                            this.setProperty(key, Config.get("${defaultKey}.$rule"))
                        }
                }
        }
    }

    /**
     * Adds an attachment
     * @param filename
     * @return
     */
    boolean addAttachment(String filename) {
        def file = new File(filename)
        return addAttachment(file)
    }
    /**
     * Adds an attachment using a File object
     * @param file
     * @return
     */
    boolean addAttachment(File file) {
        boolean exists = false
        if(file.exists()) {
            attachments << file
            exists = true
        } else {
            Log.e("Attachment: " + file.toString() + " was not found.")
        }
        return exists
    }

    /**
     * Clear all attachments
     */
    void clearAttachments() {
        attachments.clear()
    }

    /**
     * Sends an email to the default recipient
     * @param subject
     * @param body
     * @return
     */
    boolean sendDefault(String subject = "", String body = "", String bodyText = "") {
        send([:], subject, body, bodyText)
    }
    /**
     * Sends an email to a single recipient
     * @param to
     * @param subject
     * @param body
     * @return
     */
    boolean send(Email to, String subject = "", String body = "", String bodyText = "") {
        Map<String,Mode> map = [:]
        map[to.toString()] = Mode.TO
        send(map, subject, body, bodyText)
    }
    /**
     * Sends an email to a single recipient
     * @param to
     * @param subject
     * @param body
     * @return
     */
    boolean send(String to, String subject = "", String body = "", String bodyText = "") {
        Map<String,Mode> map = [:]
        map[to] = Mode.TO
        send(map, subject, body, bodyText)
    }

    /**
     * Sends an email to two or more recipients all using TO:
     * @param tos
     * @param subject
     * @param body
     * @return
     */
    boolean send(List<String> tos, String subject = "", String body = "", String bodyText = "") {
        Map<String,Mode> map = [:]
        tos.each {
            map[it] = Mode.TO
        }
        send(map, subject, body, bodyText)
    }

    /**
     * Sends an email to one recipient
     * @param to
     * @param subject
     * @param body
     * @param bodyText : TXT version of message
     * @return
     */
    boolean send(Map<String, Mode> recipients, String subject = "", String body = "", String bodyText = "") {
        Email emailFrom
        //If config is set, send a copy to those
        if(defaultTo) {
            if (defaultTo.contains(',')) {
                defaultTo.split(',').each {
                    String to ->
                        recipients[to] = recipients.isEmpty() ? Mode.TO : Mode.BCC
                }
            } else {
                recipients[defaultTo] = recipients.isEmpty() ? Mode.TO : Mode.BCC
            }
        }
        //Setup javamail
        if (username) {
            Config.system.set("mail.smtp.auth", "true")
            Config.system.set("mail.smtp.user", username)
            Config.system.set("mail.smtp.password", password)
        }
        Config.system.set("mail.smtp.host", host)
        Config.system.set("mail.smtp.port", port.toString())
        if (port == 465) {
            Config.system.set("mail.smtp.starttls.enable", "true")
            Config.system.set("mail.smtp.EnableSSL.enable", "true")
            Config.system.set("mail.smtp.ssl.trust", host)
        } else if (startTLS) {
            Config.system.set("mail.smtp.starttls.enable", "true")
        }
        if(port == 587 || port == 465) {
            transportType = TransportType.SMTPS
        }
        if(recipients.isEmpty()) {
            Log.e("No recipients specified. Neither default or explicit set.")
            return false
        }
        try {
            emailFrom = new Email(from)
        } catch(Email.EmailMalformedException e) {
            Log.e("Sender email is not correct: $from", e)
            return false
        }
        Session session = Session.getDefaultInstance(Config.system.properties as Properties)
        MimeMessage message = new MimeMessage(session)
        try {
            if(fromName) {
                message.setFrom(new InternetAddress(emailFrom.toString(), fromName))
            } else {
                message.setFrom(new InternetAddress(emailFrom.toString()))
            }
        } catch (MessagingException e) {
            Log.e("Failed to set FROM: $from, error was: ", e)
            return false
        }
        if(replyTo) {
            message.setReplyTo([ new InternetAddress(replyTo) ] as Address[])
        }
        recipients.each {
            String to, Mode mode ->
                Email emailTo
                try {
                    emailTo = new Email(to)
                } catch (Email.EmailMalformedException e) {
                    Log.e("Receiver email is not correct: $to", e)
                    return false
                }
                try {
                    message.addRecipient(mode.toRecipientType(), new InternetAddress(emailTo.toString()))
                } catch (MessagingException e) {
                    Log.e("Failed to set TO: $to, error was: ", e)
                    return false
                }
        }
        try {
            message.setSubject(subject)
        } catch (MessagingException e) {
            Log.e("Failed to set mail subject: ["+subject+"], error was: ", e)
            return false
        }
        //If we have attachments or we defined bodyText (which means there body is HTML)
        //Alternative + Attachments : code using as reference: https://mlyly.wordpress.com/2011/05/13/hello-world/
        if(attachments || bodyText) {
                def wrapBodyPart = new MimeBodyPart()
                if(bodyText) {
                    def altPart = new MimeMultipart("alternative")
                    try {
                        if (bodyText) {
                            def textBodyPart = new MimeBodyPart()
                            def htmlBodyPart = new MimeBodyPart()
                            textBodyPart.setContent(bodyText, "text/plain; charset=UTF-8")
                            htmlBodyPart.setContent(body, "text/html; charset=UTF-8")
                            altPart.addBodyPart(textBodyPart)
                            altPart.addBodyPart(htmlBodyPart)
                        } else {
                            def messageBodyPart = new MimeBodyPart()
                            messageBodyPart.setText(body, "UTF-8")
                            altPart.addBodyPart(messageBodyPart)
                        }
                        wrapBodyPart.setContent(altPart)
                    } catch (MessagingException e) {
                        Log.e("Unable to set the body in multipart. Error was: ", e)
                        return false
                    }
                }

                def relatedPart = new MimeMultipart("related")
                message.setContent(relatedPart)
                relatedPart.addBodyPart(wrapBodyPart)

                if(attachments) {
                    attachments.each {
                        File file ->
                            try {
                                DataSource source = new FileDataSource(file)
                                def messageBodyPart = new MimeBodyPart(
                                        dataHandler: new DataHandler(source, "text/plain; charset=UTF-8"),
                                        fileName: file.name
                                )
                                relatedPart.addBodyPart(messageBodyPart)
                                Log.v("Attached: " + file.name)
                            } catch (MessagingException e) {
                                Log.e("Attachment was not added: $file, error was: ", e)
                                return false
                            }
                    }

                }
        } else {
            try {
                message.setText(body, "UTF-8")
            } catch (MessagingException e) {
                Log.e("Unable to set text to body: "+body.substring(0,10)+"... , error was: ", e)
                return false
            }
        }
        // Add headers
        message.setSentDate(SysClock.dateTime.toDate())
        try {
            if(!headers.isEmpty()) {
                headers.each {
                    String head, String value ->
                        message.addHeader(head, value)
                }
            }
        } catch (MessagingException e) {
            Log.e("Unable to set headers.", e)
        }
        Log.i("Email from: $from -> "+ ( recipients.keySet().first() ) + "... (recipients: "+recipients.size()+")" +" is sending...")
        try {
            if(simulate) {
                Log.w("SMTP Simulation mode is ON. No messages are sent.")
                message.getAllRecipients().each {
                    Address address ->
                    Log.v("    Recipient -> "+address.toString())
                }
                Log.v("##### HEADERS #####")
                message.getAllHeaders().each {
                    Header h ->
                        Log.v(h.name+" : "+h.value)
                }
                Log.v("###################")
            } else {
                Transport transport = session.getTransport(transportType.toString().toLowerCase())
                transport.connect(host, username, password)
                transport.sendMessage(message, message.getAllRecipients())
                transport.close()
            }
        } catch(MessagingException e) {
            Log.e("Mail was not sent. Error was: ", e)
            Exception ne
            String last = e.message
            while ((ne = e.nextException) && (last != ne.message)) {
                Log.e("... ", ne)
                last = ne.message
            }
            return false
        }
        return true
    }
}
