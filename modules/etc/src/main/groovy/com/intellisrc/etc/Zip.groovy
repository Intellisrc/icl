package com.intellisrc.etc

import com.nixxcode.jvmbrotli.common.BrotliLoader
import com.nixxcode.jvmbrotli.dec.Decoder
import com.nixxcode.jvmbrotli.enc.Encoder
import groovy.transform.CompileStatic

import java.nio.charset.Charset
import java.util.zip.*

import static java.nio.charset.StandardCharsets.UTF_8

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
     * override if differs
     */
    static Charset charset = UTF_8
    /**
     * Compress a file and rename it to *.gz
     * @param file
     */
    static boolean gzip(File file) {
        //noinspection GroovyUnusedAssignment : IDE mistake
        boolean ok = false
        if (file.exists()) {
            if (!file.name.endsWith(".gz")) {
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
    static byte[] gzip(byte[] uncompressed) {
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
        //noinspection GroovyUnusedAssignment : IDE mistake
        boolean ok = false
        if (file.exists()) {
            if (file.name.endsWith(".gz")) {
                file.bytes = gunzip(file.bytes)
                ok = file.renameTo(file.path.replaceAll(/\.gz$/, ''))
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
    static byte[] gunzip(byte[] compressed) {
        def inflaterStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
        return inflaterStream.getBytes()
    }

    /**
     * Compress using Brotli.
     * NOTE: in order to use this method, you need to add the dependency: https://mvnrepository.com/artifact/com.nixxcode.jvmbrotli
     * (it might be necessary to include the architecture-specific-library dependency as well)
     * @param uncompressed
     * @param q quality (from 0 to 11)
     * @return
     */
    static byte[] brotliCompress(byte[] uncompressed, int q = 9) {
        byte[] out = uncompressed
        if(BrotliLoader.isBrotliAvailable()) {
                if (q <= 0) { q = 10 }
                if (q > 11) { q = 11 }
                out = Encoder.compress(uncompressed, new Encoder.Parameters().setQuality(q))
        }
        return out
    }
    /**
     * Decompress using Brotli
     * NOTE: in order to use this method, you need to add the dependency: https://mvnrepository.com/artifact/com.nixxcode.jvmbrotli
     * (it might be necessary to include the architecture-specific-library dependency as well)
     * @param compressed
     * @return
     */
    static byte[] brotliDecode(byte[] compressed) {
        byte[] out = compressed
        if(BrotliLoader.isBrotliAvailable()) {
            out = Decoder.decompress(compressed)
        }
        return out
    }
    /**
     * Compress a directory and create a zip file (v.2 : not compatible with Windows compress directory)
     * code posted here: https://stackoverflow.com/a/67295386/196507
     * @since 2021/04/28.
     * @param srcDir
     * @param zipFile
     * @return
     */
    static File compressDir(final File srcDir, final File zipFile) {
        Map<String, byte[]> namesData = [:]
        srcDir.eachFileRecurse {
            if(it.file) {
                namesData[it.path - srcDir.path + (it.directory ? File.separatorChar : "")] = it.bytes
            }
        }
        zipFile.withOutputStream {
            zip(namesData).writeTo(it)
        }
        return zipFile
    }
    /**
     * Creates a file when decompressing a zip file
     * @param destinationDir
     * @param zipEntry
     * @return
     * @throws IOException
     */
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.name)
        String destDirPath = destinationDir.canonicalPath
        String destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.name)
        }
        return destFile
    }
    /**
     * From https://www.baeldung.com/java-compress-and-uncompress
     * @param zipFile
     * @param destDir
     * @return
     */
    static File decompressZip(final File zipFile, final File destDir) {
        byte[] buffer = new byte[1024]
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))
        ZipEntry zipEntry = zis.nextEntry
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry)
            if (zipEntry.directory) {
                if (!newFile.directory && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile)
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.parentFile
                if (!parent.directory && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent)
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile)
                int len
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
        zis.close()
        return destDir
    }
    /**
     * Compress byte arrays without using files
     *
     * @param namesData : Map with <Logical Path, data> values
     * @return
     */
    static ByteArrayOutputStream zip(final Map<String, byte[]> namesData) {
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        ZipOutputStream zos = new ZipOutputStream(os, charset)
        namesData.each ({
            zos.putNextEntry(new ZipEntry(it.key))
            zos << it.value
            zos.closeEntry()
        })
        zos.close()
        return os
    }
    /**
     * Decompress a zip file into a Map <Logical Path, data>
     * @param is
     * @return
     */
    static Map<String, byte[]> unzip(InputStream is) {
        Map<String, byte[]> namesData = [:]
        byte[] buffer = new byte[1024]
        ZipInputStream zis = new ZipInputStream(is, charset)
        ZipEntry zipEntry = zis.nextEntry
        while (zipEntry != null) {
            if(! zipEntry.directory) {
                // write file content
                ByteArrayOutputStream os = new ByteArrayOutputStream()
                int len
                while ((len = zis.read(buffer)) > 0) {
                    os.write(buffer, 0, len)
                }
                namesData[zipEntry.name] = os.toByteArray()
                os.close()
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
        zis.close()
        return namesData
    }
}
