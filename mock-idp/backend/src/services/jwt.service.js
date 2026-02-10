const jwt = require('jsonwebtoken');
const keyService = require('./keys.service');
const testPersons = require('../config/testPersons.json');

class JWTService {
    constructor() {
        this.issuer = process.env.ISSUER_URL || 'http://mock-idp-backend:3001';
    }

    /**
     * Generate a signed JWT for identity verification
     * @param {string} personId - ID of the selected person
     * @param {string} transactionId - Transaction ID from Keycloak
     * @param {string} clientId - Client ID (audience)
     * @returns {string} Signed JWT
     */
    async generateIdentityToken(personId, transactionId, clientId) {
        // Find person data
        const person = testPersons.find(p => p.id === personId);
        if (!person) {
            throw new Error(`Person with ID ${personId} not found`);
        }

        // Get private key
        const privateKey = keyService.getPrivateKey();
        const privateKeyPem = privateKey.toPEM(true);

        // Current timestamp
        const now = Math.floor(Date.now() / 1000);

        // JWT payload
        const payload = {
            // Standard claims
            sub: person.id,                    // Subject (person ID)
            aud: clientId,                     // Audience (client ID)
            iss: this.issuer,                  // Issuer (this service)
            iat: now,                          // Issued at
            exp: now + 3600,                   // Expires in 1 hour
            
            // Custom claims
            tid: transactionId,                // Transaction ID
            email: person.email,               // Email address
            name: `${person.firstName} ${person.lastName}`,
            given_name: person.firstName,
            family_name: person.lastName,
            birthdate: person.birthdate,
            verification_status: person.status
        };

        // Sign JWT with RS256
        const token = jwt.sign(payload, privateKeyPem, {
            algorithm: 'RS256',
            keyid: privateKey.kid || 'mock-idp-key-1'
        });

        return token;
    }

    /**
     * Generate OIDC ID Token
     * @param {object} claims - User claims to include in token
     * @returns {string} Signed ID Token
     */
    generateToken(claims) {
        // Get private key
        const privateKey = keyService.getPrivateKey();
        const privateKeyPem = privateKey.toPEM(true);

        // Current timestamp
        const now = Math.floor(Date.now() / 1000);

        // ID Token payload (OIDC standard)
        const payload = {
            // Required OIDC claims
            iss: this.issuer,                  // Issuer
            sub: claims.sub,                   // Subject
            aud: claims.aud,                   // Audience
            iat: now,                          // Issued at
            exp: now + 3600,                   // Expires in 1 hour
            
            // Optional OIDC claims
            ...claims
        };

        // Sign JWT with RS256
        const token = jwt.sign(payload, privateKeyPem, {
            algorithm: 'RS256',
            keyid: privateKey.kid || 'mock-idp-key-1'
        });

        return token;
    }

    /**
     * Verify a JWT (for testing purposes)
     * @param {string} token - JWT to verify
     * @returns {object} Decoded payload
     */
    async verifyToken(token) {
        const publicKey = keyService.getPublicKey();
        const publicKeyPem = publicKey.toPEM(false);

        try {
            const decoded = jwt.verify(token, publicKeyPem, {
                algorithms: ['RS256'],
                issuer: this.issuer
            });
            return decoded;
        } catch (error) {
            console.error('JWT verification failed:', error.message);
            throw error;
        }
    }
}

// Singleton instance
const jwtService = new JWTService();

module.exports = jwtService;
