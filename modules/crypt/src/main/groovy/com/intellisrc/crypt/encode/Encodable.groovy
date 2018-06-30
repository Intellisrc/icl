package com.intellisrc.crypt.encode

/**
 * @since 17/04/07.
 */
@groovy.transform.CompileStatic
trait Encodable {
    abstract byte[] encrypt(byte[] original)
    abstract byte[] decrypt(byte[] encoded)
}