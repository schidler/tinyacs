package vertx2.cpe.sim;

import vertx2.VertxUtils;
import vertx2.cwmp.CwmpInformEventCodes;
import vertx2.model.Cpe;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.HashMap;
import java.util.Map;

/**
 * Project:  cwmp CPE Simulator
 *
 * @author: ronyang
 */
public class HttpRequestHandler implements Handler<HttpServerRequest>{
    private static final Logger log = LoggerFactory.getLogger(HttpRequestHandler.class.getName());

    /**
     * Vertx
     */
    Vertx vertx;

    /**
     * Vert.X Container Instance
     */
    Container container;

    /**
     * URL path to CWMP Inform Event Type Map
     */
    Map<String, String> eventCodeMap;

    /**
     * Constructor.
     *
     * @param vertx
     */
    public HttpRequestHandler(Vertx vertx, Container container) {
        this.vertx = vertx;
        this.container = container;

        eventCodeMap = new HashMap<>();
        eventCodeMap.put("connreq", CwmpInformEventCodes.CONNECTION_REQUEST);
        eventCodeMap.put("periodic", CwmpInformEventCodes.PERIODIC);
        eventCodeMap.put("boot", CwmpInformEventCodes.BOOT);
        eventCodeMap.put("bootstrap", CwmpInformEventCodes.BOOT_STRAP);
        eventCodeMap.put("value-change", CwmpInformEventCodes.VALUE_CHANGE);
        eventCodeMap.put("transfer-complete", CwmpInformEventCodes.TRANSFER_COMPLETE);
        eventCodeMap.put("diagnostics-complete", CwmpInformEventCodes.DIAGNOSTICS_COMPLETE);
        eventCodeMap.put("auto-transfer-complete", CwmpInformEventCodes.AUTONOMOUS_TRANSFER_COMPLETE);
        eventCodeMap.put("m-boot", CwmpInformEventCodes.M_REBOOT);
        eventCodeMap.put("m-download", CwmpInformEventCodes.M_DOWNLOAD);
        eventCodeMap.put("m-change-du-state", CwmpInformEventCodes.M_CHANGE_DU_STATE);
        eventCodeMap.put("m-upload", CwmpInformEventCodes.M_UPLOAD);
    }

    /**
     * Handler Body
     *
     * @param request
     */
    @Override
    public void handle(final HttpServerRequest request) {
        /**
         * Parse the URL Path.
         */
        String path = request.path().substring(1);

        /**
         * Check for exit request
         */
        if (path.equals("exit")) {
            log.info("Exiting...");

            // Send an empty response
            request.response().end();

            // Call the Vert.X Container's exit() method for a clean exit
            container.exit();
            return;
        }

        /**
         * Parse the URL Path without the query string(s).
         *
         * Expecting "/{event-code}/{orgId}/{OUI}/{SN}"
         */
        final String[] pathParams = StringUtil.split(path, '/');
        if (pathParams.length != 4) {
            VertxUtils.badHttpRequest(
                    request,
                    "Invalid URL path! Must be in the format of \"/{event-code}/{orgId}/{OUI}/{SN}\"!");
            return;
        }
        final String orgId = pathParams[1];
        final String oui = pathParams[2];
        final long baseSn = Long.decode("0x" + pathParams[3]);

        /**
         * Find the CPE from DB and spawn a new CWMP Session
         */
        final String cpeKey = Cpe.getCpeKey(pathParams[1], pathParams[2], pathParams[3]);

        /**
         * Analyze the Event Code
         */
        final String eventCode = eventCodeMap.get(pathParams[0]);
        if (eventCode != null) {
            log.info("Simulating a " + eventCode + " event for CPE " + cpeKey);
        } else if (pathParams[0].equals("populateDb")){
            /**
             * Populate MongoDB directly with many CPE records into
             */
            String lastSnHexStr = request.params().get("lastSn");
            if (lastSnHexStr != null) {
                PopulateCpeDb.doPopulate(
                        vertx,
                        request,
                        pathParams[1],
                        pathParams[2],
                        pathParams[3],
                        lastSnHexStr
                );
            } else {
                VertxUtils.badHttpRequest(
                        request,
                        "Last SN must be specified via query parameter!");
            }
            return;
        } else {
            VertxUtils.badHttpRequest(
                    request,
                    "Invalid request type \"" + pathParams[0] + "\"!\n");
            return;
        }

        /**
         * Wait till the entire request body has been received
         */
        request.bodyHandler(new Handler<Buffer>() {
            public void handle(final Buffer body) {
                JsonObject newValues = null;

                /**
                 * Check new values for value change events
                 */
                if (CwmpInformEventCodes.VALUE_CHANGE.equals(eventCode)) {
                    if (body.length() > 0) {
                        newValues = new JsonObject(body.toString());
                    } else {
                        log.error("No new values found in payload!");
                        VertxUtils.badHttpRequest(request, "No new values found in payload!");
                        return;
                    }

                    /**
                     * Update MongoDB with the new value(s), and then query the DB and start session(s)
                     */
                    CpeSimUtils.updateCpeById(
                            vertx.eventBus(),
                            cpeKey,
                            newValues,
                            new MongoDbAsyncResultHandler(
                                    vertx,
                                    request,
                                    newValues,
                                    eventCode,
                                    baseSn,
                                    orgId,
                                    oui,
                                    baseSn
                            )
                    );
                } else {
                    /**
                     * Query the DB with a handler that starts a CWMP session based on the query result
                     */
                    CpeSimUtils.findCpeById(
                            vertx.eventBus(),
                            cpeKey,
                            new MongoDbAsyncResultHandler(
                                    vertx,
                                    request,
                                    newValues,
                                    eventCode,
                                    baseSn,
                                    orgId,
                                    oui,
                                    baseSn
                            )
                    );
                }
            }
        });
    }
}
