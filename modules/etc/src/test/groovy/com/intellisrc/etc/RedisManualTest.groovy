package com.intellisrc.etc

import groovy.transform.CompileStatic

/**
 * This class is to test Redis
 * As it requires redis-server, we test it manually
 * @since 2026/01/20.
 */
@CompileStatic
class RedisManualTest {
    static void main(String[] args) {
        Redis redis = new Redis("test",":")
        redis.set("hello", "world")
        assert redis.get("hello") == "world"

        Redis redis2 = new Redis("test")
        redis2.delete("hello")
        assert ! redis2.exists("hello")

        println "✅ Test finished correctly"
        Redis.quit()
    }
}
