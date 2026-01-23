package com.intellisrc.etc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.props.StringProperties
import groovy.transform.CompileStatic
import redis.clients.jedis.ConnectionPoolConfig
import redis.clients.jedis.RedisClient
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.exceptions.JedisDataException

import java.time.Duration

/**
 * This class simplifies the use of a single server of Redis and implements most common commands (using Jedis).
 *
 * If you need advanced commands (e.g. scan, zadd, rpushx, etc), byte support or to connect to multiple servers, use Jedis directly.
 *
 * This implementation use a single instance of RedisClient (to use a pool of connections)
 * and having multiple Redis pool will add complexity to this class (and it is rarely needed).
 */
@CompileStatic
class Redis extends StringProperties {
    static final int timeBetweenEvictionRuns = Config.any.get("redis.check.interval", 5) //Seconds
    static final int port = Config.any.get("redis.port", 6379)
    static final boolean warn = Config.any.get("redis.warn", true)
    static final String host = Config.any.get("redis.host", "localhost")
    static final ConnectionPoolConfig jedisPool = new ConnectionPoolConfig()

    static RedisClient jedis
    static boolean running = false
    static final String OK = "ok"
    /**
     * Close all connections to Redis
     */
    static void quit() {
        jedis?.close()
        running = false
        jedis = null
    }

    protected preserveTypes = false

    /**
     * Constructor
     * @param keyPrefix : If used all keys will be prefixed with it (useful to group keys)
     * @param keyPrefixSeparator : separator used, for example: redis.set("key","x") will be
     *                             stored as: prefix.key (if separator is '.')
     * @param preserveTypes : if true, Map and Collection objects will be stored as YAML.
     *                        While types will be preserved (not converted into strings),
     *                        performance will be reduced due to the conversion overhead.
     */
    Redis(String keyPrefix = "", String keyPrefixSeparator = ".", boolean preserveTypes = false) {
        super(keyPrefix, keyPrefixSeparator)
        this.preserveTypes = preserveTypes
        if(! running) {
            jedisPool.setTestWhileIdle(true)
            jedisPool.setTestOnBorrow(true)
            jedisPool.setTimeBetweenEvictionRuns(Duration.ofSeconds(timeBetweenEvictionRuns))
            jedis = RedisClient.builder().hostAndPort(host, port).poolConfig(jedisPool).build()
            running = true
            if(warn) {
                Log.w("Using preserveTypes with hget is a performance killer. Disable warning with: redis.warn = false")
            }
        }
    }

    // Redis direct methods:
    private static <T> T withJedis(Closure<T> action) {
        assert running : "Redis was closed"
        try {
            return action.call()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection", jce)
            return null
        }
    }

    // Redis direct methods:
    private <T> T withJedis(String key, T defaultValue, Closure<T> action) {
        assert running : "Redis was closed"
        try {
            return action.call(getFullKey(key))
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection (key: %s)", key, jce)
            return defaultValue
        } catch (JedisDataException jde) {
            Log.w("Data Exception on key [%s]", key, jde)
            return defaultValue
        }
    }

    // Special method for mget
    private <T> T withJedis(List<String> keys, T defaultValue, Closure<T> action) {
        assert running : "Redis was closed"
        try {
            return action.call(keys.collect { getFullKey(it) })
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection", jce)
            return defaultValue
        }
    }

    List<String> get(List<String> keys) {
        withJedis(keys, [] as List<String>) { List<String> ks ->
            String[] kk = ks.toArray(new String[ks.size()])
            return jedis.mget(kk)
        }
    }

    @Override
    String get(String key, String defVal = "") {
        withJedis(key, defVal) {
            String k ->
                String type = jedis.type(k)
                String value = switch (type) {
                    case "list"     -> "[" + getList(k).join(",") + "]"
                    case "hash"     -> "{" + getMap(k).collect {it.key.toString() + ":" + it.value.toString() }.join(",") +"}"
                    case "string"   -> jedis.get(k)
                    default         -> defVal
                }
                return value == null ? defVal : value
        }
    }

    @Override
    List get(String key, List defVal) {
        withJedis(key, defVal) {
            String k ->
                List vals = []
                if(preserveTypes) {
                    String list = jedis.get(k)
                    if(list) {
                        vals = YAML.decode(list) as List
                    }
                } else {
                    vals = jedis.lrange(k, 0, -1)
                }
                return vals.empty ? defVal : vals
        }
    }

    @Override
    Set get(String key, Set defVal) {
        withJedis(key, defVal) {
            String k ->
                Set vals = []
                if(preserveTypes) {
                    String list = jedis.get(k)
                    if(list) {
                        vals = YAML.decode(list) as Set
                    }
                } else {
                    vals = jedis.smembers(k)
                }
                return vals.empty ? defVal : vals
        }
    }

    @Override
    Map get(String key, Map defVal) {
        withJedis(key, defVal) {
            String k ->
                Map<String, String> vals = [:]
                if(preserveTypes) {
                    String map = jedis.get(k)
                    if(map) {
                        vals = YAML.decode(map) as Map
                    }
                } else {
                    vals = jedis.hgetAll(k)
                }
                return vals.isEmpty() ? defVal : vals
        }
    }

    @Override
    Set<String> getKeys() {
        withJedis {
            Set<String> vals = jedis.keys((prefix ? prefix + prefixSeparator : "") + "*").toSet()
            if(prefix) {
                vals = vals.collect {it.substring((prefix + prefixSeparator).length()) }.toSet()
            }
            return vals
        }
    }

    @Override
    boolean set(String key, String value) {
        withJedis(key, false) {
            String k ->
                return value == null ?
                    jedis.del(k) > 0 :
                    jedis.set(k, value).toLowerCase() == OK
        }
    }

    /**
     * This method is different from StringProperties as it doesn't store the whole list
     * as string, but as a list inside Redis (if preserveTypes == false [default])
     * @param key
     * @param list
     * @return
     */
    @Override
    boolean set(String key, Collection list) {
        withJedis(key, false) {
            String k ->
                jedis.del(k)
                return preserveTypes ?
                    jedis.set(k, YAML.encode(list)) == OK :
                    switch (list) {
                        case Set  -> jedis.sadd(k, listToArray(list))
                        default   -> list.sum { jedis.rpush(k, it.toString()) }
                    }
        }
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
        withJedis(key, false) {
            String k ->
                return preserveTypes ?
                    jedis.set(k, YAML.encode(map)) == OK :
                    jedis.hset(k, map) > 0
        }
    }

    /**
     * Remove all keys
     * @return
     */
    @Override
    boolean clear() {
        withJedis {
            return prefix ?
                keys.every {delete(it) } :
                jedis.flushAll().toLowerCase() == OK
        }
    }

    long hset(String key, String field, Object value) {
        withJedis(key, 0L) {
            String k ->
                long ret
                if (preserveTypes) {
                    Map map = get(key, [:]) // Internal use, we send "key"
                    map[field] = value
                    ret = set(key, map) ? 1 : 0
                } else {
                    ret = jedis.hset(k, field, value.toString())
                }
                return ret
        }
    }

    Object hget(String key, String field) {
        withJedis(key, "") {
            String k ->
                Object ret
                if(preserveTypes) {
                    Map map = get(key, [:])
                    ret = map[field]
                } else {
                    ret = jedis.hget(k, field)
                }
                return ret ?: ""
        }
    }
    /**
     * Delete a key
     * @param key
     * @return
     */
    @Override
    boolean delete(String key) {
        withJedis(key, false) { String k -> jedis.del(k) > 0 }
    }

    @Override
    boolean exists(String key) {
        withJedis(key, false) { String k -> jedis.exists(k) }
    }

    String type(String key) {
        withJedis(key, "") { String k -> jedis.type(k) }
    }

    String rename(String key, String newKey) {
        withJedis(key, "") { String k -> jedis.rename(k, getFullKey(newKey)) }
    }

    long incr(String key) {
        withJedis(key, 0L) { String k -> jedis.incr(k) }
    }

    long incrBy(String key, long by) {
        withJedis(key, 0L) { String k -> jedis.incrBy(k, by) }
    }
    double incrBy(String key, double by) {
        withJedis(key, 0d) { String k -> jedis.incrByFloat(k, by) }
    }

    long len(String key) {
        withJedis(key, 0L) {
            String k ->
                if(preserveTypes) {
                    Object data = YAML.decode(jedis.get(k))
                    if(data) {
                        return switch (data) {
                            case Set    -> (data as Set).size()
                            case List   -> (data as List).size()
                            case Map    -> (data as Map).keySet().size()
                            default -> 1L
                        }
                    } else {
                        return 0L
                    }
                }
                return switch (type(key)) {
                    case "set"  -> jedis.scard(k)
                    case "list" -> jedis.llen(k)
                    case "hash" -> jedis.hlen(k)
                    case "none" -> 0L
                    // "string", et al.
                    default -> 1L
                }

        }
    }

    List lrange(String key, long start, long end) {
        withJedis(key, [] as List) {
            String k ->
                preserveTypes ?
                    get(key, []).subList(start as int, end as int) :    //internal use, we use "key"
                    jedis.lrange(k, start, end)
        }
    }

    long hdel(String key, String... field) {
        withJedis(key, 0L) {
            String k ->
                long ret = 0L
                if(preserveTypes) {
                    Map map = get(key, [:])
                    field.each {
                        if(map.containsKey(it)) {
                            map.remove(it)
                            ret++
                        }
                    }
                    set(key, map)
                } else {
                    ret = jedis.hdel(k, field)
                }
                return ret
        }
    }

    long expire(String key, long seconds) {
        withJedis(key, 0L) { String k -> jedis.expire(k, seconds) }
    }

    long ttl(String key) {
        withJedis(key, 0L) { String k -> jedis.ttl(k) }
    }

    long persist(String key) {
        withJedis(key, 0L) { String k -> jedis.persist(k) }
    }

    boolean exists(String key, Object member) {
        withJedis(key, false) {
            String k ->
                if(preserveTypes) {
                    Object data = YAML.decode(jedis.get(k))
                    return switch (data) {
                        case Set    -> (data as Set).contains(member)
                        case List   -> (data as List).contains(member)
                        case Map    -> (data as Map).containsKey(member)
                        default     -> false
                    }
                } else {
                    return switch (type(key)) {
                        case "list" -> jedis.lpos(k, member.toString()) !== null
                        case "set"  -> jedis.sismember(k, member.toString())
                        case "hash" -> jedis.hexists(k, member.toString())
                        case "none" -> false
                        default -> jedis.exists(key)
                    }
                }
        }
    }

    long lpush(String key, Object... items) {
        withJedis(key, 0L) {
            String k ->
                long ret
                if(preserveTypes) {
                    List list = get(key, [])
                    list.addAll(0, items)
                    set(key, list)
                    ret = items.length
                } else {
                    ret = jedis.lpush(k, toStringArray(items))
                }
                return ret
        }
    }

    long rpush(String key, Object... items) {
        withJedis(key, 0L) {
            String k ->
                long ret
                if(preserveTypes) {
                    List list = get(key, [])
                    list.addAll(items)
                    set(key, list)
                    ret = items.length
                } else {
                    ret = jedis.rpush(k, toStringArray(items))
                }
                return ret
        }
    }

    Object lpop(String key) {
        withJedis(key, "") {
            String k ->
                Object ret = ""
                if(preserveTypes) {
                    List list = get(key, [])
                    if(!list.empty) {
                        ret = list.pop()
                        set(key, list)
                    }
                } else {
                    ret = jedis.lpop(k)
                }
                return ret
        }
    }

    Object rpop(String key) {
        withJedis(key, "") {
            String k ->
                Object ret = ""
                if(preserveTypes) {
                    List list = get(key, [])
                    if(!list.empty) {
                        ret = list.removeLast()
                        set(key, list)
                    }
                } else {
                    ret = jedis.rpop(k)
                }
                return ret
        }
    }

    private static String[] toStringArray(Object... args) {
        if (args instanceof String[]) {
            return (String[]) args
        }
        String[] out = new String[args.length]
        for (int i = 0; i < args.length; i++) {
            out[i] = (String) args[i]   // fails fast if not String
        }
        return out
    }

    private static String[] listToArray(Collection list) {
        return list.collect { it.toString() }.toArray(new String[0]) as String[]
    }
}
