package com.intellisrc.web.services

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

    AutoConfigService(ConfigAuto configAuto) {
        this.configAuto = configAuto
    }

    @Override
    String getPath() {
        return "/cfg"
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
                                ok = configAuto.update(it.key.toString(), it.value.toString())
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
                    path : "/:key",
                    method: Service.Method.PUT,
                    action: {
                        Request request ->
                            String key = request.params("key")
                            Map val = JSON.decode(request.body()).toMap()
                            return [ ok : configAuto.update(key, val) ]
                    }
            )
        ]
    }
}