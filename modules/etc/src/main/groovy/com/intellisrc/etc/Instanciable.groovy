package com.intellisrc.etc

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.lang.reflect.ParameterizedType

/**
 * This is a "trait" which acts like an interface and an abstract class.
 * It provides a way to initialize parametrized types <T>
 *
 * This trait can be used in multiple parametrized types as well, for example
 * <code>
 * class Sample<A,B> implements Instanciable<A>, Instanciable<B> {
 *     ...
 *     A a = getParametrizedInstance()
 *     B b = getParametrizedInstance(1)
 *     ...
 * }
 *
 * </code>
 * @param <T>
 */
@CompileStatic
trait Instanciable<T> {
    T getParametrizedInstance(int index = 0) {
        T obj = null
        try {
            ParameterizedType parameterizedType = this.class.genericSuperclass as ParameterizedType
            Class type = (Class) parameterizedType.actualTypeArguments[index]
            obj = (T) type.getDeclaredConstructor().newInstance()
        } catch(Exception e) {
            Log.e("Unable to initialize parametrized instance: %s", this.class.simpleName, e)
        }
        return obj
    }
}