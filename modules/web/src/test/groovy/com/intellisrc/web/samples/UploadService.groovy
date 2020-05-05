package com.intellisrc.web.samples

import com.intellisrc.web.Service
import com.intellisrc.web.ServiciableSingle
import spark.Request

/**
 * @since 11/30/17.
 */
class UploadService implements ServiciableSingle {
    final static String fieldName = "image_name"
    @Override
    Service getService() {
        return new Service(
            path: "/upload",
            contentType: "image/gif",
            uploadField: fieldName,
            action: {
                File tmpFile ->
                    return tmpFile
            }
        )
    }
}
