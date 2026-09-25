package com.intellisrc.web

import com.intellisrc.web.service.Compression
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.ServiceOutput
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Regression guard: a Service that returns a {@link File} (e.g. an image, video, or
 * static asset) must pass through {@code handleContentType} without buffering it into
 * memory or throwing a cast exception. The File reference must be preserved so it can
 * be streamed (and served with HTTP Range support) by the write switch.
 *
 * <p>Covers every {@link ServiceOutput.Type} a File can resolve to via its extension:
 * IMAGE (png/jpg/gif/svg), BINARY (mp4/pdf), TEXT (txt/html), JSON, YAML.
 *
 * @since 2.10.5
 */
class HandleContentTypeTest extends Specification {

    @Unroll
    def "File .#ext -> handleContentType keeps File reference and does not throw"() {
        given:
            java.io.File file = java.io.File.createTempFile("asset", ".${ext}")
            file.deleteOnExit()
            byte[] sig = [-119, 80, 78, 71, 13, 10, 26, 10] as byte[] // arbitrary non-empty bytes
            file.withOutputStream { it.write(sig); it.write(new byte[64]) }
            def sp = new Service(path: "asset.${ext}", action: { file })
        when:
            ServiceOutput out = WebService.handleContentType(sp, file, null, "UTF-8", false, Compression.NONE)
        then:
            out != null
            out.content instanceof java.io.File   // preserved for streaming, never buffered
            out.size == file.size()                // size from disk metadata
        cleanup:
            file.delete()
        where:
            ext << ["png", "jpg", "gif", "svg", "mp4", "pdf", "txt", "html", "json", "yaml"]
    }
}
