package jp.sharelock.web.samples

import jp.sharelock.web.ServicePath
import jp.sharelock.web.ServiciableSingle
import jp.sharelock.web.ServicePath.Action

/**
 * @since 17/04/19.
 */
class IDService implements ServiciableSingle {
    ServicePath getService() {
        return new ServicePath(
            cacheTime: 10,
            cacheExtend: true,
            action: {
                return [
                    i : 200
                ]
            } as Action
        )
    }

    String getPath() {
        return "/id"
    }
}
