package com.intellisrc.etc

import groovy.transform.CompileStatic
import spock.lang.Specification

/**
 * @since 2021/06/30.
 */
@CompileStatic
class YAMLTest extends Specification {
    def "List to yaml"() {
        setup :
            List<String> list = ["a","b","c"]
            String res = YAML.encode(list)
        when :
            List<String> list1 = YAML.decode(res) as List
            list1.each {
                println "> $it"
            }
        then:
            assert list == list1
    }
    def "List to yaml 2"() {
        setup :
            List<Integer> list = [1,2,3,4,5]
            String res = YAML.encode(list)
        when :
            List<Integer> list1 = YAML.decode(res) as List
            list1.each {
                println "> $it"
            }
        then:
            assert list == list1
    }
    def "Map to yaml"() {
        setup :
            Map<String, Integer> list = [a : 1, b : 2, c : 3, d : 4, e : 5]
            String res = YAML.encode(list)
        when :
            Map<String, Integer> list1 = YAML.decode(res) as Map
            list1.each {
                println "> ${it.key} -> ${it.value}"
            }
        then:
            assert list == list1
    }
}
