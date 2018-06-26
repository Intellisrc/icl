package com.intellisrc.web.samples

import com.intellisrc.web.ServiciableSingle

/**
 * @since 11/30/17.
 */
class UploadService implements ServiciableSingle {
    final static String fieldName = "image_name"
    @Override
    com.intellisrc.web.Service getService() {
        return new com.intellisrc.web.Service(
            path: "/upload",
            contentType: "image/gif",
            uploadField: fieldName,
            upload: {
                File tmpFile ->
                    return tmpFile
            } as com.intellisrc.web.Service.Upload
        )
    }
}
