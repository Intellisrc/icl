package jp.sharelock.web.samples

import jp.sharelock.web.ServicePath
import jp.sharelock.web.ServiciableSingle
import jp.sharelock.web.ServicePath.ActionRequestResponse
import spark.Request
import spark.Response

/**
 * @since 17/04/19.
 */
class IDService implements ServiciableSingle {
    ServicePath getService() {
        return new ServicePath(
            //cacheTime: 10,
            //cacheExtend: true,
            action: {
                Request request, Response response ->
                    //Example use of Response
                    if(request.params().isEmpty()) {
                        response.status(404)
                        response.redirect("/")
                    }
                return [
                    i : 200
                ]
            } as ActionRequestResponse
        )
    }

    String getPath() {
        return "/id"
    }
}
