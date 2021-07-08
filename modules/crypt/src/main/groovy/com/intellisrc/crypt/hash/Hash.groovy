package com.intellisrc.crypt.hash

import com.intellisrc.crypt.Crypt
import com.intellisrc.etc.Bytes
import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.digests.MD5Digest
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.SHA224Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA384Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter

import java.security.MessageDigest
import java.security.Provider
import java.security.Provider.Service
import java.security.Security

/**
 * @since 17/04/07.
 */
@CompileStatic
class Hash extends Crypt implements Hashable {
    /**
     * Note: Using cost will reuse the bytes returned to rehash it again
     * This is different than: sha(sha(x)) because sha(x) returns String HEX
     * and not bytes (and thus restricting the characters availability).
     * In order to imitate this method in other languages something like
     * this must be done: sha(hex2bytes(sha(x)))
     */
    protected int cost = 1 //Iterations to use
    static enum BasicAlgo {
        MD5, SHA, SHA224, SHA256, SHA384, SHA512
        String toString() {
            String ret = ""
            switch(this) {
                case MD5    : ret = "MD5"; break
                case SHA    : ret = "SHA-1"; break
                case SHA224 : ret = "SHA-224"; break
                case SHA256 : ret = "SHA-256"; break
                case SHA384 : ret = "SHA-384"; break
                case SHA512 : ret = "SHA-512"; break
            }
            return ret
        }
    }
    /**
     * Search Basic Algorithm using also aliases
     * @param algo
     * @return
     */
    private static isBasicAlgo(String algo) {
        return ['MD5','SHA','SHA1','SHA-1','SHA224','SHA-224','SHA256','SHA-256','SHA384','SHA-384','SHA512','SHA-512'].contains(algo)
    }

    /**
     * Hash key and returns bytes
     * @return
     */
    byte[] asBytes(BasicAlgo algorithm) {
        return asBytes(algorithm.toString())
    }
    /**
     * Hash key and returns bytes
     * @return
     */
    byte[] asBytes(String algorithm) {
        assert cost > 0
        byte[] hash = key
        if(!isBasicAlgo(algorithm)) {
            if(!getAlgorithms(true).contains(algorithm)) {
                Log.e("Algorithm %s doesn't exists. Perhaps the library is not installed?", algorithm)
                return null
            }
        }
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
    String hash(BasicAlgo algorithm) {
        return hash(algorithm.toString())
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
     * Sign a message with HMAC
     * @param text
     * @param algorithm
     * @return
     */
    byte[] sign(final String text, BasicAlgo algorithm) {
        Digest digest
        switch (algorithm) {
            case BasicAlgo.SHA      : digest = new SHA1Digest(); break
            case BasicAlgo.SHA224   : digest = new SHA224Digest(); break
            case BasicAlgo.SHA256   : digest = new SHA256Digest(); break
            case BasicAlgo.SHA384   : digest = new SHA384Digest(); break
            case BasicAlgo.SHA512   : digest = new SHA512Digest(); break
            case BasicAlgo.MD5      :
            default:
                digest = new MD5Digest(); break
        }
        return sign(text, digest)
    }

    /**
     * Sign message with specific Digest (not included in the BasicAlgo enum), example:
     *
     * sign('hello', new TigerDigest())
     *
     * Other algorithms:
     * GOST3411Digest, MD2Digest, MD4Digest, RIPEMD128Digest, RIPEMD160Digest, RIPEMD256Digest,
     * RIPEMD320Digest, TigerDigest, WhirlpoolDigest
     *
     * @param text
     * @param digest
     * @return
     */
    byte[] sign(final String text, Digest digest) {
        HMac hmac = new HMac(digest)
        hmac.init(new KeyParameter(key))
        hmac.update(Bytes.fromString(text), 0, text.size())
        byte[]  resBuf = new byte[digest.getDigestSize()]
        hmac.doFinal(resBuf, 0)
        return resBuf
    }
    /**
     * Return Sign as HEX
     * @param text
     * @param digest
     * @return
     */
    String signHex(final String text, Digest digest) {
        return Bytes.toHex(sign(text, digest))
    }
    /**
     * Return Sign as HEX
     * @param text
     * @param digest
     * @return
     */
    String signHex(final String text, BasicAlgo algorithm) {
        return Bytes.toHex(sign(text, algorithm))
    }

    /**
     * Verifies that the input hash correspond to the input value
     * It will guess the hash used depending on hash length
     * @param hash
     * @param input
     * @return
     */
    boolean verify(String hash, BasicAlgo algorithm) {
        return verify(hash, algorithm.toString())
    }
    /**
     * Verify bytes instead of HEX hash
     * @param bytes
     * @return
     */
    boolean verify(byte[] bytes, BasicAlgo algorithm) {
        return verify(bytes, algorithm.toString())
    }
    /**
     * Verify bytes instead of HEX hash
     * @param bytes
     * @return
     */
    boolean verify(byte[] bytes, String algorithm) {
        return verify(Bytes.toHex(bytes), algorithm)
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
                    Log.w("Unable to guess hash type")
                    return false
            }
        } else if(!isBasicAlgo(algorithm)) {
            if(!getAlgorithms(true).contains(algorithm)) {
                Log.e("Algorithm %s doesn't exists. Perhaps the library is not installed?", algorithm)
                return false
            }
        }
        return this.hash(algorithm) == hash.toUpperCase()
    }
    /**
     * Return a list of supported Algorithms
     * @param incAlias : if true, it will include alias list as well
     * @param logVerbose : if true, it will printout the list
     * @return
     */
    static final List<String> getAlgorithms(boolean incAlias = false, boolean logVerbose = false) {
        List<String> algorithms = []
        def providers = Security.getProviders()
        providers.each {
            Provider prov ->
                String type = MessageDigest.class.simpleName
                List<Service> algos = []

                prov.getServices().each {
                    Service service ->
                        if (service.type.equalsIgnoreCase(type)) {
                            algos << service
                        }
                }

                if (!algos.isEmpty()) {
                    if(logVerbose) {
                        Log.v(" --- Provider %s, v. %.2f --- %n", prov.getName(), prov.getVersion())
                    }
                    algos.each {
                        Service service ->
                            String algo = service.getAlgorithm()
                            algorithms << algo
                            if(logVerbose) {
                                Log.v("Algorithm: \"%s\"%n", algo)
                            }
                    }
                }

                if(incAlias || logVerbose) {
                    // --- find aliases (inefficiently)
                    prov.keySet().each {
                        Object okey ->
                            def skey = okey.toString()
                            final String prefix = "Alg.Alias." + type + "."
                            if (skey.startsWith(prefix)) {
                                String alias = skey.substring(prefix.length())
                                String value = prov.get(skey).toString()
                                if(incAlias) {
                                    algorithms << alias
                                }
                                if(logVerbose) {
                                    Log.v("Alias: \"%s\" -> \"%s\"%n", alias, value)
                                }
                            }
                    }
                }
        }
        return algorithms
    }

}
