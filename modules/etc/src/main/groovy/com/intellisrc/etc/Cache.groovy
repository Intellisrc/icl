package com.intellisrc.etc

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@CompileStatic
/**
 * Manage Cache of objects
 * @author Alberto Lepe <lepe@intellisrc.com>
 * @param <V>
 *
 * gcInterval  : how many seconds to execute Garbage Collector. Set it to zero to disabled it (for any crazy reason)
 * extend       : if true, it will extend time upon access. If false, it will expire when the time is due (without changing expire value).
 */
class Cache<V> {
    static final int FOREVER = -1
    static final int DEFAULT = 600
    interface onNotFound {
        V call()
    }
	int gcInterval = 120 //seconds
    int timeout = DEFAULT //seconds
    boolean extend  = false

    /**
     * Object used as data structure inside Cache
     * long expire : stores the time in which will expire
     * length      : time in milliseconds that will be stored (used to extend cache)
     * value       : value to store
     */
    private static class CacheItem<V> {
        V               value
        LocalDateTime   expire
        private int     length = 0
        private boolean forever = false

        CacheItem(V obj, int timeToStoreSec = DEFAULT) {
            if(timeToStoreSec != 0) { //Do not store if its disabled
                value = obj
                if (timeToStoreSec > 0) {
                    expire = LocalDateTime.now().plusSeconds(timeToStoreSec)
                    length = timeToStoreSec
                } else if (timeToStoreSec == FOREVER) {
                    forever = true
                }
            }
        }
    }
    protected final ConcurrentMap<String, CacheItem<V>> cache = new ConcurrentHashMap<>()

	/**
	 * Constructor
	 */
	Cache() {
        Thread.start {
            while(gcInterval > 0) {
                sleep(gcInterval * 1000)
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
    @Deprecated
    boolean exists(final String key) {
        return contains(key)
    }
    boolean contains(final String key) {
        return cache.containsKey(key) &&! expired(key)
    }
    /**
     * Return true if expired and remove it from cache
     * @param key
     * @return
     */
    private boolean expired(final String key) {
        def expired = false
        def obj = cache.get(key)
        if(obj) {
            if(!obj.forever) {
                def now = LocalDateTime.now()
                if (obj.expire < now) {
                    Log.v("Key [$key] expired.")
                    del(key)
                    expired = true
                } else {
                    if (extend) {
                        obj.expire = now.plusSeconds(obj.length)
                    }
                }
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
    V get(final String key, onNotFound notFound = null, int time = timeout) {
        V ret = null
        if(contains(key)) {
            ret = cache.get(key).value
			Log.d( "[$key] read from cache")
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
    void set(final String key, final V value, int time = timeout) {
		if(value == null) {
			del(key)
		} else {
	        cache.put(key, new CacheItem(value, time))
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
     * Return all keys in cache
     * @return
     */
    List<String> keys() {
        return cache.keySet().toList()
    }

    /**
     * Will remove expired elements
     * It is initialized automatically
     */
    private garbageCollect() {
        cache.each {
            String key, CacheItem co ->
                expired(key)
        }
    }
}