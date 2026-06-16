package com.intellisrc.etc.auto_test

import com.intellisrc.core.SysClock
import com.intellisrc.etc.config.AutoConfig

import java.time.LocalDateTime

/**
 * @since 2026/06/16.
 */
@AutoConfig
class Test {
    @AutoConfig
    public static boolean bool        = false
    @AutoConfig
    public static Integer integer     = 4
    @AutoConfig
    public static String string       = "a"
    @AutoConfig
    public static List list1          = [0]
    @AutoConfig
    public static List list2          = [0]
    @AutoConfig
    public static List list3          = [0]
    @AutoConfig
    public static Map map1            = [ a : 1 ]
    @AutoConfig
    public static Map map2            = [ a : 1 ]
    @AutoConfig
    public static Map map3            = [ a : 1 ]
    @AutoConfig
    public static Enum num            = Enum.ONE
    @AutoConfig
    public static File file           = File.createTempFile("auto", "config")
    @AutoConfig
    public static Inet4Address inet   = "0.0.0.0".toInet4Address()
    @AutoConfig
    public static URI uri             = "http://example.com".toURI()
    @AutoConfig
    public static LocalDateTime date  = SysClock.now
}

