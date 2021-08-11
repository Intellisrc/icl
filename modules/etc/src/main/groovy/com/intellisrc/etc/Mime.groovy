package com.intellisrc.etc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.awt.image.BufferedImage
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
    //
    static protected Map<String,String> types = [
        au 	    : "audio/basic",
        avi 	: "video/msvideo,video/avi,video/x-msvideo",
        bmp 	: "image/bmp",
        bz2 	: "application/x-bzip2",
        css 	: "text/css",
        dtd 	: "application/xml-dtd",
        doc 	: "application/msword",
        docx 	: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        dotx 	: "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
        eot 	: "application/vnd.ms-fontobject",
        es 	    : "application/ecmascript",
        exe 	: "application/octet-stream",
        gif 	: "image/gif",
        gz 	    : "application/x-gzip",
        ico 	: "image/x-icon",
        hqx 	: "application/mac-binhex40",
        htm 	: "text/html",
        html 	: "text/html",
        jar 	: "application/java-archive",
        jpg 	: "image/jpeg",
        js 	    : "application/javascript",
        mjs 	: "application/javascript",
        json 	: "application/json",
        midi 	: "audio/x-midi",
        mp3 	: "audio/mpeg",
        mp4 	: "video/mp4",
        mpeg 	: "video/mpeg",
        ogg     : "audio/vorbis,application/ogg",
        otf     : "application/font-otf",
        pdf     : "application/pdf",
        pl      : "application/x-perl",
        png     : "image/png",
        potx    : "application/vnd.openxmlformats-officedocument.presentationml.template",
        ppsx    : "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        ppt     : "application/vnd.ms-powerpointtd",
        pptx    : "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        ps      : "application/postscript",
        qt      : "video/quicktime",
        ra      : "audio/x-pn-realaudio,audio/vnd.rn-realaudio",
        rar     : "application/x-rar-compressed",
        ram     : "audio/x-pn-realaudio,audio/vnd.rn-realaudio",
        rdf     : "application/rdf,application/rdf+xml",
        rtf     : "application/rtf",
        sgml    : "text/sgml",
        sit     : "application/x-stuffit",
        sldx    : "application/vnd.openxmlformats-officedocument.presentationml.slide",
        svg     : "image/svg+xml",
        swf     : "application/x-shockwave-flash",
        tgz     : "application/x-tar",
        tiff    : "image/tiff",
        tsv     : "text/tab-separated-values",
        ttf     : "application/font-ttf",
        txt     : "text/plain",
        wav     : "audio/wav,audio/x-wav",
        woff    : "application/font-woff",
        woff2   : "application/font-woff2",
        xlam    : "application/vnd.ms-excel.addin.macroEnabled.12",
        xls     : "application/vnd.ms-excel",
        xlsb    : "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
        xlsx    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        xltx    : "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
        xml     : "text/xml",
        yaml    : "text/yaml",
        zip     : "application/zip,application/x-compressed-zip"
    ]
    /**
     * Return the mime type of a file
     * @param file
     * @return
     */
    static String getType(final File file) {
        String type = (file.exists() ? Files.probeContentType(file.toPath()) : "")
        if(!type) {
            type = types.get(file.name.tokenize(".")?.last()) ?: URLConnection.guessContentTypeFromName(file.name)
        }
        if(!type) {
            type = getTypeFromConfig(file.name)
        }
        if(!type) {
            Log.w("Unknown mime type for file: %s", file.name)
        }
        return type ?: "" //Prevent NULL value
    }
    /**
     * Return the mime type of a file or extension type:
     *      e.g. "image.png" or "png"
     * @param extension
     * @return
     */
    static String getType(final String fileNameOrExt) {
        String fileName
        if(fileNameOrExt.contains(".")) {
            fileName = fileNameOrExt
        } else {
            fileName = "example." + fileNameOrExt.toLowerCase()
        }
        File file = new File(fileName)
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
