package com.example.keycloak.service;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Session cache using Keycloak's SingleUseObjectProvider.
 * This is cluster-safe and survives restarts (when using a persistent store).
 */
public class MockIdpSessionCache {

    private static final int TTL_SECONDS = 600; // 10 minutes
    private static final String KEY_PREFIX = "mock-idp-";

    /**
     * Data stored for each transaction
     */
    public static class SessionData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String tabId;
        public String clientUuid;
        public String authSessionId;
        public String callbackToken;
        
        public SessionData() {}
        
        public SessionData(String tabId, String clientUuid, String authSessionId, String callbackToken) {
            this.tabId = tabId;
            this.clientUuid = clientUuid;
            this.authSessionId = authSessionId;
            this.callbackToken = callbackToken;
        }
    }

    /**
     * Store session data with transaction ID
     */
    public static void put(KeycloakSession session, String transactionId, SessionData data) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("tabId", data.tabId);
        dataMap.put("clientUuid", data.clientUuid);
        dataMap.put("authSessionId", data.authSessionId);
        dataMap.put("callbackToken", data.callbackToken);
        
        singleUseStore.put(KEY_PREFIX + transactionId, TTL_SECONDS, dataMap);
    }

    /**
     * Retrieve session data by transaction ID
     */
    public static SessionData get(KeycloakSession session, String transactionId) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        Map<String, String> dataMap = singleUseStore.get(KEY_PREFIX + transactionId);
        
        if (dataMap == null) {
            return null;
        }
        
        SessionData data = new SessionData();
        data.tabId = dataMap.get("tabId");
        data.clientUuid = dataMap.get("clientUuid");
        data.authSessionId = dataMap.get("authSessionId");
        data.callbackToken = dataMap.get("callbackToken");
        return data;
    }

    /**
     * Remove session data (consume the single-use object)
     */
    public static SessionData remove(KeycloakSession session, String transactionId) {
        SingleUseObjectProvider singleUseStore = session.singleUseObjects();
        Map<String, String> dataMap = singleUseStore.remove(KEY_PREFIX + transactionId);
        
        if (dataMap == null) {
            return null;
        }
        
        SessionData data = new SessionData();
        data.tabId = dataMap.get("tabId");
        data.clientUuid = dataMap.get("clientUuid");
        data.authSessionId = dataMap.get("authSessionId");
        data.callbackToken = dataMap.get("callbackToken");
        return data;
    }
}
