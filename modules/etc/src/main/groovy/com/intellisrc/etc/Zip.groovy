package com.intellisrc.etc

import groovy.transform.CompileStatic

import java.util.zip.*

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
        boolean ok = false
        if (file.exists()) {
            if (file.name.endsWith(".gz")) {
                file.bytes = gunzip(file.bytes)
                ok = file.renameTo(file.name.replace(/\.gz$/, ''))
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
     * Compress a directory and create a zip file (v.2 : not compatible with Windows compress directory)
     * code posted here: https://stackoverflow.com/a/67295386/196507
     * @since 2021/04/28.
     * @param srcDir
     * @param zipFile
     * @return
     */
    static File compressDir(final File srcDir, final File zipFile) {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))
        srcDir.eachFileRecurse({
            zos.putNextEntry(new ZipEntry(it.path - srcDir.path + (it.directory ? "/" : "")))
            if (it.file) {
                zos << it.bytes
            }
            zos.closeEntry()
        })
        zos.close()
        return zipFile
    }

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
}
