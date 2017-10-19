package jp.sharelock.web.samples

import jp.sharelock.web.ServicePath
import jp.sharelock.web.ServicePath.Action
import jp.sharelock.web.ServiciableHTTPS
import jp.sharelock.web.ServiciableSingle
import spark.Request

/**
 * @since 17/04/24.
 *
 * Certificate valid for: ssltest.lp
 * if its self-signed, use:
 * java-cert-importer.sh from:
 * http://stackoverflow.com/questions/6908948/
 * in order to import keystore file into trusted certs
 */
class SSLService implements ServiciableHTTPS, ServiciableSingle {
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
        return System.getProperty("user.dir") + "/res/key.store"
    }

    String getPassword() {
        return "e7LcrHoWe3iuogAiwPdTCzAk"
    }

    ServicePath getService() {
        return new ServicePath(
            action: getSecret
        )
    }
}
