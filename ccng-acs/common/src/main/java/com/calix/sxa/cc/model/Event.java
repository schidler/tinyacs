package com.calix.sxa.cc.model;

import com.calix.sxa.SxaVertxException;
import com.calix.sxa.VertxMongoUtils;
import com.calix.sxa.cc.util.AcsConstants;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

/**
 * Project:  SXA-CC (aka CCFG)
 *
 * Event Data model.
 *
 * @author: jqin
 */
public class Event {
    /**
     * DB Collection Name
     */
    public static final String DB_COLLECTION_NAME = "sxacc-events";

    /**
     * Field Name Constants
     */
    public static final String FIELD_NAME_TIMESTAMP = "timestamp";
    public static final String FIELD_NAME_DEVICE_SN = "deviceSn";
    public static final String FIELD_NAME_TYPE = "type";
    public static final String FIELD_NAME_SEVERITY = "severity";
    public static final String FIELD_NAME_SOURCE = "source";
    public static final String FIELD_NAME_DETAILS = "details";

    /**
     * Save an event
     */
    public static void saveEvent(
            EventBus eventBus,
            String orgId,
            String deviceSn,
            EventTypeEnum eventType,
            EventSourceEnum source,
            JsonObject details) {
        JsonObject jsonObject = new JsonObject()
                .putString(AcsConstants.FIELD_NAME_ORG_ID, orgId)
                .putObject(FIELD_NAME_TIMESTAMP, VertxMongoUtils.getDateObject())
                .putString(FIELD_NAME_TYPE, eventType.typeString)
                .putString(FIELD_NAME_SEVERITY, eventType.severity.name())
                .putString(FIELD_NAME_SOURCE, source.name());

        if (details != null) {
            jsonObject.putObject(FIELD_NAME_DETAILS, details);
        }
        if (deviceSn != null) {
            jsonObject.putString(FIELD_NAME_DEVICE_SN, deviceSn);
        }

        // Persist it
        //log.debug("Persisting a " + type + " ...");
        try {
            VertxMongoUtils.save(
                    eventBus,
                    DB_COLLECTION_NAME,
                    jsonObject,
                    null
            );
        } catch (SxaVertxException e) {
            e.printStackTrace();
        }
    }
}