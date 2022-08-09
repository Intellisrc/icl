package com.intellisrc.etc

import com.intellisrc.core.Log
import com.intellisrc.core.props.StringProperties
import groovy.transform.CompileStatic
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException

@CompileStatic
class Redis extends StringProperties {
    /**
     * Constructor
     * @param keyPrefix : If used all keys will be prefixed with it (useful to group keys)
     */
    Redis(String keyPrefix = "", String keyPrefixSeparator = ".") {
        super(keyPrefix, keyPrefixSeparator)
    }

    @Override
    String get(String key, String defVal) {
        String value = ""
        try {
            Jedis jedis = new Jedis()
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
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return value
    }

    @Override
    List get(String key, List defVal) {
        List<String> vals = []
        try {
            Jedis jedis = new Jedis()
            vals = jedis.lrange(getFullKey(key), 0, -1)
            if(vals.empty) {
                vals = defVal
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }

    @Override
    Map get(String key, Map defVal) {
        Map<String, String> vals = [:]
        try {
            Jedis jedis = new Jedis()
            vals = jedis.hgetAll(getFullKey(key))
            if(vals.keySet().empty) {
                vals = defVal
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }

    @Override
    boolean exists(String key) {
        boolean exists = false
        try {
            Jedis jedis = new Jedis()
            exists = jedis.exists(getFullKey(key))
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return exists
    }

    @Override
    Set<String> getKeys() {
        Set<String> vals = []
        try {
            Jedis jedis = new Jedis()
            vals = jedis.keys((prefix ? prefix + prefixSeparator : "") + "*").toSet()
            jedis.close()
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
        boolean ok = false
        try {
            Jedis jedis = new Jedis()
            if(value == null) {
                ok = jedis.del(getFullKey(key)) > 0
            } else {
                ok = jedis.set(getFullKey(key), value).toLowerCase() == "ok"
            }
            jedis.close()
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
        boolean ok = false
        try {
            Jedis jedis = new Jedis()
            jedis.del(getFullKey(key))
            list.each {
                jedis.rpush(getFullKey(key), it.toString())
            }
            jedis.close()
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
        boolean ok = false
        try {
            Jedis jedis = new Jedis()
            ok = jedis.hset(getFullKey(key), map) > 0
            jedis.close()
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
        boolean ok = false
        try {
            Jedis jedis = new Jedis()
            ok = jedis.del(getFullKey(key)) > 0
            jedis.close()
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
        boolean deleted = false
        try {
            Jedis jedis = new Jedis()
            if(prefix) {
                deleted = keys.every {
                    delete(it)
                }
            } else {
                deleted = jedis.flushAll().toLowerCase() == "ok"
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return deleted
    }
}
