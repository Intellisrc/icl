package jp.sharelock.etc

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@groovy.transform.CompileStatic
/**
 * Manage Cache of objects
 * @author Alberto Lepe <lepe@sharelock.jp>
 * @param <V>
 *
 * GC_INTERVAL  : how many seconds to execute Garbage Collector. Set it to zero to disabled it (for any crazy reason)
 * extend       : if true, it will extend time upon access. If false, it will expire when the time is due (without changing expire value).
 */
class Cache<V> {
    interface onNotFound {
        V call()
    }
	int GC_INTERVAL = 120 //seconds
    boolean extend  = false

    private final int DEFAULT_TIMEOUT = 60 //seconds
    private class CacheObj {
        V       value
        long    expire
        private long length
        CacheObj(V obj, long time) {
            value = obj
            expire = time
            length = expire - new Date().time
        }
    }
    final ConcurrentMap<String, CacheObj> cache = new ConcurrentHashMap<>()

	/**
	 * Constructor
	 */
	Cache() {
        Thread.start {
            while(GC_INTERVAL > 0) {
                sleep(GC_INTERVAL * 1000)
                garbageCollect()
            }
        }
	}
	/**
	 * Returns true if cache is empty
	 * @return 
	 */
	boolean isEmpty() {
		return cache.isEmpty()
	}
	/**
	 * Returns true if key exists
	 * @param key
	 * @return 
	 */
    boolean exists(final String key) {
        return cache.containsKey(key) &&! expired(key)
    }
    /**
     * Return true if expired and remove it from cache
     * @param key
     * @return
     */
    private boolean expired(final String key) {
        def expired = true
        def obj = cache.get(key)
        def now = new Date().time
        if(obj) {
            if (obj.expire < now) {
                Log.d("Key [$key] expired.")
                del(key)
            } else {
                if(extend) {
                    obj.expire = now + obj.length
                }
                expired = false
            }
        }
        return expired
    }
	/**
	 * Returns value if found, or set object if not
	 * @param key
	 * @param default_val
	 * @return
	 */
    V get(final String key, onNotFound notFound = null, long time = DEFAULT_TIMEOUT) {
        V ret = null
        if(exists(key)) {
            ret = cache.get(key).value
			Log.v( "[$key] read from cache")
        } else {
            if(notFound) {
                ret = notFound.call()
				if(ret) {
					Log.v( "[$key] added to cache")
				}
            }
            set(key, ret, time)
        }
        return ret
    }

	/**
	 * Set value to key. If value is null, it will be removed
	 * @param key
	 * @param value 
	 */
    void set(final String key, final V value, long time = DEFAULT_TIMEOUT) {
		if(value == null) {
			del(key)
		} else {
	        cache.put(key, new CacheObj(value, new Date().getTime() + (time * 1000)))
		}
	}

	/**
	 * Removes a key from the cache
	 * @param key
	 * @return 
	 */
	boolean del(final String key) {
		if(cache.containsKey(key)) {
			cache.remove(key)
			return true
		}
		return false
	}
	
	/**
	 * Clear all values
	 */
	void clear() {
		cache.clear()
	}

	/**
	 * Return cache size
	 * @return 
	 */
	int size() {
		return cache.size()
	}

    /**
     * Will remove expired elements
     * It is initialized automatically
     */
    private garbageCollect() {
        cache.each {
            String key, CacheObj co ->
                expired(key)
        }
    }
}