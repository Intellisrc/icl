package com.intellisrc.web.service

import groovy.transform.CompileStatic

/**
 * Most of the common HTTP headers
 * https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
 */
@CompileStatic
class HttpHeader {
    // Shared Headers (Request / Response)
    public static final String CACHE_CONTROL = "Cache-Control"
    public static final String CONNECTION = "Connection"
    public static final String CONTENT_ENCODING = "Content-Encoding"
    public static final String CONTENT_LENGTH = "Content-Length"
    public static final String CONTENT_TYPE = "Content-Type"
    public static final String DATE = "Date"
    public static final String PRAGMA = "Pragma"
    public static final String TRAILER = "Trailer"
    public static final String TRANSFER_ENCODING = "Transfer-Encoding"
    public static final String UPGRADE = "Upgrade"
    public static final String VIA = "Via"

    // Request Headers
    public static final String A_IM = "A-IM"
    public static final String ACCEPT = "Accept"
    public static final String ACCEPT_CHARSET = "Accept-Charset"
    public static final String ACCEPT_DATETIME = "Accept-Datetime"
    public static final String ACCEPT_ENCODING = "Accept-Encoding"
    public static final String ACCEPT_LANGUAGE = "Accept-Language"
    public static final String ACCEPT_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method"
    public static final String ACCEPT_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers"
    public static final String AUTHORIZATION = "Authorization"
    public static final String COOKIE = "Cookie"
    public static final String DNT = "DNT" // Do not track
    public static final String EXPECT = "Expect"
    public static final String FORWARDED = "Forwarded"
    public static final String FROM = "From"
    public static final String HOST = "Host"
    public static final String IF_MATCH = "If-Match"
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since"
    public static final String IF_NONE_MATCH = "If-None-Match"
    public static final String IF_RANGE = "If-Range"
    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since"
    public static final String MAX_FORWARDS = "Max-Forwards"
    public static final String ORIGIN = "Origin"
    public static final String PREFER = "Prefer"
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization"
    public static final String RANGE = "Range"
    public static final String REFERER = "Referer"
    public static final String TE = "TE" //Transfer encodings
    public static final String USER_AGENT = "User-Agent"
    public static final String X_FORWARDED_FOR = "X-Forwarded-For"
    public static final String X_FORWARDED_HOST = "X-Forwarded-Host"
    public static final String X_FORWARDED_PORT = "X-Forwarded-Port"
    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto"
    public static final String X_FORWARDED_SERVER = "X-Forwarded-Server"
    public static final String X_POWERED_BY = "X-Powered-By"
    public static final String X_REQUEST_ID = "X-Request-ID"
    public static final String X_REQUESTED_WITH = "X-Requested-With"

    // Response Headers
    public static final String ACCEPT_CH = "Accept-CH" //Client Hints
    public static final String ACCEPT_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
    public static final String ACCEPT_PATCH = "Accept-Patch"
    public static final String ACCEPT_RANGES = "Accept-Ranges"
    public static final String AGE = "Age"
    public static final String ALLOW = "Allow"
    public static final String ALT_SVC = "Alt-Svc"
    public static final String CONTENT_DISPOSITION = "Content-Disposition"
    public static final String CONTENT_LANGUAGE = "Content-Language"
    public static final String CONTENT_LOCATION = "Content-Location"
    public static final String CONTENT_RANGE = "Content-Range"
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding"
    public static final String DELTA_BASE = "Delta-Base"
    public static final String ETAG = "ETag"
    public static final String EXPIRES = "Expires"
    public static final String IM = "IM" // Instance Manipulations
    public static final String LAST_MODIFIED = "Last-Modified"
    public static final String LINK = "Link"
    public static final String LOCATION = "Location"
    public static final String PREFERENCE_APPLIED = "Preference-Applied"
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate"
    public static final String PUBLIC_KEY_PINS = "Public-Key-Pins"
    public static final String RETRY_AFTER = "Retry-After"
    public static final String SERVER = "Server"
    public static final String SERVER_CACHE = "Server-Cache"
    public static final String SET_COOKIE = "Set-Cookie"
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security"
    public static final String TK = "Tk" // Tracking Status
    public static final String VARY = "Vary"
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate"
}