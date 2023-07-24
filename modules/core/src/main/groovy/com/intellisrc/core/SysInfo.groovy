package com.intellisrc.core

import groovy.transform.CompileStatic

/**
 * @since 1/2/17.
 */
@CompileStatic
/**
 * System Information class
 * For example: get OS type
 */
class SysInfo {
    static String newLine = getNewLineForCurrentOS() //Keep in memory

    static final enum OSType {
        UNKNOWN, LINUX, WINDOWS, ANDROID, IOS
    }
    /**
     * Returns OS in which the system is installed
     * @param osStr : if not set will read it from localhost
     */
    static OSType getOS() {
        def osType = OSType.UNKNOWN
        def osStr = System.getProperty("os.name")
        //noinspection GroovyFallthrough
        switch(osStr) {
            case ~/(?i).*version.*service.*pack.*/ :
            case ~/.*(?i)win.*/ : osType = OSType.WINDOWS; break
            case ~/.*(?i)android.*/ : osType = OSType.ANDROID; break
            case ~/.*(?i)(nix|nux|aix).*/ : osType = OSType.LINUX; break
            case ~/(?i).*mac.*/ : osType = OSType.IOS; break
            default:
                //We can't use Log.v() here as that function call this one
                // and enter in an infinite loop
                println "> Not Found: $osStr"
        }
        return osType
    }
    /**
     * Alternative to `new File()` which is more flexible
     * (alias for File.get in groovy extends)
     *
     * @param self
     * @param path
     * @return
     */
    static File getFile(Object... path) {
        return File.get(path)
    }
    
    /**
     * Identify if OS is Windows
     * @return
     */
    static boolean isWindows() {
        return getOS() == OSType.WINDOWS
    }
    /**
     * Identify if OS is Linux (excluding Android)
     * @return
     */
    static boolean isLinux() {
        return getOS() == OSType.LINUX
    }
    /**
     * Identify if OS is any Linux including Android
     * @return
     */
    static boolean isAnyLinux() {
        return getOS() == OSType.ANDROID || getOS() == OSType.LINUX
    }
    /**
     * Identify if OS is MacOS or iOS
     * @return
     */
    static boolean isMac() {
        return getOS() == OSType.IOS
    }
    /**
     * Identify if OS is Android
     * @return
     */
    static boolean isAndroid() {
        return getOS() == OSType.ANDROID
    }
    /**
     * Get OS Version
     */
    static String getOSVersion() {
        return System.getProperty("os.version")
    }
    /**
     * Returns OS Architecture
     */
    static String getOSArch() {
        return System.getProperty("os.arch")
    }

    /**
     * New Line by OS
     */
    enum NewLine {
        LINUX("\n"), IOS("\n"), WIN("\r")
        final String value
        NewLine(String nl) { value = nl }
    }

    /**
     * Return NewLine depending on on OS
     * @return
     */
    static private String getNewLineForCurrentOS() {
        return System.lineSeparator()
    }
}
