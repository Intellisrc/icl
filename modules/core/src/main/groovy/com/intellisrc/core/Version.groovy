package com.intellisrc.core

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
    /**
     * Return system version from JAR or Gradle file
     * @return
     */
    static String get() {
        String version = mainfestAttributes?.getValue("Implementation-Version")
        if(version) {
            source = Source.JAR
        } else {
            version = gradleProps?.get("currentVersion")
            if(version) {
                source = Source.GRADLE
            }
        }
        if(!version && Config.exists()) {
            version = Config.get("version")
            if(version) {
                source = Source.CONFIG
            }
        }
        return version ?: "0.0"
    }
    /**
     * Return source of version
     * @return
     */
    static String getSource() {
        return source.toString()
    }
    /**
     * Return Main class canonical name (including package name)
     * @return
     */
    static String getMainClass() {
        String mainClass = mainfestAttributes?.getValue("Main-Class")
        if(!mainClass) {
            mainClass = gradleProps?.get("jarClass")
        }
        return mainClass
    }
    /**
     * Get Mainfest Attributes
     * @return
     */
    static private Attributes getMainfestAttributes() {
        Attributes attr = null
        try {
            Class self = Version.class
            def className = self.simpleName + ".class"
            def classPath = self.getResource(className).toString()
            if (classPath.startsWith("jar")) {
                def manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"
                def url = new URL(manifestPath)
                Manifest manifest = new Manifest(url.openStream())
                attr = manifest.getMainAttributes()
            }
        } catch(Exception e) { /*IGNORE*/ }
        return attr
    }
    /**
     * Get Gradle Properties if present
     * @return
     */
    static private Config.Props getGradleProps() {
        Config.Props props = null
        def bg = new File(SysInfo.userDir + "gradle.properties")
        if(bg.exists()) {
            props = new Config.Props(bg)
        }
        return props
    }
}
