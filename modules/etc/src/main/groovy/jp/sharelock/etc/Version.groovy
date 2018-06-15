package jp.sharelock.etc

import groovy.transform.CompileStatic

import java.util.jar.Attributes
import java.util.jar.Manifest

/**
 * Automatic search for version
 * @since 17/11/17.
 */
@CompileStatic
class Version {
    private static enum Source {
        JAR, CONFIG, GRADLE, NONE
    }
    private static Source source = Source.NONE
    static String get() {
        def version = ""
        try {
            Class self = Version.class
            def className = self.simpleName + ".class"
            def classPath = self.getResource(className).toString()
            if (classPath.startsWith("jar")) {
                def manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"
                def url = new URL(manifestPath)
                Manifest manifest = new Manifest(url.openStream())
                Attributes attr = manifest.getMainAttributes()
                version = attr.getValue("Implementation-Version")
                if(version) {
                    source = Source.JAR
                }
            }
        } catch(Exception e) { /*IGNORE*/ }
        if(!version) {
            def bg = new File("gradle.properties")
            if(bg.exists()) {
                def bgp = new Properties()
                bgp.load(bg.newDataInputStream())
                version = bgp.getProperty("currentVersion")
                if(version) {
                    source = Source.GRADLE
                }
            }
        }
        if(!version && Config.exists()) {
            version = Config.get("version")
            if(version) {
                source = Source.CONFIG
            }
        }
        return version
    }
    static String getSource() {
        return source.toString()
    }
}
