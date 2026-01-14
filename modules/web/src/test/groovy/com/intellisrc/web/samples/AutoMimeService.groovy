package com.intellisrc.web.samples

import com.intellisrc.web.WebService
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Service

/**
 * Manual test of parameters
 * @since 2022/07/27.
 */
class AutoMimeService {
    static int fixedPort = 6789

    static void main(String[] args) {
        WebService ws = new WebService(
            port: fixedPort
        )
        /*
            When we specify the path as fixed, the content-type header will
            be set depending on the extension (in this case: text/yaml) and
            the Map will be encoded to YAML format.
        */
        ws.add(new Service(
            path: "/users/info.yaml",
            action: {
                Request request ->
                    return [ok : true, info: "hello"]
            }
        ))
        /*
            In this case, even if we call the service as: /config/peter.yaml
            it will not return a YAML string. As we are returning a Map, the
            content-type will be automatically set to JSON (and encoded as JSON).
            If you want YAML output in this case, you need to set it manually.
            This also applies to GlobMatcher, RegExMatcher and OptionalMatcher.
         */
        ws.add(new Service(
            path: "/config/:user",
            //contentType: Mime.YAML
            action: {
                Request request ->
                    return [ok : true]
            }
        )).start()
    }
}
