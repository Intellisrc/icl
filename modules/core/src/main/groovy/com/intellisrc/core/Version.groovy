package com.intellisrc.core

import groovy.transform.CompileStatic

import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.regex.Matcher

/**
 * Version object which can be used to easily update parts of it.
 * It can import versions from String and export them to String
 *
 * For example:
 *  2.0-beta1
 *  3.11.293
 *  4.1.1.14
 *  5.0.1-SNAPSHOT
 *
 * [Static] Automatic search for version
 * @since 17/11/17.
 */
@CompileStatic
class Version {
    ////////////////////// INSTANCE ///////////////////////////
    int mayor = 0
    int minor = 0
    int build = 0
    int revision = 0
    String suffix = ""

    boolean minorEnabled = false
    boolean buildEnabled = false
    boolean revisionEnabled = false
    /**
     * Constructor
     * @param version
     */
    Version(String version = "") {
        parseString(version ?: get())
    }
    /**
     * Parse string
     * @param version
     */
    protected void parseString(String version) {
        Matcher m = (version =~/^(\d+)(\.(\d+))?(\.(\d+))?(\.(\d+))?([a-zA-Z0-9-]+)?$/)
        if(m.matches()) {
            mayor = m.group(1) as int
            if(m.group(3)) {
                minor = m.group(3) as int
                minorEnabled = true
            }
            if(m.group(5)) {
                build = m.group(5) as int
                buildEnabled = true
            }
            if(m.group(7)) {
                revision = m.group(7) as int
                revisionEnabled = true
            }
            suffix = m.group(8) ?: ""
        }
    }
    /**
     * Return version to string
     * @return
     */
    String toString() {
        String min = minorEnabled ? "." + minor : ""
        String bld = buildEnabled ? "." + build : ""
        String rev = revisionEnabled ? "." + revision : ""
        return mayor + min + bld + rev + suffix
    }
    ////////////////////// STATIC ///////////////////////////
    //This class will be changed by SysMain or SysService
    static public Class mainClass = Version.class
    private static enum Source {
        JAR, CONFIG, GRADLE, NONE
    }
    private static Source source = Source.NONE
    /**
     * Parse a string and return Version object
     * @return
     */
    static Version parse(String version) {
        return new Version(version)
    }
    /**
     * Return current version as Version
     * @return
     */
    static Version getCurrent() {
        return new Version(get())
    }
    /**
     * Return system version from JAR or Gradle file
     * @return
     */
    static String get() {
        String version = mainfestAttributes?.getValue("Implementation-Version")
        if(version) {
            source = Source.JAR
        }
        if(!version && Config.exists()) {
            version = Config.get("version")
            if(version) {
                source = Source.CONFIG
            }
        }
        if(!version) {
            Config.Props gradle = gradleProps
            if(gradle) {
                version = gradle.keys.find { it.toLowerCase().contains("version") }
                if (version) {
                    source = Source.GRADLE
                }
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
