package com.intellisrc.etc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.props.StringProperties
import groovy.transform.CompileStatic
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.RedisClient
import redis.clients.jedis.exceptions.JedisConnectionException

import java.time.Duration

/**
 * This class simplifies the use of Jedis/Redis when connecting to a single server
 * For simplicity, if you need to connect to more than one redis server, use Jedis directly.
 * The reason is because we use a single instance of RedisClient (to use a pool of connections)
 * and having multiple Redis pool will add complexity to this class (and it is rarely needed).
 */
@CompileStatic
class Redis extends StringProperties {
    static final int timeBetweenEvictionRuns = Config.any.get("redis.check.interval", 5) //Seconds
    static final int port = Config.any.get("redis.port", 6379)
    static final String host = Config.any.get("redis.host", "localhost")
    static final ConnectionPoolConfig jedisPool = new ConnectionPoolConfig()

    static RedisClient jedis
    static boolean running = false
    /**
     * Close all connections to Redis
     */
    static void quit() {
        jedis?.close()
        running = false
        jedis = null
    }

    /**
     * Constructor
     * @param keyPrefix : If used all keys will be prefixed with it (useful to group keys)
     */
    Redis(String keyPrefix = "", String keyPrefixSeparator = ".") {
        super(keyPrefix, keyPrefixSeparator)
        if(! running) {
            jedisPool.setTestWhileIdle(true)
            jedisPool.setTestOnBorrow(true)
            jedisPool.setTimeBetweenEvictionRuns(Duration.ofSeconds(timeBetweenEvictionRuns))
            jedis = RedisClient.builder().hostAndPort(host, port).poolConfig(jedisPool).build()
            running = true
        }
    }

    @Override
    String get(String key, String defVal) {
        assert running : "Redis was closed"
        String value = ""
        try {
            String type = jedis.type(getFullKey(key))
            switch (type) {
                case "list":
                    value = "[" + getList(key).join(",") + "]"
                    break
                case "hash":
                    value = "{" + getMap(key).collect {it.key.toString() + ":" + it.value.toString() }.join(",") +"}"
                    break
                case "string":
                    value = jedis.get(getFullKey(key))
                    break
                default:
                    value = "[$type]"
            }
            if(value == null) {
                value = defVal
            }
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return value
    }

    @Override
    List get(String key, List defVal) {
        assert running : "Redis was closed"
        List<String> vals = []
        try {
            vals = jedis.lrange(getFullKey(key), 0, -1)
            if(vals.empty) {
                vals = defVal
            }
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }

    @Override
    Map get(String key, Map defVal) {
        assert running : "Redis was closed"
        Map<String, String> vals = [:]
        try {
            vals = jedis.hgetAll(getFullKey(key))
            if(vals.keySet().empty) {
                vals = defVal
            }
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }

    @Override
    boolean exists(String key) {
        assert running : "Redis was closed"
        boolean exists = false
        try {
            exists = jedis.exists(getFullKey(key))
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return exists
    }

    @Override
    Set<String> getKeys() {
        assert running : "Redis was closed"
        Set<String> vals = []
        try {
            vals = jedis.keys((prefix ? prefix + prefixSeparator : "") + "*").toSet()
            if(prefix) {
                vals = vals.collect {it.substring((prefix + prefixSeparator).length()) }.toSet()
            }
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }

    @Override
    boolean set(String key, String value) {
        assert running : "Redis was closed"
        boolean ok = false
        try {
            if(value == null) {
                ok = jedis.del(getFullKey(key)) > 0
            } else {
                ok = jedis.set(getFullKey(key), value).toLowerCase() == "ok"
            }
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return ok
    }

    /**
     * This method is different from StringProperties as it doesn't store the whole list
     * as string, but as a list inside Redis
     * @param key
     * @param list
     * @return
     */
    @Override
    boolean set(String key, Collection list) {
        assert running : "Redis was closed"
        boolean ok = false
        try {
            jedis.del(getFullKey(key))
            list.each {
                jedis.rpush(getFullKey(key), it.toString())
            }
            ok = true
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return ok
    }

    /**
     * This method is different from StringProperties as it doesn't store the whole map
     * as string, but as a hash inside Redis
     * @param key
     * @param map
     * @return
     */
    @Override
    boolean set(String key, Map map) {
        assert running : "Redis was closed"
        boolean ok = false
        try {
            ok = jedis.hset(getFullKey(key), map) > 0
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return ok
    }

    /**
     * Delete a key
     * @param key
     * @return
     */
    @Override
    boolean delete(String key) {
        assert running : "Redis was closed"
        boolean ok = false
        try {
            ok = jedis.del(getFullKey(key)) > 0
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return ok
    }

    /**
     * Remove all keys
     * @return
     */
    @Override
    boolean clear() {
        assert running : "Redis was closed"
        boolean deleted = false
        try {
            if(prefix) {
                deleted = keys.every {
                    delete(it)
                }
            } else {
                deleted = jedis.flushAll().toLowerCase() == "ok"
            }
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return deleted
    }
}
