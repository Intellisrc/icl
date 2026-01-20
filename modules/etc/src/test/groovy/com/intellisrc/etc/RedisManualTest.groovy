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

        Redis redis3 = new Redis("adv", ":")
        redis3.set("list", [1,2,3,4,5,6])

        redis.clear() // Previous prefix (should not affect "adv" prefix
        assert redis3.get("list",[]).collect { it as int }.contains(3)

        // will be converted to hset so values become string (numeric)
        // this is done by redis, so no much we can do
        redis3.set("map",[ok: false])
        assert redis3.get("map",[:]).ok == "0"

        redis3.set("url","http://localhost".toURL())
        assert redis3.get("url").toURL().host == "localhost"

        redis3.set("num", 30000)
        assert redis3.get("num", 0) == 30000

        redis3.set("dbl", 100.4d)
        assert redis3.get("dbl", 0d) == 100.4d

        redis3.clear()
        assert redis3.keys.empty

        // In these cases, types will be preserved as List and Map will be stored as YAML
        Redis redis4 = new Redis("types",".", true)
        redis4.set("list", [1,2,3,4,5,6])
        assert redis4.get("list",[]).contains(3)

        redis4.set("map",[ok: false])
        assert redis4.get("map",[:]).ok == false

        redis4.clear()
        assert redis4.keys.empty

        println "✅ Test finished correctly"
        Redis.quit()
    }
}
