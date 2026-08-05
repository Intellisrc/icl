package com.intellisrc.web.service

import groovy.transform.CompileStatic

/**
 * Parsed representation of a single HTTP {@code Range: bytes=...} request.
 *
 * <p>Added to support HTTP {@code 206 Partial Content} for {@code File} responses
 * (e.g. video streaming), which also makes the benign "Broken pipe" disconnects
 * rare (browsers no longer need to abort and re-request to seek).
 *
 * <p>Only single byte-ranges are supported. Multi-range requests ({@code bytes=0-100,200-300})
 * are intentionally not handled: {@link #parse} returns {@code null} so the caller falls back
 * to a full {@code 200} response, which matches typical HTML5 {@code <video>} usage.
 *
 * @since 2.10.5
 * <Generated>
 */
@CompileStatic
final class Range {
    /** Inclusive start byte offset. */
    final long start
    /** Inclusive end byte offset. */
    final long end
    /** Total size of the underlying resource in bytes. */
    final long total

    private Range(long start, long end, long total) {
        this.start = start
        this.end = end
        this.total = total
    }

    /** Length in bytes covered by this range = {@code end - start + 1}. */
    long getLength() { end - start + 1 }

    /** Value for the {@code Content-Range} response header: {@code "bytes start-end/total"}. */
    String contentRangeHeader() { "bytes ${start}-${end}/${total}" }

    /**
     * A range is valid only if it describes at least one byte inside the resource:
     * {@code total > 0 && start >= 0 && start <= end && end < total}.
     */
    boolean isValid() { total > 0 && start >= 0 && start <= end && end < total }

    /**
     * Parse a single {@code "Range: bytes=..."} header for a resource of {@code total} bytes.
     *
     * @param header the raw {@code Range} header value (may be {@code null})
     * @param total  total size in bytes of the resource being served
     * @return {@code null} if the header is absent or unsupported (no range -> serve full 200);
     *         a {@link Range} with {@link #isValid()} == {@code false} if the syntax is malformed
     *         or the range is out of bounds, so the caller can emit {@code 416} with
     *         {@code Content-Range: bytes *&#47;total}; otherwise a valid {@link Range}.
     */
    static Range parse(String header, long total) {
        if (header == null || header.empty) return null
        header = header.trim()
        if (!header.startsWith("bytes=")) return null      // we only support byte ranges
        String spec = header.substring("bytes=".length()).trim()
        if (spec.contains(",")) return null                // multipart -> fall back to 200
        String[] parts = spec.split("-", 2)
        if (parts.length != 2) return invalid(total)
        try {
            if (parts[0].empty) {                          // suffix: "bytes=-500" -> last 500
                long suffix = parts[1].toLong()
                if (suffix <= 0) return invalid(total)
                long s = Math.max(0L, total - suffix)
                return new Range(s, total - 1, total)
            }
            long s = parts[0].toLong()
            if (parts[1].empty) {                          // open: "bytes=500-"
                if (s >= total) return invalid(total)
                return new Range(s, total - 1, total)
            }
            long e = parts[1].toLong()                     // closed: "bytes=0-499"
            if (s > e || s >= total) return invalid(total)
            e = Math.min(e, total - 1)                     // clamp end to last byte
            return new Range(s, e, total)
        } catch (NumberFormatException ignore) {
            return invalid(total)
        }
    }

    private static Range invalid(long total) { new Range(0, -1, total) }  // isValid() == false
}
