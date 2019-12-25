package com.intellisrc.net

import com.intellisrc.core.Log
import com.intellisrc.core.Log.Level
import com.intellisrc.core.SysClock
import com.intellisrc.core.Version
import groovy.transform.CompileStatic

/**
 * Send error and security alerts to administrator
 * @since 17/12/27.
 */
@CompileStatic
class ErrorMailer {
    static Level minLevel = Level.SECURITY
    /**
     * Override the next strings in case
     * it is displaying incorrect information
     */
    static String sysVersion = ""
    static String hostName = ""
    static String ipAddress = ""
    /**
     * Prevent sending over and over the same error
     */
    static private final List<Log.Info> reported = []
    /**
     * Set ErrorMailer
     * @param destiny
     */
    static void set(final String destiny = null) {
        Log.onLog = {
            Level level, String message, Log.Info info ->
                // We don't send any issue with the Smtp class as it may loop forever
                if(level >= minLevel &&! reported.find{ it.className == info.className && it.lineNumber == it.lineNumber }) {
                    def body  = "Date:" + SysClock.dateTime.YMDHms + "\n" +
                                "Message: $message" + "\n" +
                                "At: " + info.fileName + ":" + info.lineNumber + "\n" +
                                "Class: " + info.className + "\n" +
                                "Method: " + info.methodName + "\n" +
                                "Version:" + (sysVersion ?: Version.get()) + "\n" +
                                "Host:" + (hostName ?: InetAddress.getLocalHost().getHostName()) + "\n" +
                                "IP:" + (ipAddress ?: InetAddress.getLocalHost().getHostAddress()) + "\n"
                    def mailer = new Smtp()
                    if(destiny) {
                        try {
                            mailer.defaultTo = new Email(destiny).toString() //Verify value
                        } catch (Exception e) {
                            Log.e("Email is not correct. Please fix it.", e)
                        }
                    }
                    reported << info
                    if(mailer.sendDefault("[" + level.name() + "] $message", body)) {
                        Log.v("Error report sent.")
                    }
                }
        }
    }
}
