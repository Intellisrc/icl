package jp.sharelock.web.samples

import jp.sharelock.web.ServicePath
import jp.sharelock.web.ServiciableSingle
import spark.Request
import spark.Response

/**
 * @since 11/30/17.
 */
class UploadService implements ServiciableSingle {
    @Override
    ServicePath getService() {
        return new ServicePath(
            uploadField: "image_name",
            upload: {
                File tmpFile, Request request, Response response ->
                    tmpFile.renameTo("uploads/"+request.queryParams("user"))
                    response.redirect("/done")
            } as ServicePath.Upload
        )
    }

    @Override
    String getPath() {
        return "/upload"
    }
}
