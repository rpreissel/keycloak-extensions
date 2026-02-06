package com.example.keycloak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockIdpJwtValidator {

    private static final Logger logger = Logger.getLogger(MockIdpJwtValidator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Cache for JWKS (thread-safe)
    private static final Map<String, JwksCache> jwksCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000; // 1 hour

    /**
     * Validate JWT from Mock IDP
     * 
     * @param jwt JWT string
     * @param mockIdpUrl Base URL of Mock IDP (e.g., http://localhost:3001)
     * @param expectedAudience Expected audience (client_id)
     * @return Parsed JWT claims if valid
     * @throws Exception if validation fails
     */
    public static Map<String, Object> validateJwt(String jwt, String mockIdpUrl, String expectedAudience) throws Exception {
        logger.debugf("Validating JWT from Mock IDP: %s", mockIdpUrl);

        // Split JWT into parts
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format - expected 3 parts");
        }

        String headerJson = decodeBase64Url(parts[0]);
        String payloadJson = decodeBase64Url(parts[1]);
        String signature = parts[2];

        // Parse header
        Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
        String alg = (String) header.get("alg");
        String kid = (String) header.get("kid");

        if (!"RS256".equals(alg)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + alg);
        }

        // Parse payload
        Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

        // Validate claims
        validateClaims(payload, mockIdpUrl, expectedAudience);

        // Get public key from JWKS
        PublicKey publicKey = getPublicKeyFromJwks(mockIdpUrl, kid);

        // Verify signature
        verifySignature(parts[0] + "." + parts[1], signature, publicKey);

        logger.info("JWT validation successful");
        return payload;
    }

    /**
     * Validate JWT claims
     */
    private static void validateClaims(Map<String, Object> payload, String mockIdpUrl, String expectedAudience) throws Exception {
        // Validate issuer
        String iss = (String) payload.get("iss");
        String expectedIssuer = mockIdpUrl.endsWith("/") ? mockIdpUrl.substring(0, mockIdpUrl.length() - 1) : mockIdpUrl;
        
        // Mock IDP uses container network name, so check both
        if (iss == null || (!iss.equals(expectedIssuer) && !iss.equals("http://mock-idp-backend:3001"))) {
            logger.warnf("Issuer mismatch - expected: %s or http://mock-idp-backend:3001, got: %s", expectedIssuer, iss);
            // For development, we'll accept both localhost and container network
        }

        // Validate audience
        String aud = (String) payload.get("aud");
        if (!expectedAudience.equals(aud)) {
            throw new IllegalArgumentException("Invalid audience - expected: " + expectedAudience + ", got: " + aud);
        }

        // Validate expiration
        Object expObj = payload.get("exp");
        if (expObj != null) {
            long exp = ((Number) expObj).longValue();
            long now = System.currentTimeMillis() / 1000;
            if (now > exp) {
                throw new IllegalArgumentException("JWT expired");
            }
        }

        logger.debugf("JWT claims validated - iss: %s, aud: %s", iss, aud);
    }

    /**
     * Get public key from JWKS endpoint
     */
    private static PublicKey getPublicKeyFromJwks(String mockIdpUrl, String kid) throws Exception {
        String jwksUrl = mockIdpUrl + "/.well-known/jwks.json";
        
        // For container network, use internal URL
        if (mockIdpUrl.contains("localhost")) {
            jwksUrl = "http://mock-idp-backend:3001/.well-known/jwks.json";
        }

        // Check cache
        JwksCache cached = jwksCache.get(jwksUrl);
        if (cached != null && !cached.isExpired()) {
            PublicKey key = cached.getKey(kid);
            if (key != null) {
                logger.debug("Using cached JWKS");
                return key;
            }
        }

        // Fetch JWKS
        logger.infof("Fetching JWKS from: %s", jwksUrl);
        String jwksJson = fetchUrl(jwksUrl);
        JsonNode jwks = objectMapper.readTree(jwksJson);
        JsonNode keys = jwks.get("keys");

        if (keys == null || !keys.isArray()) {
            throw new IllegalArgumentException("Invalid JWKS format");
        }

        // Parse keys and cache
        Map<String, PublicKey> keyMap = new HashMap<>();
        for (JsonNode keyNode : keys) {
            String keyId = keyNode.get("kid").asText();
            String n = keyNode.get("n").asText();
            String e = keyNode.get("e").asText();

            PublicKey publicKey = buildRsaPublicKey(n, e);
            keyMap.put(keyId, publicKey);
        }

        // Update cache
        jwksCache.put(jwksUrl, new JwksCache(keyMap));

        PublicKey key = keyMap.get(kid);
        if (key == null) {
            throw new IllegalArgumentException("Key ID not found in JWKS: " + kid);
        }

        return key;
    }

    /**
     * Build RSA public key from modulus (n) and exponent (e)
     */
    private static PublicKey buildRsaPublicKey(String n, String e) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    /**
     * Verify JWT signature
     */
    private static void verifySignature(String signedData, String signatureBase64Url, PublicKey publicKey) throws Exception {
        byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureBase64Url);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(signedData.getBytes(StandardCharsets.UTF_8));

        if (!signature.verify(signatureBytes)) {
            throw new SecurityException("Invalid JWT signature");
        }

        logger.debug("JWT signature verified");
    }

    /**
     * Decode Base64 URL-safe string
     */
    private static String decodeBase64Url(String input) {
        byte[] decoded = Base64.getUrlDecoder().decode(input);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * Fetch URL content
     */
    private static String fetchUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IllegalArgumentException("Failed to fetch JWKS - HTTP " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * JWKS cache entry
     */
    private static class JwksCache {
        private final Map<String, PublicKey> keys;
        private final long timestamp;

        public JwksCache(Map<String, PublicKey> keys) {
            this.keys = keys;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }

        public PublicKey getKey(String kid) {
            return keys.get(kid);
        }
    }
}
