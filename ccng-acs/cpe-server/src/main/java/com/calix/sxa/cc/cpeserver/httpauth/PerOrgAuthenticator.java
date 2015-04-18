package com.calix.sxa.cc.cpeserver.httpauth;

import com.calix.sxa.cc.model.Organization;
import org.slf4j.Logger;
import org.vertx.java.core.json.JsonObject;

/**
 * Project:  SXA-CC
 *
 * Abstract Per-Organization Authenticator
 *
 * @author: jqin
 */
public abstract class PerOrgAuthenticator extends Organization{
    // Logger Instance (to be initialized)
    public Logger log;

    /**
     * Constructor by a JSON Object.
     *
     * @param jsonObject
     */
    public PerOrgAuthenticator(JsonObject jsonObject) {
        super(jsonObject);
    }

    /**
     * Initialize Logger Instance.
     */
    public abstract void initLogger(Logger logger);


    /**
     * Get an Auth Challenge String.
     */
    public abstract String getChallengeString();

    /**
     * Authenticate the auth header received from CPE.
     *
     * @return true if successfully verified; or false
     *
     *         Upon failure, an HTTP "NOT AUTHORIZED" response will be sent to CPE.
     */
    public abstract boolean verifyAuthHeader(String authHeader);


    /**
     * Check to see if the auth header contains the Zero-Touch Credentials.
     *
     * @return true if yes; or false
     */
    public abstract boolean hasZeroTouchCredentials(String authHeader);
}
