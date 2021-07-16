package com.intellisrc.etc.config

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException

/**
 * Implementation of SavedProperties for Redis
 * @since 2019/12/09.
 */
@CompileStatic
class SavedPropertiesRedis extends SavedProperties {
    SavedPropertiesRedis(String root) {
        super(root)
    }
    void set(String key, String value) {
        try {
            Jedis jedis = new Jedis()
            jedis.hset(propKey, key, value)
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
    }
    void set(String key, Collection<String> list) {
        try {
            Jedis jedis = new Jedis()
            jedis.del(propKey + "." + key)
            list.each {
                jedis.rpush(propKey + "." + key, it)
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
    }
    void set(String key, Map<String, String> map) {
        try {
            Jedis jedis = new Jedis()
            jedis.hset(propKey + "." + key, map)
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
    }

    String get(String key, String defVal) {
        String value = ""
        try {
            Jedis jedis = new Jedis()
            value = jedis.hget(propKey, key)
            if(value == null) {
                value = defVal
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return value
    }
    Collection<String> get(String key, Collection<String> defVal) {
        Collection<String> vals = []
        try {
            Jedis jedis = new Jedis()
            vals = jedis.lrange(propKey + "." + key, 0, -1)
            if(vals.empty) {
                vals = defVal
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }
    Map<String, String> get(String key, Map<String, String> defVal) {
        Map<String, String> vals = [:]
        try {
            Jedis jedis = new Jedis()
            vals = jedis.hgetAll(propKey + "." + key)
            if(vals.keySet().empty) {
                vals = defVal
            }
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return vals
    }

    boolean exists(String key) {
        boolean exists = false
        try {
            Jedis jedis = new Jedis()
            exists = jedis.exists(propKey, key)
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return exists
    }
    
    Map<String, String> getAll() {
        Map<String, String> map = [:]
        try {
            Jedis jedis = new Jedis()
            map = jedis.hgetAll(propKey)
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return map
    }
    
    boolean delete(String key) {
        long deleted = 0
        try {
            Jedis jedis = new Jedis()
            deleted = jedis.hdel(propKey, key)
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return deleted > 0
    }
    
    boolean clear() {
        long deleted = 0
        try {
            Jedis jedis = new Jedis()
            deleted = jedis.del(propKey)
            jedis.close()
        } catch (JedisConnectionException jce) {
            Log.e("Exception in Jedis connection ", jce)
        }
        return deleted > 0
    }
}
