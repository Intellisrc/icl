package com.intellisrc.net

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.etc.Mime
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import jakarta.activation.DataSource
import jakarta.mail.*
import jakarta.mail.event.TransportEvent
import jakarta.mail.event.TransportListener
import jakarta.mail.internet.*

import java.nio.charset.Charset

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
    public String mailer       = Config.any.get("mail.smtp.mailer","Groovy-ICL-Library")
    public boolean startTLS    = false
    public boolean simulate    = false //If true, it won't send any email
    public int port            = 25
    public Map<String,String> headers = [:] //custom headers
    public EventResult onDelivered = { TransportEvent e, boolean partial -> }
    public EventResult onNotDelivered = { TransportEvent e, boolean partial -> }
    public ConnectionFailure onConnectionFailed = {}

    static enum Mode {
        TO, CC, BCC
        Message.RecipientType toRecipientType() {
            switch (this) {
                case CC: return Message.RecipientType.CC
                case BCC: return Message.RecipientType.BCC
                //case TO:
                default: return Message.RecipientType.TO
            }
        }
    }
    /**
     * Interface used to call events on connection failure
     */
    static interface ConnectionFailure {
        void call()
    }

    /**
     * Interface used to call events on delivered/not-delivered
     */
    static interface EventResult {
        void call(TransportEvent e, boolean partial)
    }

    /**
     * Listener for events when messages are sent
     */
    @Canonical
    static class SendListener implements TransportListener {
        @SuppressWarnings('GrFinalVariableAccess')
        final EventResult success
        @SuppressWarnings('GrFinalVariableAccess')
        final EventResult failure

        @Override
        void messageDelivered(TransportEvent e) {
            success.call(e, false)
        }

        @Override
        void messageNotDelivered(TransportEvent e) {
            failure.call(e, false)
        }

        @Override
        void messagePartiallyDelivered(TransportEvent e) {
            success.call(e, true)
            failure.call(e, true)
        }
    }
    /**
     * Class to be used when attachments are not File objects
     */
    static class InputStreamDataSource implements DataSource {
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream()
        private final String name
        private final mimeType

        InputStreamDataSource(InputStream inputStream, String name, String mimeType = "") {
            this.name = name
            this.mimeType = mimeType
            try {
                int nRead
                byte[] data = new byte[32 * 1024]
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead)
                }
                inputStream.close()
                buffer.flush()
            } catch (IOException e) {
                e.printStackTrace()
            }
        }

        @Override
        String getContentType() {
            return mimeType ?: Mime.getType(name)
        }

        @Override
        InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(buffer.toByteArray())
        }

        @Override
        String getName() {
            return name
        }

        @Override
        OutputStream getOutputStream() throws IOException {
            throw new IOException("Read-only data")
        }

        byte[] getBytes() {
            return buffer.toByteArray()
        }
    }

    private List<File> attachments = []
    private List<InputStreamDataSource> attachmentsDS = []
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
                        if(fileSettings && Config.exists("${cfgKey}.$rule")) {
                            this.setProperty(key, Config.any.getBool("${cfgKey}.$rule"))
                        } else if(fileSettingsDefault && Config.exists("${defaultKey}.$rule")) {
                            this.setProperty(key, Config.any.getBool("${defaultKey}.$rule"))
                        }
                        break
                    case "port":
                        if(fileSettings && Config.exists("${cfgKey}.$rule")) {
                            this.setProperty(key, Config.any.getInt("${cfgKey}.$rule"))
                        } else if(fileSettingsDefault && Config.exists("${defaultKey}.$rule")) {
                            this.setProperty(key, Config.any.getInt("${defaultKey}.$rule"))
                        }
                        break
                    default:
                        if(fileSettings && Config.exists("${cfgKey}.$rule")) {
                            this.setProperty(key, Config.any.get("${cfgKey}.$rule"))
                        } else if(fileSettingsDefault && Config.exists("${defaultKey}.$rule")) {
                            this.setProperty(key, Config.any.get("${defaultKey}.$rule"))
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
        def file = File.get(filename)
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
     * Adds an attachment using an InputStream
     * @param source
     * @return
     */
    boolean addAttachment(InputStreamDataSource source) {
        attachmentsDS << source
        return true
    }

    /**
     * Clear all attachments
     */
    void clearAttachments() {
        attachments.clear()
        attachmentsDS.clear()
    }
    /**
     * Get number of attachments
     * @return
     */
    int getAttachments() {
        return attachments.size()
    }
    /**
     * Get number of data sources attached
     * @return
     */
    int getAttachDataSources() {
        return attachmentsDS.size()
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
     * it can be a List<String> or List<Email>
     * @param tos
     * @param subject
     * @param body
     * @return
     */
    boolean send(Collection tos, String subject = "", String body = "", String bodyText = "") {
        Map<String,Mode> map = [:]
        tos.each {
            map[it.toString()] = Mode.TO
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
        Session session = Session.getDefaultInstance(System.properties)
        MimeMessage message = new MimeMessage(session)
        message.setHeader("X-Mailer", encode(mailer))
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
        //noinspection GroovyMissingReturnStatement
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
                    Log.w("Failed to set TO: $to, error was: ", e)
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
        if(!attachments.empty || !attachmentsDS.empty || bodyText) {
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

                def relatedPart = new MimeMultipart("mixed")
                message.setContent(relatedPart)
                relatedPart.addBodyPart(wrapBodyPart)

                if(! attachments.empty) {
                    attachments.each {
                        File file ->
                            try {
                                MimeBodyPart attachBodyPart = new MimeBodyPart()
                                attachBodyPart.attachFile(file)
                                attachBodyPart.setHeader("Content-Type", Mime.getType(file) + '; name="' + encode(file.name) + '"')
                                attachBodyPart.setHeader("Content-Disposition", 'attachment; filename="' + encode(file.name) + '"')
                                relatedPart.addBodyPart(attachBodyPart)
                                Log.v("Attached: " + file.name)
                            } catch (MessagingException e) {
                                Log.e("Attachment was not added: %s, error was: ", file.name, e)
                                return false
                            }
                    }
                }
                if(! attachmentsDS.empty) {
                    attachmentsDS.each {
                        InputStreamDataSource source ->
                            try {
                                def messageBodyPart = new MimeBodyPart(
                                    fileName: source.name,
                                    disposition: MimeBodyPart.ATTACHMENT,
                                )
                                messageBodyPart.setContent(source.bytes, source.contentType)
                                relatedPart.addBodyPart(messageBodyPart)
                                Log.v("Attached: " + source.name)
                            } catch (MessagingException e) {
                                Log.e("Attachment was not added: %s, error was: ", source.name, e)
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
                    Log.v("Recipient -> "+address.toString())
                }
                Log.v("##### HEADERS #####")
                message.getAllHeaders().each {
                    Header h ->
                        Log.v(h.name+" : "+h.value)
                }
                Log.v("###################")
                Log.v(bodyText)
                Log.v("###################")
            } else {
                Transport transport = session.getTransport(transportType.toString().toLowerCase())
                try {
                    transport.connect(host, username, password)
                } catch (MessagingException e) {
                    Log.w("Unable to connect to server", e)
                    onConnectionFailed.call()
                    return false
                }
                transport.addTransportListener(new SendListener(onDelivered, onNotDelivered))
                try {
                    transport.sendMessage(message, message.getAllRecipients())
                } catch (MessagingException me) {
                    Log.w("Unable to send message", me)
                }
                try {
                    transport.close()
                } catch (MessagingException me) {
                    Log.w("Unable to close connection", me)
                }
            }
        } catch(Exception e) {
            Log.e("Mail was not sent. Error was: ", e)
            return false
        }
        return true
    }
    /**
     * Encode string if needed
     * @param str
     * @return
     */
    static String encode(String str) {
        return Charset.forName("US-ASCII").newEncoder().canEncode(str) ? str : MimeUtility.encodeText(str)
    }
}
