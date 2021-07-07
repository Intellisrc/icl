package com.intellisrc.crypt.hash

import com.intellisrc.crypt.Crypt
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic

import org.bouncycastle.crypto.generators.OpenBSDBCrypt
import org.bouncycastle.crypto.generators.SCrypt

/**
 * @since 17/04/07.
 */
@CompileStatic
class PasswordHash extends Crypt implements Hashable {
    protected int keylen = 16 //BCrypt requires 16 bytes for salt
    protected int cost = 10 //Workload
    //SCRYPT unique parameters
    protected int scryptBlockSize = 8 //default and minimum. Maximum: 32 bytes
    protected int scryptParallelization = 1
    //------------------------
    transient char[] password
    int passlen = 12 //Password length if not set

    /**
     * Alias for setKey, it makes more sense in PasswordHash
     * @param salt
     */
    void setSalt(byte[] salt){
        key = salt //linked
    }
    /**
     * Alias for getKey, it makes more sense in PasswordHash
     * @return
     */
    byte[] getSalt() {
        return key
    }
    /**
     * Return crypt parameters based on Type
     * @return
     */
    protected String getCryptParams(HashType type) {
        String header = ""
        switch(type) {
            case HashType.BCRYPT: header = sprintf("%02d", cost) + '$'; break
            case HashType.SCRYPT: header = (2**cost)+'$'+scryptBlockSize+'$'+scryptParallelization+'$'; break
        }
        return header
    }
    /**
     * Creates a hash from password
     * @isolated: if true, will return only the hash (without salt and descriptor)
     * if isolated is set, cost and hash type must be stored separately
     * @return
     */
    String hash(String algorithm = "BCRYPT", boolean header = true) {
        genIfNoKey(keylen)
        genIfNoPass()
        String hash
        HashType type = HashType.fromString(algorithm)
        switch(type) {
            case HashType.SCRYPT:
                if(keylen < 16 || keylen > 512) {
                    keylen = 32
                }
                hash = type.cryptHeader + getCryptParams(type) + Bytes.toHex(SCrypt.generate(Bytes.fromChars(password), key, (2**cost).toInteger(), scryptBlockSize, scryptParallelization, keylen))
                break
            //case HashType.BCRYPT:
            default:
                if(keylen != 16) {
                    keylen = 16
                }
                hash = OpenBSDBCrypt.generate(password, key, cost)
                break
        }
        if(!header) {
            hash = hash.replace(type.cryptHeader + getCryptParams(type),'')
        }
        return hash
    }

    /**
     * Return standard BCrypt Hash
     * @return
     */
    String BCrypt() {
        return hash("BCRYPT")
    }
    static BCrypt(char[] password, int cost = 10) {
        return new PasswordHash(password : password, cost : cost).BCrypt()
    }

    /**
     * Returns BCrypt hash without Header
     * @return
     */
    String BCryptNoHeader() {
        return hash("BCRYPT", false)
    }
    static BCryptNoHeader(char[] password, int cost = 10) {
        return new PasswordHash(password : password, cost : cost).BCryptNoHeader()
    }

    /**
     * Return a SCrypt Hash
     * Keylen must be 16 -> 512 bytes
     * @return
     */
    String SCrypt() {
        return hash("SCRYPT")
    }
    static SCrypt(char[] password, int cost = 10) {
        return new PasswordHash(password : password, cost : cost, keylen: 32).SCrypt()
    }

    /**
     * Number of iterations to perform
     * @param costSet
     */
    void setCost(int costSet) {
        cost = costSet
    }

    /**
     * Specify key length
     * @param len
     */
    void setKeyLen(int len) {
        keylen = len
    }

    /**
     * Verifies that the input hash correspond to the input value
     * It will guess the hash used depending on hash length
     * @param hash
     * @param input
     * @return
     */
    boolean verify(String hash, String algorithm = "BCRYPT") {
        genIfNoKey(keylen)
        HashType type = HashType.fromString(algorithm)
        if(!hash.startsWith(type.cryptHeader)) {
            hash = type.cryptHeader + getCryptParams(type) + hash
        }
        return OpenBSDBCrypt.checkPassword(hash, password)
    }

    /**
     * Generates a Password if not set
     */
    void genIfNoPass() {
        if(password == null) {
            password = Bytes.toChars(randomChars(passlen, Complexity.MEDIUM))
        }
    }

    /**
     * Clear used arrays
     */
    void clear() {
        Arrays.fill(password, (char) 0)
        super.clear()
    }
}
