package com.intellisrc.web.samples

import com.intellisrc.web.Service
import com.intellisrc.web.Service.Action
import com.intellisrc.web.ServiciableHTTPS
import com.intellisrc.web.SingleService
import spark.Request

/**
 * Example of HTTPS Service
 * @since 17/04/24.
 *
 * 1. Issue a certificate (self-signed or not)
 * 2. Execute: java-cert-importer.sh (inside res/public/)
 * in order to import keystore file into trusted certs
 */
class SSLService extends SingleService implements ServiciableHTTPS {
    private static final String SUPER_SECRET = "9EEyYMvmqTqppiNVjRxgWcgViMoKbMPcLutbWJEqvN93uNbkuxCV7tCLVxzVpPkUhUWJYzu"
    Action getSecret = {
        Request request ->
            return SUPER_SECRET
    } as Action

    String getPath() {
        return "/admin"
    }

    String getKeyStoreFile() {
        //Must be absolute path. Do not store it in public directory
        return File.get(File.userDir, "res", "key.store")
    }

    String getPassword() {
        return "e7LcrHoWe3iuogAiwPdTCzAk"
    }

    Service getService() {
        return new Service(
            action: getSecret
        )
    }
}
