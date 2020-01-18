package com.intellisrc.etc

import groovy.transform.CompileStatic

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * @since 18/03/09.
 *
 * General class to compress / decompress
 * Based in: https://gist.github.com/welshstew/3d1fbff954f94182477b
 */
@CompileStatic
class Zip {
    static public class InvalidExtensionException extends Exception {}
    /**
     * Compress a file and rename it to *.gz
     * @param file
     */
    static boolean gzip(File file) {
        boolean ok = false
        if(file.exists()) {
            if(!file.name.endsWith(".gz")) {
                file.bytes = gzip(file.bytes)
                ok = file.renameTo(file.path + ".gz")
            } else {
                throw new InvalidExtensionException()
            }
        } else {
            throw new FileNotFoundException()
        }
        return ok
    }
    /**
     * Compress bytes using GZIP
     * @param uncompressed
     * @return
     */
    static byte[] gzip(byte[] uncompressed){
        def targetStream = new ByteArrayOutputStream()
        def zipStream = new GZIPOutputStream(targetStream)
        zipStream.write(uncompressed)
        zipStream.close()
        def zippedBytes = targetStream.toByteArray()
        targetStream.close()
        return zippedBytes
    }
    /**
     * Uncompress file and removes the extension *.gz
     * @param file
     */
    static boolean gunzip(File file) {
        boolean ok = false
        if(file.exists()) {
            if(file.name.endsWith(".gz")) {
                file.bytes = gunzip(file.bytes)
                ok = file.renameTo(file.name.replace(/\.gz$/,''))
            } else {
                throw new InvalidExtensionException()
            }
        } else {
            throw new FileNotFoundException()
        }
        return ok
    }
    /**
     * Uncompress bytes
     * @param compressed
     * @return
     */
    static byte[] gunzip(byte[] compressed){
        def inflaterStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
        return inflaterStream.getBytes()
    }
}
