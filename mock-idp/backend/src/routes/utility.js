const express = require('express');
const router = express.Router();
const keyService = require('../services/keys.service');

/**
 * GET /health
 * Health check endpoint for container orchestration
 */
router.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        service: 'mock-identity-provider',
        timestamp: new Date().toISOString()
    });
});

/**
 * GET /.well-known/jwks.json
 * JSON Web Key Set endpoint for public key distribution
 * Keycloak can use this to verify JWTs
 */
router.get('/.well-known/jwks.json', async (req, res) => {
    try {
        const jwks = await keyService.getJWKS();
        res.json(jwks);
    } catch (error) {
        console.error('Error retrieving JWKS:', error);
        res.status(500).json({
            error: 'server_error',
            message: 'Failed to retrieve public keys'
        });
    }
});

module.exports = router;
