package com.intellisrc.web.samples

import com.intellisrc.web.Service
import com.intellisrc.web.ServiciableSingle

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
            upload: {
                File tmpFile ->
                    return tmpFile
            } as Service.Upload
        )
    }
}
