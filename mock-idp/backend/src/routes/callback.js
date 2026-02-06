const express = require('express');
const router = express.Router();
const axios = require('axios');
const { validateSelectBody } = require('../middleware/validator');
const clientService = require('../services/client.service');
const jwtService = require('../services/jwt.service');
const testPersons = require('../config/testPersons.json');

/**
 * POST /select
 * Process person selection, generate JWT, callback to Keycloak, redirect
 * Body: { personId, transactionId, callbackToken, clientId, redirectUri }
 */
router.post('/select', validateSelectBody, async (req, res) => {
    const { personId, transactionId, callbackToken, clientId, redirectUri, callbackUrl } = req.body;

    try {
        // Validate person exists
        const person = testPersons.find(p => p.id === personId);
        if (!person) {
            return res.status(400).json({
                error: 'invalid_person',
                message: `Person ${personId} not found`
            });
        }

        // Validate client
        const client = clientService.getClient(clientId);
        if (!client) {
            return res.status(400).json({
                error: 'invalid_client',
                message: `Unknown client: ${clientId}`
            });
        }

        // Validate redirect URI
        if (!clientService.isValidRedirectUri(clientId, redirectUri)) {
            return res.status(400).json({
                error: 'invalid_redirect_uri',
                message: `Redirect URI not allowed for client ${clientId}`
            });
        }

        // Generate JWT
        console.log(`Generating JWT for person ${personId}, transaction ${transactionId}`);
        const jwt = await jwtService.generateIdentityToken(personId, transactionId, clientId);

        // Call Keycloak callback endpoint
        console.log(`Calling Keycloak callback: ${callbackUrl}`);
        try {
            const callbackResponse = await axios.post(callbackUrl, {
                jwt: jwt,
                transaction_id: transactionId,
                callback_token: callbackToken
            }, {
                headers: {
                    'Content-Type': 'application/json'
                },
                timeout: 10000 // 10 second timeout
            });

            console.log(`Callback response status: ${callbackResponse.status}`);
        } catch (callbackError) {
            console.error('Callback to Keycloak failed:', callbackError.message);
            if (callbackError.response) {
                console.error('Response status:', callbackError.response.status);
                console.error('Response data:', callbackError.response.data);
            }
            
            // Continue anyway - Keycloak might not be configured yet
            console.log('Continuing despite callback failure (development mode)');
        }

        // Return redirect URI to frontend
        console.log(`Identity verification complete. Redirecting to: ${redirectUri}`);
        res.json({
            success: true,
            redirect_uri: redirectUri,
            person: {
                id: person.id,
                name: `${person.firstName} ${person.lastName}`,
                status: person.status
            }
        });

    } catch (error) {
        console.error('Error processing person selection:', error);
        res.status(500).json({
            error: 'processing_error',
            message: error.message
        });
    }
});

module.exports = router;
