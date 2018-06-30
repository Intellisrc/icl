package com.intellisrc.web

@groovy.transform.CompileStatic
/**
 * Enable HTTPS on service
 * Requires you to have a keystore file, which you can generate using the Java keytool:
 *
 *
 *
 *
 * See: http://sparkjava.com/documentation.html#enable-ssl
 * @since 17/04/19.
 */
interface ServiciableHTTPS extends Serviciable {
    String getKeyStoreFile()
    transient String getPassword()
}
