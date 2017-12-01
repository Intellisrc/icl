package jp.sharelock.web.samples

import jp.sharelock.web.ServicePath
import jp.sharelock.web.ServiciableSingle
import spark.Request
import spark.Response

/**
 * @since 11/30/17.
 */
class UploadService implements ServiciableSingle {
    final static String fieldName = "image_name"
    @Override
    ServicePath getService() {
        return new ServicePath(
            contentType: "image/gif",
            uploadField: fieldName,
            upload: {
                File tmpFile ->
                    return tmpFile
            } as ServicePath.Upload
        )
    }

    @Override
    String getPath() {
        return "/upload"
    }
}
