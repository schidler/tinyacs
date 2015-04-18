package com.calix.sxa.cc.connreq;

import com.calix.sxa.VertxRedisUtils;
import com.calix.sxa.cc.model.Cpe;
import com.calix.sxa.cc.util.AcsConstants;
import io.vertx.java.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;


/**
 * Project:  SXA-CC
 *
 * Connection-Request Related Utils
 *
 * @author: jqin
 */
public class ConnectionRequestUtils {
    private static Logger log = LoggerFactory.getLogger(ConnectionRequestUtils.class);

    /**
     * Get the ConnReq Redis Key by CPE ID String.
     *
     * @param cpeId
     */
    public static String getConnReqRedisKeyByCpeId(String cpeId) {
        return AcsConstants.REDIS_KEY_CONN_REQ_PREFIX + cpeId;
    }

    /**
     * Get the connection-request info by CPE ID String.
     *
     * @param redisClient
     * @param cpeId
     * @param handler
     */
    public static void getConnReqStateByCpeId(
            RedisClient redisClient,
            final String cpeId,
            Handler<String> handler) {
        VertxRedisUtils.getValue(
                redisClient,
                getConnReqRedisKeyByCpeId(cpeId),
                handler
        );
    }

    /**
     * Send a new connection-request request to the Connection_request Worker Vertice.
     *
     * @param vertx
     * @param cpeJsonObject
     */
    public static void sendNewConnectionRequestRequest(
            Vertx vertx,
            final JsonObject cpeJsonObject) {
        // Build a request message
        String username = cpeJsonObject.getString(Cpe.DB_FIELD_NAME_CONNREQ_USERNAME);
        String password = cpeJsonObject.getString(Cpe.DB_FIELD_NAME_CONNREQ_PASSWORD);
        String connReqUrl = cpeJsonObject.getString(Cpe.DB_FIELD_NAME_CONNREQ_URL);
        JsonObject connReqRequest = new JsonObject()
                .putString(ConnectionRequestConstants.CPE_ID, cpeJsonObject.getString(AcsConstants.FIELD_NAME_ID))
                .putString(ConnectionRequestConstants.URL, connReqUrl)
                .putString(ConnectionRequestConstants.USERNAME, username)
                .putString(ConnectionRequestConstants.PASSWORD, password);

        vertx.eventBus().send(
                AcsConstants.VERTX_ADDRESS_ACS_CONNECTION_REQUEST,
                connReqRequest
        );
    }

}
