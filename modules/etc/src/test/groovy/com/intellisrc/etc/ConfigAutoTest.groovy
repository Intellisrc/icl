package com.intellisrc.etc

import com.intellisrc.etc.config.ConfigAuto
import com.intellisrc.etc.auto_test.*
import spock.lang.Specification

import static com.intellisrc.etc.config.ConfigAuto.BasicStorage

/**
 * @since 2021/12/20.
 */
class ConfigAutoTest extends Specification {
    def "Default values should be correct"() {
        setup:
            ConfigAuto configAuto = new ConfigAuto("com.intellisrc.etc.auto_test")
            Map initial = configAuto.initialValues
        expect:
            assert (initial["test.integer"] as int) == 4
            assert (initial["test.num"] as Enum) == Enum.ONE
            assert (initial["test.file"] as File).name.contains("auto")
            assert initial["test.uri"].toString().contains("example.com")
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
            Test.file = File.createTempFile("test","test")
            Test.inet = "1.1.1.1".toInet4Address()
            Test.uri = "https://example.com/index.html".toURI()
            Test.date = Test.date.plusDays(2)
        expect:
            storageList.each {
                assert it.changed : "Object didn't change: " + it.field.name
            }
    }
}
