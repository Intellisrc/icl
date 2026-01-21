package com.intellisrc.etc

import com.intellisrc.core.Millis
import groovy.transform.CompileStatic

/**
 * This class is to test Redis
 * As it requires redis-server, we test it manually
 * @since 2026/01/20.
 */
@CompileStatic
class RedisManualTest {
    static void main(String[] args) {
        // Super simple test:
        Redis redis = new Redis("test",":")
        redis.set("hello", "world")
        assert redis.get("hello") == "world"

        // Test that other instance with the same prefix should access the same key:
        Redis redis2 = new Redis("test")
        redis2.delete("hello")
        assert ! redis2.exists("hello")

        // Test other cases:
        Redis redis3 = new Redis("adv", ":")
        redis3.set("list", [1,2,3,4,5,6])
        assert redis3.lrange("list", 2,3).contains('3')
        assert redis3.lpush("list", "0")
        assert redis3.lpop("list") == '0'
        assert redis3.rpush("list", "9")
        assert redis3.rpop("list") == '9'

        redis.clear() // Previous prefix (should not affect "adv" prefix (note we are calling 'redis' and not 'redis3')
        assert redis3.get("list",[]).collect { it as int }.contains(3)
        redis3.rename("list","array")
        assert redis3.exists("array")
        assert ! redis3.exists("list")

        // will be converted to hset so values become string (numeric)
        // this is done by redis, so no much we can do
        redis3.set("map",[ok: false])
        assert redis3.get("map",[:]).ok == "0"
        assert redis3.type("map") == "hash" //Stored as hash
        // Adding with hset:
        assert redis3.hset("map", "good",  true)
        assert redis3.hget("map", "good") == "true"
        assert redis3.hdel("map", "good")
        assert redis3.hget("map", "good") == ""

        redis3.set("url","http://localhost".toURL())
        assert redis3.getURL("url").get().host == "localhost"

        redis3.set("num", 30000)
        assert redis3.get("num", 0) == 30000
        assert redis3.incr("num") == 30001

        redis3.set("dbl", 100.4d)
        assert redis3.get("dbl", 0d) == 100.4d

        assert redis3.keys.size() == 5
        redis3.clear()
        assert redis3.keys.empty

        ///////// With preserveTypes ON ////////////////
        // In these cases, types will be preserved as List and Map will be stored as YAML
        Redis redis4 = new Redis("types",".", true)
        redis4.set("list", [1,2,3,4,5,6])
        assert redis4.get("list",[]).contains(3)
        assert redis4.lrange("list", 2,3).contains(3)
        assert redis4.lpush("list", 0)
        assert redis4.lpop("list") == 0
        assert redis4.rpush("list", 9)
        assert redis4.rpop("list") == 9

        redis4.set("map",[ok: false])
        assert redis4.get("map",[:]).ok == false
        assert redis4.type("map") == "string" //Stored as string

        redis4.hset("map", "score", 100)
        redis4.hset("map", "name", "Wong")
        assert redis4.hget("map","score") == 100
        assert redis4.hget("map","name") == "Wong"
        assert redis4.hdel("map", "name")
        assert redis4.hget("map", "name") == ""

        ////// Expiration /////////////
        redis.set("boom", "later")
        redis.set("ping", "pong")
        assert redis.expire("boom", 3)
        assert redis.expire("ping", 3)
        assert redis.ttl("boom") == 3
        assert redis.get("boom") == "later"
        assert redis.get("ping") == "pong"
        assert redis.persist("ping")

        print "Waiting 5 seconds to expire..."
        sleep(Millis.SECOND_5)
        assert redis.get("boom") == ""
        assert redis.get("ping") == "pong"

        redis4.clear()
        assert redis4.keys.empty

        println "✅ Test finished correctly"
        Redis.quit()
    }
}
