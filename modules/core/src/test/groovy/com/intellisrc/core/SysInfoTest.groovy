package com.intellisrc.core

import spock.lang.Specification

/**
 * Created by lepe on 17/02/23.
 */
class SysInfoTest extends Specification {
    def "OS detection"() {
        expect:
            SysInfo.getOS() != SysInfo.OS.UNKNOWN
    }
    /**
     * SysInfo.getFile() must generate the same path:
     */
    def "All paths must match - User dir"() {
        setup:
            File f1 = new File(new File(SysInfo.userDir, "directory"), "file.txt")
            File f2 = SysInfo.getFile("directory", "file.txt")
            File f3 = SysInfo.getFile(SysInfo.userDir, "directory", "file.txt")
        expect:
            assert f1.absolutePath == f2.absolutePath
            assert f2.absolutePath == f3.absolutePath
    }
    /**
     * SysInfo.getFile() must generate the same path:
     */
    def "All paths must match - Home dir"() {
        setup:
            File f1 = new File(new File(SysInfo.homeDir, "directory"), "file.txt")
            File f2 = SysInfo.getFile("~/directory", "file.txt")
            File f3 = SysInfo.getFile(SysInfo.homeDir, "directory", "file.txt")
        expect:
            assert f1.absolutePath == f2.absolutePath
            assert f2.absolutePath == f3.absolutePath
    }
}