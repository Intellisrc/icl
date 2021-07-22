package com.intellisrc.etc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

import java.nio.file.Files

/**
 * @since 2021/07/14.
 * https://www.baeldung.com/java-file-mime-type
 *
 * > By default, the class uses content-types.properties file in JRE_HOME/lib. We can, however, extend it, by
 * > specifying a user-specific table using the content.types.user.table property:
 * > System.setProperty("content.types.user.table","<path-to-file>");
 *
 * If these methods don't return what expected, look for alternatives in the above link.
 *
 */
@CompileStatic
class Mime {
    /**
     * Return the mime type of a file
     * @param file
     * @return
     */
    static String getType(final File file) {
        return file.exists() ? Files.probeContentType(file.toPath()) : URLConnection.guessContentTypeFromName(file.name)
    }
    /**
     * Return the mime type of a file or extension type:
     *      e.g. "image.png" or "png"
     * @param extension
     * @return
     */
    static String getType(final String fileNameOrExt) {
        File file = new File(fileNameOrExt.contains(".") ? fileNameOrExt : "example." + fileNameOrExt.tokenize('.').last())
        return getType(file)
    }
    /**
     * Guess a mime type based in header bytes
     * @param stream
     * @return
     */
    static String getType(InputStream stream) {
        return URLConnection.guessContentTypeFromStream(stream)
    }
    /**
     * Look for mime type in config, for example:
     *
     * mime.ext = unknown/ext
     * mime.svg = image/svg
     *
     * @param filename
     * @return
     */
    static String getTypeFromConfig(String filename) {
        return Config.get("mime." + (filename.tokenize('.').last()), "")
    }
}
