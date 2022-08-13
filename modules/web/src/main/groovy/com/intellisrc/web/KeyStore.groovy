package com.intellisrc.web

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

/**
 * Used for HTTPS
 *
 * You can generate a keystore (self-signed) with this command:
 *
 *  keytool -genkey -keyalg RSA -alias localhost -keystore keystore.jks -storepass yourpasswordhere -validity 365 -keysize 2048
 *
 * @since 2022/08/05.
 */
@CompileStatic
class KeyStore {
    File file
    char[] password

    KeyStore(File file, char[] pass) {
        this.file = file
        password = pass
    }
    KeyStore(File file, String pass) {
        this(file, pass.toCharArray())
    }

    boolean isValid() {
        boolean valid = false
        if(file) {
            if(file.exists()) {
                valid = password.length > 0
                if(!valid) {
                    Log.w("Key Store: password was not provided")
                }
            } else {
                Log.w("Key Store: %s doesn't exists. ")
            }
        } else {
            Log.w("Key Store: file was null. ")
        }
        return valid
    }
}
