---
name: crypt
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for security - hashing (Hash with MD5, SHA family, TIGER, WHIRLPOOL, BLAKE, SHA3 via BouncyCastle), password hashing and verification (PasswordHash with BCrypt and SCrypt), symmetric encryption (AES), OpenPGP-style encryption (PGP), string obfuscation with unicode charsets (LpCode), secure random generation, or keystore/certificate generation for HTTPS (KeyStoreGenerator). Triggers on com.intellisrc.crypt imports in an ICL project.
---

# ICL crypt Module

Crypto for ICL, simplifying BouncyCastle (bundled: bcprov/bcpkix/bcpg jdk18on). Includes core and etc.

```groovy
implementation 'com.intellisrc:crypt:2.10.5'   // + core + etc + groovy
```

Packages: `com.intellisrc.crypt` (base), `crypt.hash` (Hash, PasswordHash), `crypt.encode` (AES, PGP, LpCode).

## Hash — digests

```groovy
import com.intellisrc.crypt.hash.Hash

def h = new Hash(key: "some text".bytes)       // key = input bytes; also from a file's bytes
h.MD5() ; h.SHA() ; h.SHA256()                 // hex String shortcuts
h.hash("SHA-512") ; h.hash("TIGER")            // any registered algorithm
h.asBytes("SHA256")                            // raw bytes
Hash.getAlgorithms()                           // list everything available
h.setCost(1000)                                // iterate digest N times (stretching)

// HMAC + verify
byte[] sig = h.sign(text, Hash.BasicAlgo.SHA256) ; h.signHex(text, algo)
h.verify(hexHash)                              // true/false

// static helpers
Hash.SHA256("pass".toCharArray())
```

Output is uppercase hex; use `Bytes.toHex/fromHex` (etc module) for other formats.

## PasswordHash — BCrypt / SCrypt

```groovy
import com.intellisrc.crypt.hash.PasswordHash

def hasher = new PasswordHash(password: "secret123".toCharArray())
String hash = hasher.BCrypt()                  // $2y$10$...  (cost defaults to 10)
// also hasher.SCrypt(), hasher.hash("SCRYPT"); hasher.setCost(n) to tune

def checker = new PasswordHash(password: candidate.toCharArray())
boolean ok = checker.verify(storedHash)        // algorithm auto-detected from header

hasher.clear()                                 // zero password/salt arrays when done
```

Salt is auto-generated and embedded in the stored hash. Do not store plain MD5/SHA for passwords — always use this class.

## AES — symmetric

```groovy
import com.intellisrc.crypt.encode.AES

AES aes = new AES()                            // auto-generates a 256-bit key
byte[] enc = aes.encrypt("secret".bytes)
byte[] dec = aes.decrypt(enc)
String keyHex = Bytes.toHex(aes.key)           // store this; IV is random and prepended per message
// restore later:
AES aes2 = new AES(key: Bytes.fromHex(keyHex)) // key must be exactly 16, 24 or 32 bytes
```

## PGP — passphrase-style encryption (any key length)

```groovy
import com.intellisrc.crypt.encode.PGP

PGP pgp = new PGP()                            // auto-generates key; or new PGP(key: myBytes)
pgp.armor = true                               // ASCII-armored output for text storage
pgp.algorithm = PGPEncryptedDataGenerator.AES_256   // default BLOWFISH; also AES_128/192, TWOFISH...
byte[] enc = pgp.encrypt(data) ; pgp.decrypt(enc)
```

## LpCode — unicode obfuscation

Translates text between unicode charsets (BASIC, ALPHA, HANZU, BRAILLE, CYRILLIC, 200+). Smaller output charset = longer output. Use for obfuscation/reduction, NOT security.

```groovy
import com.intellisrc.crypt.encode.LpCode

def lp = new LpCode(inputCharset: LpCode.BASIC, outputCharset: LpCode.HANZU)
String hidden = lp.encode("hello".toCharArray())
String plain  = lp.decode(hidden)
// block/chunk variants for large text: encodeBlock/decodeBlock, encodeByChunks/decodeByChunks
// static charset tools: LpCode.getCodePoints(str), LpCode.translate(str, from, to)
```

## Crypt base + randoms

```groovy
import com.intellisrc.crypt.Crypt

Crypt.randomChars(len, Crypt.Complexity.HIGH)   // random ASCII: LOW alnum, MEDIUM +safe symbols, HIGH +extended
Crypt.randomBytes(len)
// instance helpers on Hash/AES/PGP: key(bytes), hasKey(), genKey(len), clear()
```

## KeyStoreGenerator — self-signed certs for HTTPS

```groovy
import com.intellisrc.crypt.KeyStoreGenerator

new KeyStoreGenerator(
    subject: "localhost", algo: "RSA", keysize: 2048, days: 365,
    ip: "127.0.0.1".toInet4Address(), includeIp: true      // SAN entries
).create(new File("keystore.p12"), "password".toCharArray())
// feed to web module: new WebService(ssl: new KeyStore(new File("keystore.p12"), "password".toCharArray()))
```

## Gotchas

- AES accepts only 16/24/32-byte keys (invalid lengths are auto-corrected to 32 with a warning).
- Crypto classes are NOT thread-safe; create one per thread or synchronize.
- Call `clear()` on instances holding secrets when done.
- BCrypt salt must be 16 bytes, SCrypt 16-512 (auto-corrected).
- Modern JVMs (21+) ship unlimited JCE strength — no policy files needed for AES-256.
- `PasswordHash` supports BCRYPT and SCRYPT; the PBKDF2 enum value is not implemented.
