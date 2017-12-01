package jp.sharelock.web.samples

import jp.sharelock.etc.Log
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
            cacheTime: 10,
            cacheExtend: true,
            action: {
                Request request, Response response ->
                    int id = 0
                    //Example use of Response
                    if(request.queryParams().isEmpty()) {
                        Log.e("No parameters found")
                        response.status(404)
                        response.redirect("/")
                    } else {
                        id = request.queryParams("i") as Integer
                        Log.d("ID requested: %d", id)
                    }
                return [
                    i : id,
                    t : new Date().time
                ]
            } as ActionRequestResponse
        )
    }

    String getPath() {
        return "/id"
    }
}

