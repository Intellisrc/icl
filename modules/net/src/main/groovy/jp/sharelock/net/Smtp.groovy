package jp.sharelock.net

import jp.sharelock.etc.Config
import jp.sharelock.etc.Log
import groovy.transform.CompileStatic

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.FileDataSource
import javax.mail.Address
import javax.mail.Header
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
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
    String username     = ""
    String password     = ""
    String host         = "localhost"
    String from         = ""
    String fromName     = ""
    String defaultTo    = ""
    String replyTo      = ""
    boolean startTLS    = false
    boolean simulate    = false //If true, it won't send any email
    int port            = 25
    Map<String,String> headers = [:] //custom headers

    enum Mode {
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

    private boolean useLocalSettings = false //Force to use settings specified here over configuration file or system settings
    private List<File> attachments = []
    private enum TransportType {
        SMTP, SMTPS
    }
    private TransportType transportType = TransportType.SMTP

    /**
     * Autodetect when to override global settings
     * @param sFrom
     */
    void setHost(String sHost) {
        host = sHost
        useLocalSettings = true
    }
    void setUsername(String sUserName) {
        username = sUserName
        useLocalSettings = true
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
    boolean send(String to, String subject = "", String body = "", String bodyText = "") {
        def map = [:]
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
        def map = [:]
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
        Properties props = new Properties()
        boolean fileSettings = Config.matchKey("mail.smtp")
        //Reading from file...
        if(!useLocalSettings && fileSettings) {
            props = Config.props
            simulate = Config.getBool("mail.smtp.simulate")
            //Variables needed for connection
            username = Config.get("mail.smtp.user")
            password = Config.get("mail.smtp.password")
            host     = Config.get("mail.smtp.host")
            port     = Config.getInt("mail.smtp.port")
        //Reading from variables
        } else {
            if(fileSettings) {
                Log.d("Config file settings were override with local settings.")
            }
        }
        if(from.isEmpty() && Config.hasKey("mail.smtp.from") && Config.get("mail.smtp.from")) {
            from = Config.get("mail.smtp.from")
        }
        if(fromName.isEmpty() && Config.hasKey("mail.smtp.name") && Config.get("mail.smtp.name")) {
            fromName = Config.get("mail.smtp.name")
        }
        if(replyTo.isEmpty() && Config.hasKey("mail.smtp.reply") && Config.get("mail.smtp.reply")) {
            replyTo = Config.get("mail.smtp.reply")
        }
        //If config is set, send a copy to those
        if (defaultTo) {
            recipients[defaultTo] = Mode.BCC
        } else if(Config.hasKey("mail.smtp.to") && Config.get("mail.smtp.to")) {
            Config.get("mail.smtp.to").split(",").each {
                String to ->
                    recipients[to] = Mode.BCC
            }
        }
        //Setup javamail
        if (username) {
            props.setProperty("mail.smtp.auth", "true")
            props.setProperty("mail.smtp.user", username)
            props.setProperty("mail.smtp.password", password)
        }
        props.setProperty("mail.smtp.host", host)
        props.setProperty("mail.smtp.port", port.toString())
        if (port == 465) {
            props.setProperty("mail.smtp.starttls.enable", "true")
            props.setProperty("mail.smtp.EnableSSL.enable", "true")
            props.setProperty("mail.smtp.ssl.trust", host)
        } else if (startTLS) {
            props.setProperty("mail.smtp.starttls.enable", "true")
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
            Log.e("Sender email is not correct: $from")
            return false
        }
        Session session = Session.getDefaultInstance(props)
        Message message = new MimeMessage(session)
        try {
            if(fromName) {
                message.setFrom(new InternetAddress(emailFrom.toString(), fromName))
            } else {
                message.setFrom(new InternetAddress(emailFrom.toString()))
            }
        } catch (MessagingException e) {
            Log.e("Failed to set FROM: $from, error was: "+e.message)
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
                    Log.e("Receiver email is not correct: $to")
                    return false
                }
                try {
                    message.addRecipient(mode.toRecipientType(), new InternetAddress(emailTo.toString()))
                } catch (MessagingException e) {
                    Log.e("Failed to set TO: $to, error was: "+e.message)
                    return false
                }
        }
        try {
            message.setSubject(subject)
        } catch (MessagingException e) {
            Log.e("Failed to set mail subject: ["+subject+"], error was: "+e.message)
            return false
        }
        //If we have attachments or we defined bodyText (which means there body is HTML)
        if(attachments || bodyText) {
                Multipart multipart = new MimeMultipart()
                try {
                    if(bodyText) {
                        MimeBodyPart textBodyPart = new MimeBodyPart()
                        MimeBodyPart htmlBodyPart = new MimeBodyPart()
                        htmlBodyPart.setContent(body, "text/html; charset=UTF-8")
                        textBodyPart.setText(bodyText,"UTF-8")
                        multipart.addBodyPart(htmlBodyPart)
                        multipart.addBodyPart(textBodyPart)
                    } else {
                        MimeBodyPart messageBodyPart = new MimeBodyPart()
                        messageBodyPart.setText(body,"UTF-8")
                        multipart.addBodyPart(messageBodyPart)
                    }
                } catch (MessagingException e) {
                    Log.e("Unable to set the body in multipart. Error was: "+e.message)
                    return false
                }
                attachments.each {
                    File file ->
                        try {
                            MimeBodyPart messageBodyPart = new MimeBodyPart()
                            DataSource source = new FileDataSource(file)
                            messageBodyPart.setDataHandler(new DataHandler(source))
                            messageBodyPart.setFileName(file.name)
                            multipart.addBodyPart(messageBodyPart)
                            Log.d("Attached: "+file.name)
                        } catch(MessagingException e) {
                            Log.e("Attachment was not added: $file, error was: "+e.message)
                            return false
                        }
                }
                try {
                    message.setContent(multipart)
                } catch (MessagingException e) {
                    Log.e("Unable to set multipart content. Error was: "+e.message)
                    return false
                }
        } else {
            try {
                message.setText(body)
            } catch (MessagingException e) {
                Log.e("Unable to set text to body: "+body.substring(0,10)+"... , error was: "+e.message)
                return false
            }
        }
        if(headers) {
            try {
                headers.each {
                    String head, String value ->
                        message.addHeader(head, value)
                }
            } catch (MessagingException e) {
                Log.e("Unable to set headers.")
            }
        }
        Log.d("Email from: $from -> "+ ( recipients.keySet().first() ) + "... (recipients: "+recipients.size()+")" +" is sending...")
        try {
            if(simulate) {
                Log.w("SMTP Simulation mode is ON. No messages are sent.")
                message.getAllRecipients().each {
                    Address address ->
                    Log.d("    Recipient -> "+address.toString())
                }
                Log.d("##### HEADERS #####")
                message.getAllHeaders().each {
                    Header h ->
                        Log.d(h.name+" : "+h.value)
                }
                Log.d("###################")
            } else {
                Transport transport = session.getTransport(transportType.toString().toLowerCase())
                transport.connect(host, username, password)
                transport.sendMessage(message, message.getAllRecipients())
                transport.close()
            }
        } catch(MessagingException e) {
            Log.e("Mail was not sent. Error was: "+e.message)
            if(e.nextException) {
                Log.e("... " + e.nextException)
            }
            return false
        }
        return true
    }
}
