package jp.sharelock.db

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@groovy.transform.CompileStatic
/**
 * Classes extending this class must be implemented as singleton
 * @author Alberto Lepe <lepe@sharelock.jp>
 * @param <K>
 * @param <V> 
 */
abstract class Cache<K, V> {

    final ConcurrentMap<K, V> cache = new ConcurrentHashMap<>()

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
	boolean exists(K key) {
		return cache.containsKey(key)
	}
	/**
	 * Returns value if found, or default if not
	 * @param key
	 * @param default_val
	 * @return 
	 */
    V get(K key, V default_val) {
        return cache.getOrDefault(key, default_val)
    }
	
	/**
	 * Get value by key
	 * @param key
	 * @return
	 * @throws Cache.KeyNotPresentException
	 */
    V get(K key) throws KeyNotPresentException {
		if (cache.size() > 0 && cache.containsKey(key)) {
        	return cache.get(key)
		} else {
		   throw new KeyNotPresentException()
		}
    }

	/**
	 * Set value to key. If value is null, it will be removed
	 * @param key
	 * @param value 
	 */
    void set(K key, V value) {
		if(value == null) {
			del(key)
		} else {
	        cache.put(key, value)
		}
	}

	/**
	 * Removes a key from the cache
	 * @param key
	 * @return 
	 */
	boolean del(K key) {
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
	
	static class KeyNotPresentException extends Exception {
		KeyNotPresentException() {}
	}

}