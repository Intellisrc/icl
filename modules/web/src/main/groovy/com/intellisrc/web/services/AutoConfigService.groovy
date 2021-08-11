package com.intellisrc.web.services

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.etc.config.ConfigAuto
import com.intellisrc.web.JSON
import com.intellisrc.web.Service
import com.intellisrc.web.ServiciableMultiple
import groovy.transform.CompileStatic
import spark.Request

/**
 * @since 2021/03/12.
 */
@CompileStatic
class AutoConfigService implements ServiciableMultiple {
    final ConfigAuto configAuto
    final String srvPath

    AutoConfigService(ConfigAuto configAuto, path) {
        this.configAuto = configAuto
        this.srvPath = path
    }

    @Override
    String getPath() {
        return "/" + srvPath
    }

    @Override
    List<Service> getServices() {
        return [
            // Get all configuration
            new Service(
                    path: "",
                    action: {
                        Request request ->
                            return configAuto.getCurrentValues(true)
                    }
            ),
            // Update several keys at once
            new Service (
                    path : "",
                    method: Service.Method.PUT,
                    action: {
                        Request request ->
                            Map val = JSON.decode(request.body()).toMap()
                            boolean ok = true
                            val.any {
                                ok = configAuto.update(it.key.toString(), it.value)
                                return !ok //This will stop if the previous was false
                            }
                            return [ ok : ok ]
                    }
            ),
            // Get all configuration which key is...
            new Service (
                    path : "/:key",
                    action: {
                        Request request ->
                            String key = request.params("key")
                            return [ (key) : configAuto.getCurrentValues(true).get(key) ]
                    }
            ),
            // Set value of key
            new Service (
                    path : "/:key/:val",
                    action: {
                        Request request ->
                            String key = request.params("key")
                            String val = request.params("val")
                            return [ ok : configAuto.update(key, val) ]
                    }
            ),
            // Update Map value of key
            new Service (
                    path : "/:type/:key/",
                    method: Service.Method.PUT,
                    action: {
                        Request request ->
                            String key = request.params("key")
                            String type = request.params("type")
                            boolean ok = false
                            switch (type) {
                                case "map" :
                                    Map val = JSON.decode ( request.body()).toMap()
                                    ok = configAuto.update(key, val)
                                    break
                                case "list" :
                                    List val = JSON.decode ( request.body()).toList()
                                    ok = configAuto.update(key, val)
                                    break
                                default:
                                    Log.w("Unknown type: %s", type)
                            }
                            return [ ok : ok ]
                    }
            )
        ]
    }
}
