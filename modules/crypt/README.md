# How to install BouncyCastle Security provider

### JAVA 11 or up: ---------------------------------------------------------------------

"ext" Directory is no longer supported. Adding it through gradle is all its needed.

Security file is located at: 
/etc/java-11-openjdk/security/java.security
/usr/lib/jvm/java-11-openjdk-amd64/conf/security/java.security

### JAVA 8: -----------------------------------------------------------------------------

All notable changes to this project will be documented in this file.
https://www.ibm.com/developerworks/java/tutorials/j-sec1/j-sec1.html

### Download
* Go to https://www.bouncycastle.org/latest_releases.html
* Download the package with 'prov' in it, for example: bcprov-jdk15on-156.jar

### Install
* Copy the JAR into $JAVA_HOME/jre/lib/ext/
* Add the next line into: $JAVA_HOME/jre/lib/security/java.security

`security.provider.10=org.bouncycastle.jce.provider.BouncyCastleProvider`

below the other similar lines.
