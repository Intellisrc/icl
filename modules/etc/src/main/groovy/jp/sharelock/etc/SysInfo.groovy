package jp.sharelock.etc
/**
 * @since 1/2/17.
 */
@groovy.transform.CompileStatic
class SysInfo {
    static final String LOG_TAG = SysInfo.simpleName
    static final enum OSType {
        UNKNOWN, LINUX, WINDOWS, IOS, SOLARIS, ANDROID, PRINTER, NAS, SWITCH, ROUTER, CAMERA, TV
    }
    /**
     * Returns OS in which the system is installed
     * @param osStr : if not set will read it from localhost
     */
    static OSType getOS(String osStr = System.getProperty("os.name")) {
        def os_type = OSType.UNKNOWN
        switch(osStr) {

            case ~/(?i).*(printer|copier).*/ : os_type = OSType.PRINTER; break
            case ~/(?i).*nas\sdevice.*/ : os_type = OSType.NAS; break
            case ~/(?i).*switch.*/ : os_type = OSType.SWITCH; break
            case ~/(?i).*router.*/ : os_type = OSType.ROUTER; break
            case ~/(?i).*camera.*/ : os_type = OSType.CAMERA; break
            case ~/(?i).*\stv.*/ : os_type = OSType.TV; break

            case ~/(?i).*mac.*/ : os_type = OSType.IOS; break
            case ~/(?i).*android.*/ : os_type = OSType.ANDROID; break
            case ~/(?i).*(nix|nux|aix).*/ : os_type = OSType.LINUX; break
            case ~/(?i).*sunos.*/ : os_type = OSType.SOLARIS; break
            case ~/(?i).*win.*/ : os_type = OSType.WINDOWS; break
            case ~/(?i).*version.*service.*pack.*/ : os_type = OSType.WINDOWS; break
            default:
                //We can't use Log.d() here as that function call this one
                // and enter in an infinite loop
                println LOG_TAG + "> Not Found: $osStr"
        }
        return os_type
    }
    /**
     * Returns a path in which can be written. It will try
     * current path first, then temporally directory
     */
    static String getWritablePath() {
        def usrDir = System.getProperty("user.dir") + File.separator
        if(!new File(usrDir).canWrite()) {
            def tmpDir = System.getProperty("java.io.tmpdir")
            if (!tmpDir.endsWith(File.separator)) {
                tmpDir += File.separator
            }
            usrDir = tmpDir
        }
        return usrDir
    }
    /**
     * Identify if OS is Windows
     * @return
     */
    static isWindows() {
        return getOS() == OSType.WINDOWS
    }
    /**
     * Identify if OS is Linux (not Android)
     * @return
     */
    static isCoreLinux() {
        return getOS() == OSType.LINUX
    }
    /**
     * Identify if OS is any Linux including Android
     * @return
     */
    static isAnyLinux() {
        return getOS() == OSType.ANDROID || getOS() == OSType.LINUX
    }
    /**
     * Identify if OS is MacOS or iOS
     * @return
     */
    static isMac() {
        return getOS() == OSType.IOS
    }
    /**
     * Identify if OS is Android
     * @return
     */
    static isAndroid() {
        return getOS() == OSType.ANDROID
    }
    /**
     * Get OS Version
     */
    static getOSVersion() {
        return System.getProperty("os.version")
    }
    /**
     * Returns OS Architecture
     */
    static getOSArch() {
        return System.getProperty("os.arch")
    }
    /**
     * Return the User's home directory
     * @return
     */
    static getHomeDir() {
        return System.properties.'user.dir'
    }
    /**
     * Get the % of free RAM memory
     */
    static getRAMPct() {
        /*def bean = ManagementFactory.getOperatingSystemMXBean() <-- This is static and can not be assigned
        return bean.freePhysicalMemorySize / bean.totalPhysicalMemorySize * 100*/
    }
    /**
     * Get the % of CPU used
     */
    static getCPUPct() {
        /*def bean = ManagementFactory.getOperatingSystemMXBean()
        return bean.getSystemCpuLoad() * 100*/
    }
}
