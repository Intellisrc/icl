package com.intellisrc.crypt.encode

import groovy.transform.CompileStatic

/**
 * @since 17/04/07.
 */
@CompileStatic
trait Encodable {
    /**
     * Error thrown while encoding data
     */
    static class EncodingException extends Exception {}
    /**
     * Error thrown while decoding data
     */
    static class DecodingException extends Exception {}
    abstract byte[] encrypt(byte[] original) throws EncodingException
    abstract byte[] decrypt(byte[] encoded) throws DecodingException
}