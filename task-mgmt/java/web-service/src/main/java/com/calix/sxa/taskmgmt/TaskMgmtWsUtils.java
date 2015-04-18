package com.calix.sxa.taskmgmt;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

/**
 * Project:  sxa-cc-parent
 *
 * @author: jqin
 */
public class TaskMgmtWsUtils {
    /**
     * Common Util to check async response from mod-redis and mod-mongo-persistor.
     * @param result
     * @return  An error string if result is valid, or null.
     */
    public static String checkAsyncResult(HttpServerRequest request, JsonObject result) {
        String error = null;

        /**
         * Check for null pointer
         */
        if (result == null) {
            error = "The response message is NULL!";
        } else {
            /**
             * Check status
             */
            String status = result.getString("status");
            if (status  == null) {
                error = "The response message is invalid (no status found)!";
            } else if (!status.equals("ok")) {
                error = "The response message has unexpected status " + status + "!";
            }
        }

        return  error;
    }
}
