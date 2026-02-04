package com.intellisrc.crypt

import groovy.transform.CompileStatic
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

import java.security.*
import java.security.cert.X509Certificate

/**
 * Generates a keystore (that can be used with the web server)
 */
@CompileStatic
class KeyStoreGenerator {
    String subject = "localhost"
    String alias = subject
    String algo = "RSA"
    String signAlgo = "SHA256withRSA"
    String keystoreType = "PKCS12"   // ← RECOMMENDED
    Inet4Address ip = "127.0.0.1".toInet4Address()
    int days = 365
    int keysize = 2048
    boolean includeIp = false

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider())
        }
    }

    void create(File file, char[] password, boolean replace = true) {

        if (file.exists()) {
            if (replace) file.delete()
            else return
        }

        /* ------------------------------------------------------------ */
        /* Key pair                                                     */
        /* ------------------------------------------------------------ */
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algo)
        keyGen.initialize(keysize)
        KeyPair keyPair = keyGen.generateKeyPair()

        /* ------------------------------------------------------------ */
        /* Certificate dates                                            */
        /* ------------------------------------------------------------ */
        Date notBefore = new Date(System.currentTimeMillis() - 60_000)
        Date notAfter  = new Date(System.currentTimeMillis() + days * 86_400_000L)

        BigInteger serial = new BigInteger(64, new SecureRandom())

        X500Name dn = new X500Name("CN=${subject}")

        /* ------------------------------------------------------------ */
        /* Subject Alternative Names                                    */
        /* ------------------------------------------------------------ */
        List<GeneralName> sanList = []
        sanList << new GeneralName(GeneralName.dNSName, subject)

        if (includeIp) {
            sanList << new GeneralName(
                GeneralName.iPAddress,
                ip.hostAddress
            )
        }

        GeneralNames subjectAltNames =
            new GeneralNames(sanList as GeneralName[])

        /* ------------------------------------------------------------ */
        /* Certificate builder                                          */
        /* ------------------------------------------------------------ */
        JcaX509v3CertificateBuilder certBuilder =
            new JcaX509v3CertificateBuilder(
                dn,
                serial,
                notBefore,
                notAfter,
                dn,
                keyPair.public
            )

        certBuilder.addExtension(
            Extension.subjectAlternativeName,
            false,
            subjectAltNames
        )

        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            new BasicConstraints(false)
        )

        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            new KeyUsage(
                KeyUsage.digitalSignature |
                    KeyUsage.keyEncipherment
            )
        )

        /* ------------------------------------------------------------ */
        /* Sign                                                         */
        /* ------------------------------------------------------------ */
        ContentSigner signer =
            new JcaContentSignerBuilder(signAlgo)
                .build(keyPair.private)

        X509CertificateHolder holder =
            certBuilder.build(signer)

        X509Certificate cert =
            new JcaX509CertificateConverter()
                .getCertificate(holder)

        cert.verify(keyPair.public)

        /* ------------------------------------------------------------ */
        /* Store keystore                                               */
        /* ------------------------------------------------------------ */
        KeyStore ks = KeyStore.getInstance(keystoreType)
        ks.load(null, null)

        ks.setKeyEntry(
            alias,
            keyPair.private,
            password,
            [cert] as X509Certificate[]
        )

        file.parentFile?.mkdirs()
        file.withOutputStream { os ->
            ks.store(os, password)
        }
    }
}
