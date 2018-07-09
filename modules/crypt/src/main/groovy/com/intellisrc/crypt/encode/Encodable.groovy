package com.intellisrc.crypt.encode

/**
 * @since 17/04/07.
 */
@groovy.transform.CompileStatic
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