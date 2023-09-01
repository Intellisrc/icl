package com.intellisrc.etc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.apache.tika.Tika

/**
 * @since 2021/07/14.
 *
 * This class will try to provide the most common mime types or guess it from:
 *  - file extensions
 *  - file content
 *  - streams
 *
 * https://www.baeldung.com/java-file-mime-type
 *
 * > By default, the class uses content-types.properties file in JRE_HOME/lib. We can, however, extend it, by
 * > specifying a user-specific table using the content.types.user.table property:
 * > System.setProperty("content.types.user.table","<path-to-file>");
 *
 * Extended from:
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
 *
 * If these methods don't return what expected, look for alternatives in the above link.
 *
 */
@CompileStatic
class Mime {
    /**
     * General types:
     */
    static protected Map<String,String> types = [
            '7z'	: "application/x-7z-compressed",
            aac		: "audio/aac",
            abw		: "application/x-abiword",
            arc		: "application/x-freearc",
            au 	    : "audio/basic",
            avi		: "video/x-msvideo",
            azw		: "application/vnd.amazon.ebook",
            bin		: "application/octet-stream",
            bmp		: "image/bmp",
            bz		: "application/x-bzip",
            bz2		: "application/x-bzip2",
            cda		: "application/x-cdf",
            csh		: "application/x-csh",
            css		: "text/css",
            csv		: "text/csv",
            doc		: "application/msword",
            docx	: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            dotx 	: "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            dtd 	: "application/xml-dtd",
            eot		: "application/vnd.ms-fontobject",
            epub	: "application/epub+zip",
            gif		: "image/gif",
            gz		: "application/gzip",
            htm 	: "text/html",
            html	: "text/html",
            ico		: "image/vnd.microsoft.icon",
            ics		: "text/calendar",
            jar		: "application/java-archive",
            jpeg    : "image/jpeg",
            jpg		: "image/jpeg",
            js		: "text/javascript",
            json	: "application/json",
            jsonld	: "application/ld+json",
            map     : "application/json", // .map files for js and css are json format
            mid	    : "audio/midi",
            midi	: "audio/midi",
            mjpeg   : "video/x-motion-jpeg",
            mjs		: "text/javascript",
            mp3		: "audio/mpeg",
            mp4		: "video/mp4",
            mpeg	: "video/mpeg",
            mpkg	: "application/vnd.apple.installer+xml",
            ndjson  : "application/x-ndjson",
            odp		: "application/vnd.oasis.opendocument.presentation",
            ods		: "application/vnd.oasis.opendocument.spreadsheet",
            odt		: "application/vnd.oasis.opendocument.text",
            oga		: "audio/ogg",
            ogg     : "audio/ogg",
            ogv		: "video/ogg",
            ogx		: "application/ogg",
            opus	: "audio/opus",
            otf		: "font/otf",
            pdf		: "application/pdf",
            php		: "application/x-httpd-php",
            pl      : "application/x-perl",
            png		: "image/png",
            potx    : "application/vnd.openxmlformats-officedocument.presentationml.template",
            ppsx    : "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            ppt		: "application/vnd.ms-powerpoint",
            pptx	: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            ps      : "application/postscript",
            qt      : "video/quicktime",
            ra      : "audio/x-pn-realaudio",
            ram     : "audio/x-pn-realaudio",
            rar		: "application/x-rar-compressed",
            rdf     : "application/rdf+xml",
            rtf		: "application/rtf",
            sgml    : "text/sgml",
            sh		: "application/x-sh",
            sit     : "application/x-stuffit",
            sldx    : "application/vnd.openxmlformats-officedocument.presentationml.slide",
            svg		: "image/svg+xml",
            swf		: "application/x-shockwave-flash",
            tar		: "application/x-tar",
            tgz     : "application/x-tar",
            tif     : "image/tiff",
            tiff	: "image/tiff",
            ts		: "video/mp2t",
            tsv     : "text/tab-separated-values",
            ttf		: "font/ttf",
            txt		: "text/plain",
            vsd		: "application/vnd.visio",
            wav		: "audio/wav",
            weba	: "audio/webm",
            webm	: "video/webm",
            webp	: "image/webp",
            woff	: "font/woff",
            woff2	: "font/woff2",
            xhtml	: "application/xhtml+xml",
            xlam    : "application/vnd.ms-excel.addin.macroEnabled.12",
            xls		: "application/vnd.ms-excel",
            xlsb    : "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
            xlsx	: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            xltx    : "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
            xml		: "text/xml",
            xul		: "application/vnd.mozilla.xul+xml",
            yaml    : "text/yaml",
            zip		: "application/zip",
    ]
    /**
     * Common Mime Types
     */
    // Binary
    static final String BIN     = types["bin"]
    // Images
    static final String GIF     = types["gif"]
    static final String JPG     = types["jpg"]
    static final String PNG     = types["png"]
    static final String SVG     = types["svg"]
    static final String WEBP    = types["webp"]
    // Video
    static final String AVI     = types["avi"]
    static final String MJPEG   = types["mjpeg"]
    static final String MP4     = types["mp4"]
    static final String MPG     = types["mpg"]
    static final String OGG     = types["ogg"]
    static final String WEBM    = types["webm"]
    // Audio
    static final String AAC     = types["aac"]
    static final String MP3     = types["mp3"]
    static final String WAV     = types["wav"]
    static final String WEBA    = types["weba"]
    // Data
    static final String JSON    = types["json"]
    static final String JSONLD  = types["jsonld"]
    static final String XML     = types["xml"]
    static final String YAML    = types["yaml"]
    // Documents
    static final String CSV     = types["csv"]
    static final String DOC     = types["doc"]
    static final String DOCX    = types["docx"]
    static final String HTML    = types["html"]
    static final String ODP     = types["odp"]
    static final String ODS     = types["ods"]
    static final String ODT     = types["odt"]
    static final String PDF     = types["pdf"]
    static final String PPT     = types["ppt"]
    static final String PPTX    = types["pptx"]
    static final String RTF     = types["rtf"]
    static final String TXT     = types["txt"]
    static final String XLS     = types["xls"]
    static final String XLSX    = types["xlsx"]
    // Web
    static final String CSS     = types["css"]
    static final String JS      = types["js"]
    // Fonts
    static final String OTF     = types["otf"]
    static final String TTF     = types["ttf"]
    static final String WOFF    = types["woff"]
    static final String WOFF2   = types["woff2"]
    // File Compression
    static final String BZ      = types["bz"]
    static final String BZ2     = types["bz2"]
    static final String GZ      = types["gz"]
    static final String JAR     = types["jar"]
    static final String RAR     = types["rar"]
    static final String TAR     = types["tar"]
    static final String ZIP     = types["zip"]
    static final String ZIP7    = types["7z"]
    // ServerSentEvents
    static final String SSE     = "text/event-stream"

    static Tika tikaInstance

    static protected Tika getTika() {
        if(!tikaInstance) {
            tikaInstance = new Tika()
        }
        return tikaInstance
    }
    /**
     * Return the mime type of a file
     * @param file
     * @return
     */
    static String getType(final File file) {
        String type = ""
        if(! file.isDirectory()) {
            type = getTypeFromConfig(file.name)
            if (!type) {
                type = (file.exists() ? tika.detect(file) : "")
            }
            if (!type || type == "text/plain" || type == "application/octet-stream") {
                String guessType = types.get(file.name.tokenize(".")?.last()) ?: URLConnection.guessContentTypeFromName(file.name)
                if (guessType) {
                    type = guessType
                }
            }
            if (!type) {
                Log.w("Unknown mime type for file: %s", file.name)
            }
        } else {
            Log.w("Requested Mime type for directory: %s", file.absolutePath)
        }
        return type ?: "" //Prevent NULL value
    }
    /**
     * Return
     * @param url
     * @return
     */
    static String getType(URL url) {
        return tika.detect(url) ?: getType(new File(url.getFile()))
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
        return tika.detect(stream) ?: URLConnection.guessContentTypeFromStream(stream)
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
    /**
     * Check if file extension is usually compressed
     * @param ext
     * @return
     */
    static boolean isCompressed(String mimeType) {
        return switch (mimeType) {
            case ~/^text.*$/ -> false
            case ~/^image\/(bmp|tiff|svg|x-icon|vnd.microsoft.icon).*$/ -> false
            case ~/^font\/(ttf|otf).*$/ -> false
            case ~/^application\/(vnd.ms-fontobject|xml-dtd|x-font-|(x-)?javascript).*$/ -> false
            case ~/^application\/.*(xml|json)$/ -> false
            default -> true
        }
    }
}
