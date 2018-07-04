package com.intellisrc.crypt.encode

import com.intellisrc.crypt.Crypt
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.SecureRandom
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.openpgp.PGPCompressedData
import org.bouncycastle.openpgp.PGPCompressedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPLiteralDataGenerator
import org.bouncycastle.openpgp.PGPPBEEncryptedData
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder
import org.bouncycastle.util.io.Streams

import java.security.Security

/**
 * OpenPGP wrapper
 * based in example:
 * bouncycastle/openpgp/examples/ByteArrayHandler.java
 *
 * The main difference between PGP and AES (besides the technical differences),
 * is that this doesn't require a fixed key length.
 *
 * @since 18/07/04.
 */
@CompileStatic
class PGP extends Crypt implements Encodable {
    /*
        Algorithms:
            NULL = 0            // Plaintext or unencrypted data
            IDEA = 1            // IDEA [IDEA]
            TRIPLE_DES = 2      // Triple-DES (DES-EDE, as per spec -168 bit key derived from 192)
            CAST5 = 3           // CAST5 (128 bit key, as per RFC 2144)
            BLOWFISH = 4        // Blowfish (128 bit key, 16 rounds) [BLOWFISH]
            SAFER = 5           // SAFER-SK128 (13 rounds) [SAFER]
            DES = 6             // Reserved for DES/SK
            AES_128 = 7         // Reserved for AES with 128-bit key
            AES_192 = 8         // Reserved for AES with 192-bit key
            AES_256 = 9         // Reserved for AES with 256-bit key
            TWOFISH = 10        // Reserved for Twofish
            CAMELLIA_128 = 11   // Reserved for Camellia with 128-bit key
            CAMELLIA_192 = 12   // Reserved for Camellia with 192-bit key
            CAMELLIA_256 = 13   // Reserved for Camellia with 256-bit key
     */
    int algorithm = PGPEncryptedDataGenerator.BLOWFISH
    boolean armor = false   //For String output (similar to PEM), set it true
    int keylen = 32 //Any number is possible

    /**
     * PGP constructor: initialize provider
     */
    PGP() {
        Security.addProvider(new BouncyCastleProvider())
    }
    /**
     * Encrypt some data and stored in a file
     * @param original
     * @return
     */
    @Override
    byte[] encrypt(byte[] original) {
        byte[] compressedData = compress(original, CompressionAlgorithmTags.ZIP)
        ByteArrayOutputStream bOut = new ByteArrayOutputStream()
        OutputStream out = bOut
        if (armor) {
            out = new ArmoredOutputStream(out)
        }
        genIfNoKey(keylen, false)
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(algorithm).setSecureRandom(new SecureRandom()).setProvider("BC"))
        encGen.addMethod(new JcePBEKeyEncryptionMethodGenerator(Bytes.toChars(key)).setProvider("BC"))

        OutputStream encOut = encGen.open(out, compressedData.length)
        encOut.write(compressedData)
        encOut.close()
        if (armor) {
            out.close()
        }
        return bOut.toByteArray()
    }

    /**
     *
     * @param encoded
     * @return
     */
    @Override
    byte[] decrypt(byte[] encoded) {
        InputStream inBytes = new ByteArrayInputStream(encoded)
        inBytes = PGPUtil.getDecoderStream(inBytes)
        JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(inBytes)
        PGPEncryptedDataList enc
        Object o = pgpF.nextObject()

        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList)o
        } else {
            enc = (PGPEncryptedDataList)pgpF.nextObject()
        }
        genIfNoKey(keylen, false)
        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData)enc.get(0)
        InputStream clear = pbe.getDataStream(new JcePBEDataDecryptorFactoryBuilder(new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build()).setProvider("BC").build(Bytes.toChars(key)))
        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(clear)
        PGPCompressedData cData = (PGPCompressedData)pgpFact.nextObject()
        pgpFact = new JcaPGPObjectFactory(cData.getDataStream())
        PGPLiteralData ld = (PGPLiteralData)pgpFact.nextObject()
        return Streams.readAll(ld.getInputStream())
    }

    /**
     * Compress PGP data
     * @param clearData
     * @param fileName
     * @param algorithm
     * @return
     * @throws IOException
     */
    private static byte[] compress(byte[] clearData, int algorithm) throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream()
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm)
        OutputStream cos = comData.open(bOut) // open it with the final destination
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator()
        // we want to generate compressed data. This might be a user option later,
        // in which case we would pass in bOut.
        OutputStream  pOut = lData.open(cos, // the compressed output stream
                PGPLiteralData.BINARY,
                Bytes.toString(randomChars(16)),  // "filename" to store in headers
                clearData.length, // length of clear data
                new Date()  // current time
        )
        pOut.write(clearData)
        pOut.close()
        comData.close()
        return bOut.toByteArray()
    }
}
