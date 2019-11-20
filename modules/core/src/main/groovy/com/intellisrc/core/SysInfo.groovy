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
        switch(osStr) {
            case ~/(?i).*version.*service.*pack.*/ :
            case ~/.*(?i)win.*/ : osType = OSType.WINDOWS; break
            case ~/.*(?i)android.*/ : osType = OSType.ANDROID; break
            case ~/.*(?i)(nix|nux|aix).*/ : osType = OSType.LINUX; break
            case ~/(?i).*mac.*/ : osType = OSType.IOS; break
            default:
                //We can't use Log.d() here as that function call this one
                // and enter in an infinite loop
                println "> Not Found: $osStr"
        }
        return osType
    }
    /**
     * Returns a path in which can be written. It will try
     * current path first, then temporally directory
     */
    static String getWritablePath() {
        def usrDir = getUserDir()
        if(!new File(usrDir).canWrite()) {
            usrDir = getTempDir()
        }
        return usrDir
    }
    /**
     * Return the User's home directory
     * @return
     */
    static String getHomeDir() {
        return System.getProperty('user.home') + File.separator
    }
    /**
     * Returns the root path of the application
     * @return
     */
    static String getUserDir() {
        return System.getProperty("user.dir") + File.separator
    }
    /**
     * Returns the temporally directory path
     * @return
     */
    static String getTempDir() {
        def tmpDir = System.getProperty("java.io.tmpdir")
        if (!tmpDir.endsWith(File.separator)) {
            tmpDir += File.separator
        }
        return tmpDir
    }
    
    /**
     * Alternative to `new File()` which is more flexible
     * Examples:
     *
     * Convert path to home directory:
     * SysInfo.getFile("~/system", "file.txt")
     *
     * Accepts multiple levels and automatically detect when reading from user directory:
     * SysInfo.getFile("resources", "images", "thumbs", "logo.jpg")
     *
     * It accepts File objects and String as well:
     * SysInfo.getFile(someDir, "directory", "file.txt")
     *
     * @param self
     * @param path
     * @return
     */
    static File getFile(Object... path) {
        File pathFile
        boolean isFirstInPath = true
        path.each {
            def part ->
                String pathPart
                switch (part) {
                    case File:
                        if (isFirstInPath) {
                            pathPart = (part as File).absolutePath
                        } else {
                            pathPart = (part as File).name
                        }
                        break
                    default:
                        pathPart = part.toString()
                        break
                }
                if (isFirstInPath) {
                    switch (pathPart) {
                        case ~/^\/.*$/:
                            pathFile = new File(pathPart)
                            break
                        case ~/^~.*$/:
                            pathFile = new File(homeDir, pathPart.replace('~/', ''))
                            break
                        default:
                            pathFile = new File(userDir, pathPart)
                    }
                } else {
                    pathFile = new File(pathFile, pathPart)
                }
                isFirstInPath = false
        }
        return pathFile
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
}
