package jp.sharelock.crypt.hash

import jp.sharelock.crypt.Crypt
import jp.sharelock.etc.Bytes
import jp.sharelock.etc.Log

import java.security.MessageDigest

/**
 * @since 17/04/07.
 */
@groovy.transform.CompileStatic
class Hash extends Crypt implements Hashable {
    private static final String LOG_TAG = Hash.simpleName
    /**
     * Note: Using cost will reuse the bytes returned to rehash it again
     * This is different than: sha(sha(x)) because sha(x) returns String HEX
     * and not bytes (and thus restricting the characters availability).
     * In order to imitate this method in other languages something like
     * this must be done: sha(hex2bytes(sha(x)))
     */
    protected int cost = 1 //Iterations to use

    /**
     * Hash key and returns bytes
     * @return
     */
    byte[] asBytes(String algorithm) {
        assert cost > 0
        byte[] hash = key
        MessageDigest digest = MessageDigest.getInstance(algorithm)
        (1..cost).each {
            hash = digest.digest(hash)
        }
        return hash
    }
    /**
     * Hash with a specific algorithm.
     * If Security Extensions are used, it can use any of those algorithms,
     * for example: RIPEMD128, SHA3-512, TIGER, WHIRLPOOL, BLAKE2B-160, etc
     * @param algorithm
     * @return
     */
    String hash(String algorithm = "SHA") {
        return Bytes.toHex(asBytes(algorithm)).toUpperCase()
    }

    /**
     * Creates a sha1 from input
     * @return
     *
     */
    String SHA() {
        return hash("SHA")
    }
    static String SHA(byte[] key, int cost = 1) {
        return new Hash(key: key, cost: cost).SHA()
    }
    static String SHA(char[] key, int cost = 1) {
        return SHA(Bytes.fromChars(key), cost)
    }
    /**
     * Creates a md5 hash from input
     * @return
     */
    String MD5() {
        return hash("MD5")
    }
    static String MD5(byte[] key, int cost = 1) {
        return new Hash(key: key, cost: cost).MD5()
    }
    static String MD5(char[] key, int cost = 1) {
        return MD5(Bytes.fromChars(key), cost)
    }
    /**
     * Creates a sha224 from input
     * @return
     */
    String SHA224() {
        return hash("SHA-224")
    }
    static String SHA224(byte[] key, int cost = 1) {
        return new Hash(key: key, cost: cost).SHA224()
    }
    static String SHA224(char[] key, int cost = 1) {
        return SHA224(Bytes.fromChars(key), cost)
    }
    /**
     * Creates a sha256 from input
     * @return
     */
    String SHA256() {
        return hash("SHA-256")
    }
    static String SHA256(byte[] key, int cost = 1) {
        return new Hash(key: key, cost: cost).SHA256()
    }
    static String SHA256(char[] key, int cost = 1) {
        return SHA256(Bytes.fromChars(key), cost)
    }
    /**
     * Creates a sha384 from input
     * @return
     */
    String SHA384() {
        return hash("SHA-384")
    }
    static String SHA384(byte[] key, int cost = 1) {
        return new Hash(key: key, cost: cost).SHA384()
    }
    static String SHA384(char[] key, int cost = 1) {
        return SHA384(Bytes.fromChars(key), cost)
    }
    /**
     * Creates a sha512 from input
     * @return
     */
    String SHA512() {
        return hash("SHA-512")
    }
    static String SHA512(byte[] key, int cost = 1) {
        return new Hash(key: key, cost: cost).SHA512()
    }
    static String SHA512(char[] key, int cost = 1) {
        return SHA512(Bytes.fromChars(key), cost)
    }

    /**
     * Number of iterations to perform
     * @param costSet
     */
    void setCost(int costSet) {
        cost = costSet
    }

    /**
     * Verifies that the input hash correspond to the input value
     * It will guess the hash used depending on hash length
     * @param hash
     * @param input
     * @return
     */
    boolean verify(String hash, String algorithm = "") {
        if(algorithm == "") { //If its the same as the default
            switch(hash.length()) {
                case 32 : algorithm = "MD5"; break
                case 40 : algorithm = "SHA"; break
                case 56 : algorithm = "SHA-224"; break
                case 64 : algorithm = "SHA-256"; break
                case 96 : algorithm = "SHA-384"; break
                case 128: algorithm = "SHA-512"; break
                default:
                    Log.w(LOG_TAG, "Unable to guess hash type")
                    return false
            }
        }
        return this.hash(algorithm) == hash.toUpperCase()
    }
    /**
     * Verify bytes instead of HEX hash
     * @param bytes
     * @return
     */
    boolean verify(byte[] bytes, String algorithm) {
        return verify(Bytes.toHex(bytes), algorithm)
    }

}
