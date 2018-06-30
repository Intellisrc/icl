package com.intellisrc.etc

import com.intellisrc.core.Log
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
    /**
     * Compress a file and rename it to *.gz
     * @param file
     */
    static boolean gzip(File file, boolean log = true) { // log: As it is used inside Log class, we need a way to turn it off to prevent infinitive loop
        boolean ok = false
        if(file.exists()) {
            if(!file.name.endsWith(".gz")) {
                file.bytes = gzip(file.bytes)
                ok = file.renameTo(file.name + ".gz")
            } else {
                if(log) { Log.w("File seems already compressed: %s", file.name) }
            }
        } else {
            if(log) { Log.e("File: %s doesn't exists", file.toString()) }
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
    static boolean gunzip(File file, boolean log = true) { // log: For uniformity with gzip
        boolean ok = false
        if(file.exists()) {
            if(file.name.endsWith(".gz")) {
                file.bytes = gunzip(file.bytes)
                ok = file.renameTo(file.name.replace(/\.gz$/,''))
            } else {
                if(log) { Log.w("File name does not ends with 'gz': %s", file.name) }
            }
        } else {
            if(log) { Log.e("File: %s doesn't exists", file.toString()) }
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
