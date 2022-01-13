package com.intellisrc.etc

import com.intellisrc.core.SysClock
import com.intellisrc.etc.config.AutoConfig
import org.assertj.core.util.Files
import spock.lang.Specification
import java.time.LocalDateTime

import static com.intellisrc.etc.config.ConfigAuto.*

/**
 * @since 2021/12/20.
 */
class ConfigAutoTest extends Specification {
    static enum Enum {
        ONE, TWO, THREE
    }
    @AutoConfig
    static class Test {
        @AutoConfig
        public static boolean bool        = false
        @AutoConfig
        public static Integer integer     = 0
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
        public static File file           = Files.newTemporaryFile()
        @AutoConfig
        public static Inet4Address inet   = "0.0.0.0".toInet4Address()
        @AutoConfig
        public static URI uri             = "http://example.com".toURI()
        @AutoConfig
        public static LocalDateTime date  = SysClock.now
    }
    def "Objects should be able to detect changes"() {
        setup :
            List<BasicStorage> storageList = Test.declaredFields.findAll {
                !it.synthetic
            }.collect {
                new BasicStorage(it)
            }
            Test.bool = true
            Test.integer = 1
            Test.string = "b"
            Test.list1 = [1]
            Test.list2 << 1
            Test.list3[0] = 1
            Test.map1 = [ b : 2 ]
            Test.map2.b = 2
            Test.map3.a = 2
            Test.num = Enum.TWO
            Test.file = Files.newTemporaryFile()
            Test.inet = "1.1.1.1".toInet4Address()
            Test.uri = "https://example.com/index.html".toURI()
            Test.date = Test.date.plusDays(2)
        expect:
            storageList.each {
                assert it.changed : "Object didn't change: " + it.field.name
            }
    }
}
