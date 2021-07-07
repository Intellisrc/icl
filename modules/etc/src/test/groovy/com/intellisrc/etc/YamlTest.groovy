package com.intellisrc.etc

import groovy.transform.CompileStatic
import spock.lang.Specification
import org.yaml.snakeyaml.Yaml

/**
 * @since 2021/06/30.
 */
@CompileStatic
class YamlTest extends Specification {
    def "List to yaml"() {
        setup :
            Yaml yaml = new Yaml()
            List<String> list = ["a","b","c"]
            String res = yaml.dump(list)
        when :
            List<String> list1 = yaml.load(res) as List
            list1.each {
                println "> $it"
            }
        then:
            assert list == list1
    }
    def "List to yaml 2"() {
        setup :
            Yaml yaml = new Yaml()
            List<Integer> list = [1,2,3,4,5]
            String res = yaml.dump(list)
        when :
            List<Integer> list1 = yaml.load(res) as List
            list1.each {
                println "> $it"
            }
        then:
            assert list == list1
    }
    def "Map to yaml"() {
        setup :
            Yaml yaml = new Yaml()
            Map<String, Integer> list = [a : 1, b : 2, c : 3, d : 4, e : 5]
            String res = yaml.dump(list)
        when :
            Map<String, Integer> list1 = yaml.load(res) as Map
            list1.each {
                println "> ${it.key} -> ${it.value}"
            }
        then:
            assert list == list1
    }
}
