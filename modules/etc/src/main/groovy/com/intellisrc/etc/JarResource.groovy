package com.intellisrc.etc

import groovy.transform.CompileStatic

import java.nio.file.Files

/**
 * @since 18/09/05.
 */
@CompileStatic
class JarResource {
    /**
     * Return a string from a resource inside a Jar
     * @param path
     * @return
     */
    static String getAsString(Class reference, String path) {
        InputStream input = reference.getResourceAsStream(path)
        return new BufferedReader(new InputStreamReader(input)).text
    }
    /**
     * Return a binary file from a Jar
     * @param path
     * @return
     */
    static byte[] getAsBytes(Class reference, String path) {
        return reference.getResourceAsStream(path).bytes
    }
    /**
     * Return a file after being copied into a temporally location
     * @param path
     * @return
     */
    static File getAsTempFile(Class reference, String path) {
        InputStream input = reference.getResourceAsStream(path)
        File tempFile = File.createTempFile("jar","tmp")
        Files.copy(input, tempFile.toPath())
        return tempFile
    }
}
