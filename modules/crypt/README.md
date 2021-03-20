# How to install BouncyCastle Security provider
All notable changes to this project will be documented in this file.
https://www.ibm.com/developerworks/java/tutorials/j-sec1/j-sec1.html

### Download
* Go to https://www.bouncycastle.org/latest_releases.html
* Download the package with 'prov' in it, for example: bcprov-jdk15on-156.jar

### Install
* Copy the JAR into $JAVA_HOME/jre/lib/ext/
* Add the next line into: 
  $JAVA_HOME/jre/lib/security/java.security  or
  $JAVA_HOME/conf/security/java.security

`security.provider.10=org.bouncycastle.jce.provider.BouncyCastleProvider`

(you may change the number "10" for the next in the list)

below the other similar lines.