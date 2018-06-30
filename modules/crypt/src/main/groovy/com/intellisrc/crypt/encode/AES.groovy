package com.intellisrc.crypt.encode

import com.intellisrc.core.Log
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV

/**
 * @since 17/04/07.
 * based on: http://stackoverflow.com/questions/4243650/
 */
@groovy.transform.CompileStatic
class AES extends com.intellisrc.crypt.Crypt implements Encodable {
    private static final int IV_LENGTH = 16 //That is 128bits for AESEngine
    private byte[] iv
    int keylen = 32 //Valid values are: 16, 24 and 32

    /**
     * Encrypt some text
     * @param text
     * @return
     */
    byte[] encrypt(byte[] text) {
        return cipherData(text, true)
    }

    /**
     * Decrypt encrypted text
     * @param encoded
     * @return
     */
    byte[] decrypt(byte[] encoded) {
        return cipherData(encoded, false)
    }

    /**
     * Generates a new IV
     * @param len
     */
    private void genIV(int len) {
        iv = randomBytes(len)
    }

    /**
     * Perform the encryption / decryption of data
     * @param data
     * @param encode
     * @return
     */
    private byte[] cipherData(byte[] data, boolean encode = true) {
        byte[] result = new byte[0]
        // Fix keylen if mistaken
        if(![16,24,32].contains(keylen)) {
            Log.w("Key length must be 16,24 or 32 bytes long")
            keylen = 32
        }
        genIfNoKey(keylen)
        genIV(IV_LENGTH)
        //We extract the IV in decode
        if(!encode) {
            System.arraycopy(data, 0, iv, 0, IV_LENGTH)
            data = Arrays.copyOfRange(data, IV_LENGTH, data.length)
        }
        PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()))
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv)
        try {
            aes.init(encode, ivAndKey)
            int minSize = aes.getOutputSize(data.length)
            byte[] outBuf = new byte[minSize]
            int length1 = aes.processBytes(data, 0, data.length, outBuf, 0)
            int length2 = aes.doFinal(outBuf, length1)
            int actualLength = length1 + length2
            if(encode) {
                actualLength += IV_LENGTH
            }
            result = new byte[actualLength]
            if(encode) {
                System.arraycopy(iv, 0, result, 0, IV_LENGTH)
                System.arraycopy(outBuf, 0, result, IV_LENGTH, result.length - IV_LENGTH)
            } else {
                System.arraycopy(outBuf, 0, result, 0, result.length)
            }
        } catch(Exception e){
            Log.e( "Unable to encode/decode data", e)
        }
        return result
    }
}
