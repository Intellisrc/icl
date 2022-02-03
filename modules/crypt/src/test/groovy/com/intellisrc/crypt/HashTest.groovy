package com.intellisrc.crypt

import com.intellisrc.core.Log
import com.intellisrc.crypt.hash.Hash
import com.intellisrc.etc.Bytes
import org.bouncycastle.crypto.digests.TigerDigest
import spock.lang.Specification

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * @since 17/04/11.
 */
class HashTest extends Specification {
    def "Get List of Algorithms"() {
        given:
            def listAlgo = Hash.getAlgorithms()
            def listAlias = Hash.getAlgorithms(true,true)
        expect:
            assert listAlgo.size()
            assert listAlias.size()
            assert listAlias.size() > listAlgo.size()
    }
    def "Testing Enum BasicAlgo"() {
        given:
            def hba224 = Hash.BasicAlgo.SHA224
            def hbamd5 = Hash.BasicAlgo.MD5
        expect:
            assert hba224.toString() == "SHA-224"
            assert hbamd5.toString() == "MD5"
    }
    def "MD5 Hash"() {
        given:
            String str = "admin"
            Hash hash = new Hash(key: Bytes.fromString(str))
        expect:
            assert hash.MD5() == "21232f297a57a5a743894a0e4a801fc3".toUpperCase()
            assert hash.verify("21232f297a57a5a743894a0e4a801fc3")
    }
    def "SHA Hash as default"() {
        given:
            String str = "admin"
            Hash hash = new Hash(key: Bytes.fromString(str))
        expect:
            assert hash.hash() == "d033e22ae348aeb5660fc2140aec35850c4da997".toUpperCase()
            assert hash.verify("d033e22ae348aeb5660fc2140aec35850c4da997")
    }
    def "SHA256 Hash and setting type"() {
        given:
            String str = "admin"
            Hash hash = new Hash(key: Bytes.fromString(str))
        expect:
            assert hash.SHA256() == "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918".toUpperCase()
            assert hash.verify("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918")
    }
    def "SHA512 Hash and Auto-detect-SHA512"() {
        given:
            String str = "admin"
            Hash hash = new Hash(key: Bytes.fromString(str))
            Hash hash2 = new Hash(key: Bytes.fromString(str)) //We verify the hash without specifying what kind of hash is
        expect:
            assert hash.SHA512() == "c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec".toUpperCase()
            assert hash2.verify("c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec")
    }
    def "MD2 Hash and Auto-detect-MD2"() {
        given:
            String str = "admin"
            Hash hash = new Hash(key: Bytes.fromString(str))
            Hash hash2 = new Hash(key: Bytes.fromString(str)) //We verify the hash without specifying what kind of hash is
        expect:
            assert hash.hash("MD2") == "3e3e6b0e5c1c68644fc5ce3cf060211d".toUpperCase()
            assert hash2.verify("3e3e6b0e5c1c68644fc5ce3cf060211d", "MD2")
    }
    def "SHA Cost"() {
        given:
            String str = "admin"
            Hash hash = new Hash(key: Bytes.fromString(str), cost: 100)
        expect:
            assert hash.SHA() == "E9336BA3B1F8A49D98C888FD54EE923B3942A4D8"
            assert hash.verify("E9336BA3B1F8A49D98C888FD54EE923B3942A4D8")
    }
    def "Hash as bytes and extended algorithm"() {
        given:
            String str = "admin"
            String algo = "SHA-256"
            Hash hash = new Hash(key: Bytes.fromString(str))
            byte[] bytes = hash.asBytes(algo)
            Log.i("TIGER HASH: "+Bytes.toHex(bytes))
        expect:
            if(!Hash.getAlgorithms().contains(algo)) {
                Log.e("TIGER requires BountyCastle to be installed, Refer to the README.md file")
            }
            assert hash.verify(bytes, algo)
    }
    def "Static SHA"() {
        given:
            String hash = Hash.SHA("admin".toCharArray())
        expect:
            assert hash == "d033e22ae348aeb5660fc2140aec35850c4da997".toUpperCase()
    }
    def "Static SHA with cost"() {
        given:
            String hash = Hash.SHA("admin".toCharArray(), 100)
        expect:
            assert hash == "E9336BA3B1F8A49D98C888FD54EE923B3942A4D8"
    }
    def "Sign message with HMAC_256"() {
        given:
            def key = "secret"
            def msg = "Message"
            def hash = new Hash(key: Bytes.fromString(key))
        expect:
            def hashed = hash.sign(msg, Hash.BasicAlgo.SHA256)
            def hashedHex = hash.signHex(msg, Hash.BasicAlgo.SHA256)
            def otherMethod = hmac_sha256(key, msg)
            assert hashed == otherMethod
            assert hashedHex == Bytes.toHex(otherMethod)
            println hashedHex
            def asB64 = otherMethod.encodeBase64().toString()
            assert hashed.encodeBase64().toString() == asB64
            println asB64
            assert asB64 == "qnR8UCqJggD55PohusaBNviGoOJ67HC6Btry4qXLVZc="
    }
    // Implementation using java.crypto (as reference)
    def hmac_sha256(String secretKey, String data) {
        Mac mac = Mac.getInstance("HmacSHA256")
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal(data.getBytes())
    }

    def "Sign with custom Digest"() {
        given:
        def key = "secret"
        def msg = "Message"
        def hash = new Hash(key: Bytes.fromString(key))
        expect:
        def hashed = hash.signHex(msg, new TigerDigest())
        assert hashed
        println hashed
    }
}
