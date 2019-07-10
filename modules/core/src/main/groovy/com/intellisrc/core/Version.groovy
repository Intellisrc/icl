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
    //This class will be changed by SysMain or SysService
    static Class mainClass = Version.class
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
     * Get Mainfest Attributes
     * @return
     */
    static private Attributes getMainfestAttributes() {
        Attributes attr = null
        try {
            String className = mainClass.simpleName + ".class"
            String classPath = mainClass.getResource(className).toString()
            if (classPath.startsWith("jar")) {
                String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"
                URL url = new URL(manifestPath)
                Manifest manifest = new Manifest(url.openStream())
                attr = manifest.mainAttributes
            }
        } catch(Exception ignored) { /*IGNORE*/ }
        return attr
    }
    /**
     * Get Gradle Properties if present
     * @return
     */
    static private Config.Props getGradleProps() {
        Config.Props props = null
        File bg = new File(SysInfo.userDir, "gradle.properties")
        if(bg.exists()) {
            props = new Config.Props(bg)
        }
        return props
    }
}
