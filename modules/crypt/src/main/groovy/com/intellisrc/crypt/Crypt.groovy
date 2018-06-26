package com.intellisrc.crypt

import com.intellisrc.etc.Bytes
import com.intellisrc.etc.Log

import java.security.SecureRandom

/**
 * @since 17/04/07.
 *
 * NOTE:
     According to: http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords/
     using char[] instead of String increases security as String is an immutable object and it may reside
     in memory until the GC clear it as it cannot be changed (reset).
     However, if we are using other libraries (like a web server, etc), it is very likely they will be using
     String instead of char[] making the protection useless, which is supported by:
     http://stackoverflow.com/questions/15016250/in-java-how-do-i-extract-a-password-from-a-httpservletrequest-header-without-ge

     You can use etc.Bytes.* to convert from/to char[], byte[] and String
 *
 */
@groovy.transform.CompileStatic
class Crypt {
    protected static SecureRandom random = new SecureRandom()

    static enum Complexity {
        LOW, MEDIUM, HIGH
    }

    protected transient byte[] key
    /**
     * Generates a random ascii string
     * @param len : length of string
     * @param extended : use extended
     * @return
     */
    static byte[] randomChars(int len, Complexity type = Complexity.LOW) {
        String AlphaUP  = ('A'..'Z').join('')
        String AlphaLW  = ('a'..'z').join('')
        String Num      = ('0'..'9').join('')
        String Safe     = "@.,:;\$=-+_"
        String Ext      = "'\"<>\\[]{}|^%`?!~#&()/*"
        String chars    = ""
        switch(type) {
            case Complexity.HIGH:
                chars += Ext
                //No break on purpose
            case Complexity.MEDIUM:
                chars += Safe
                //No break on purpose
            case Complexity.LOW:
                chars += AlphaUP + AlphaLW + Num
                //No break on purpose
        }
        char[] charPool = chars.toCharArray()
        char[] randAlpha = (1..len).collect {
            charPool[random.nextInt(charPool.length)]
        }
        Arrays.fill(charPool, (char)0)
        return Bytes.fromChars(randAlpha)
    }
    /**
     * Generates a random alphanumeric string
     * @return
     */
    static byte[] randomBytes(int len) {
        byte[] bytes = new byte[len]
        random.nextBytes(bytes)
        return bytes
    }
    /**
     * Changes key
     * @param key
     */
    void key(byte[] ikey) {
        key = ikey
        Arrays.fill(ikey, (byte)0)
    }
    /**
     * Test if it has a key or not
     */
    boolean hasKey() {
        return key != null
    }
    /**
     * Generates a new key
     */
    void genKey(int len) {
        this.key = randomChars(len)
    }
    /**
     * If has no key, generates one
     */
    void genIfNoKey(int len) {
        if(!hasKey()) {
            genKey(len)
        } else if(key.length != len) {
            Log.w("Key length didn't match the requirement. Generating a new one.")
            genKey(len)
        }
    }
    /**
     * clears key
     */
    void clear() {
        Arrays.fill(key, (byte)0)
    }
}
