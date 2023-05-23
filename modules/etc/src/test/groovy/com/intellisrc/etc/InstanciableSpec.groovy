package com.intellisrc.etc

import spock.lang.Ignore
import spock.lang.Specification

/**
 * @since 2023/01/27.
 */
class InstanciableSpec extends Specification {
    class Test {}
    class Sample<T> implements Instanciable<T> {
        T getClassType() {
            return getParametrizedInstance()
        }
    }
    @Ignore
    def "getParametrizedInstance should return new instance of provided class"() {
        given:
            Sample<Test> sample = new Sample<Test>()

        when:
            Test test = sample.getClassType()

        then:
            assert test != null
            assert test.class == Test
    }
    @Ignore
    def "getParametrizedInstance should return null if exception occurs"() {
        given:
            Sample<String> sample = new Sample<String>()

        when:
            sample.class.metaClass.getDeclaredConstructor = { -> throw new Exception() }
            Class clazz = sample.getClassType()

        then:
            clazz == null
    }
}