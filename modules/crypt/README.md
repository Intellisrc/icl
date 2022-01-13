# CRYPT Module

Offers methods to encode, decode, hash and encrypt
information. It is built using the BouncyCastle
library and simplifying its usage without reducing
its safety.

This module was built using `byte` and `char` arrays
instead of `String` to prevent those values to reside
in memory.


### Examples

This module provides a simple interface for all hashing
algorithms supported by BouncyCastle.

```groovy
// List of supported algorithms:
List<String> listAlgo = Hash.getAlgorithms()
Hash hash = new Hash(key: "SomeKey".bytes)
byte[] bytes = hash.asBytes("TIGER") //Using TIGER algorithm
Log.i("TIGER HASH: %s", Bytes.toHex(bytes))
```

You can also sign messages:

```groovy
String msg = "Message"
def hash = new Hash(key: "secret".bytes)
byte[] hashed = hash.sign(msg, Hash.BasicAlgo.SHA256)
String signedHex = hash.signHex(msg, Hash.BasicAlgo.SHA256)
```

To hash passwords, this module provides implementations
for BCrypt (CPU intensive) and SCrypt (Memory Intensive)

```groovy
// Generate random strings:
byte[] rand = crypt.randomChars(50, Crypt.Complexity.HIGH)

// Get BCrypt password hash:
def hasher = new PasswordHash(password: rand)
String hash = hasher.BCrypt()
hasher.clear() //Optional to clear used memory

byte[] pwd = "Some password provided by user".bytes
def verifier = new PasswordHash(password: pwd)
// Verify that provided password matches stored hash:
verifier.verify(hash)
```

For encryption/decryption, this module provides implementations for
AES and PGP. 

```groovy
byte[] key = "584F5A53487853313550577939666B37665973623145516D37327655347A4F43".bytes
byte[] secret = "Hello Earth!".bytes

//If no key is provided, it will generate one. You can get it with `aes.key`
AES aes = new AES(key: key)
byte[] encodedBytes = aes.encrypt(secret)
String encoded = Bytes.toHex(encodedBytes)
byte[] decoded = aes.decrypt(Bytes.fromHex(encoded))
println Bytes.toString(decoded)
// will print: "Hello Earth!"
```

```groovy
PGP pgp = new PGP()
byte[] original = "This is a super secret message!".bytes
byte[] encrypted = pgp.encrypt(original)
println "encrypted data = '" + Bytes.toHex(encrypted) + "'"
byte[] decrypted = pgp.decrypt(encrypted)
println "decrypted data = '" + Bytes.toString(decrypted) + "'"
assert decrypted == original
// When no key is specified, it will generate one for you, this is your key:
println "Used Key: " + Bytes.toString(pgp.key)
```

## LpCode

With this class you can obfuscate strings in a different way. It uses blocks
in the UTF-8 range to convert and randomize (when using seed) the output.
It can also be used to reduce the amount of data to store (see details in class).

### Example

```groovy
char[] toEncode = "HelloWorld".toCharArray()
long seed = new Random().nextLong()
LpCode lpCode = new LpCode(ALPHA, HANZU, seed)
println lpCode.encode(toEncode)
// Will return something like: 
// 竢茫哊鰱
```

**NOTE** : Please check the comments inside the class. There are many 
possible conversions like: NUM, HASH, BASE64, KOR, HIRA, KANA, etc.


## How to install BouncyCastle Security provider

To use BouncyCastle library, you don't need to do anything, 
but in the case that the list of supported algorithms doesn't include
all the supported by that library, you may need to add it manually
to your `java.security` file. This is how:

### JAVA 11 or up: ---------------------------------------------------------------------

Adding it through gradle is all its needed.

For your reference, security file is located at: 
/etc/java-11-openjdk/security/java.security
/usr/lib/jvm/java-11-openjdk-amd64/conf/security/java.security

### JAVA 8: -----------------------------------------------------------------------------

### Download
* Go to https://www.bouncycastle.org/latest_releases.html
* Download the package with 'prov' in it, for example: bcprov-jdk15on-156.jar

### Install
* Copy the JAR into $JAVA_HOME/jre/lib/ext/
* Add the next line into: $JAVA_HOME/jre/lib/security/java.security

`security.provider.10=org.bouncycastle.jce.provider.BouncyCastleProvider`

below the other similar lines (you may need to fix the number before '=' sign).
