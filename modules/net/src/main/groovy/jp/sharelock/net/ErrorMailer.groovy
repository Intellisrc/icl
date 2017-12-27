package jp.sharelock.net

import jp.sharelock.etc.Log
import jp.sharelock.etc.Log.Level
import jp.sharelock.etc.Version
import groovy.transform.CompileStatic

/**
 * Send error and security alerts to administrator
 * @since 17/12/27.
 */
@CompileStatic
class ErrorMailer {
    static Level minLevel = Level.ERROR
    /**
     * Override the next strings in case
     * it is displaying incorrect information
     */
    static String sysVersion = ""
    static String hostName = ""
    static String ipAddress = ""
    /**
     * Set ErrorMailer
     * @param destiny
     */
    static void set(Email destiny = null) {
        Log.onLog = {
            Level level, String message, Log.Info stack ->
                if(level >= minLevel) {
                    def body  = "Date:" + new Date().toYMDHms() +
                                "Message: $message" +
                                "At: " + stack.fileName + ":" + stack.lineNumber
                                "Class: " + stack.className +
                                "Method: " + stack.methodName +
                                "Host:" + hostName ?: InetAddress.getLocalHost().getHostName() +
                                "IP:" + ipAddress ?: InetAddress.getLocalHost().getHostAddress() +
                                "Version:" + sysVersion ?: Version.get()
                    def mailer = new Smtp()
                    if(!destiny) {
                        destiny = mailer.from ? new Email(mailer.from) : new Email("root@localhost")
                    }
                    mailer.send(destiny.toString(), "[" + level.name() + "] $message", body)
                }
        }
    }
}
