package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.Zip
import groovy.transform.CompileStatic

import java.lang.reflect.InvocationTargetException

/**
 * Handles the output compression
 */
@CompileStatic
enum Compression {
    BROTLI_COMPRESSED, GZIP_COMPRESSED, DEFLATE_COMPRESSED, NONE

    protected static Map<Compression, Boolean> availability = [:]
    /**
     * Compress bytes depending on method
     * @param bytes
     * @return
     */
    Object compress(byte[] bytes) {
        return switch(this) {
            case BROTLI_COMPRESSED -> Zip.brotliCompress(bytes)
            case GZIP_COMPRESSED -> Zip.gzip(bytes)
            case DEFLATE_COMPRESSED -> Zip.deflate(bytes)
            default -> bytes
        }
    }
    @Override
    String toString() {
        return switch(this) {
            case BROTLI_COMPRESSED -> "br"
            case GZIP_COMPRESSED -> "gzip"
            case DEFLATE_COMPRESSED -> "deflate"
            default -> ""
        }
    }
    /**
     * Get compression from String
     * @param encoding
     * @return
     */
    static Compression fromString(String encoding) {
        return switch (encoding) {
            case "br" -> BROTLI_COMPRESSED
            case "gzip" -> GZIP_COMPRESSED
            case "deflate" -> DEFLATE_COMPRESSED
            default -> NONE
        }
    }
    /**
     * Get all compression methods available in server
     * @return
     */
    static List<Compression> getAvailable() {
        return values().findAll {
            boolean available = true
            if (it == BROTLI_COMPRESSED) {
                if (availability.containsKey(it)) {
                    available = availability[it]
                } else {
                    try {
                        Class<?> brotli = Class.forName("com.nixxcode.jvmbrotli.common.BrotliLoader")
                        available = (Boolean) brotli.getMethod("isBrotliAvailable").invoke(null)
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        Log.v("Brotli was not found: %s", e)
                        available = false
                    }
                    availability[it] = available
                }
            }
            return available
        }
    }
}