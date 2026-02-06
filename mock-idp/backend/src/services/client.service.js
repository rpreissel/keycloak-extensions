const clients = require('../config/clients.json');

class ClientService {
    /**
     * Get client configuration by client ID
     * @param {string} clientId - Client ID
     * @returns {object|null} Client configuration or null if not found
     */
    getClient(clientId) {
        return clients[clientId] || null;
    }

    /**
     * Get callback URL for a client
     * @param {string} clientId - Client ID
     * @returns {string|null} Callback URL or null if client not found
     */
    getCallbackUrl(clientId) {
        const client = this.getClient(clientId);
        return client ? client.callback_url : null;
    }

    /**
     * Validate redirect URI against client's allowed URIs
     * @param {string} clientId - Client ID
     * @param {string} redirectUri - Redirect URI to validate
     * @returns {boolean} True if valid, false otherwise
     */
    isValidRedirectUri(clientId, redirectUri) {
        const client = this.getClient(clientId);
        if (!client || !client.allowed_redirect_uris) {
            return false;
        }

        // Check if redirect URI matches any allowed pattern
        return client.allowed_redirect_uris.some(pattern => {
            // Convert wildcard pattern to regex
            const regexPattern = pattern
                .replace(/\./g, '\\.')  // Escape dots
                .replace(/\*/g, '.*');   // Convert * to .*
            
            const regex = new RegExp(`^${regexPattern}$`);
            return regex.test(redirectUri);
        });
    }

    /**
     * Get all registered client IDs
     * @returns {string[]} Array of client IDs
     */
    getAllClientIds() {
        return Object.keys(clients);
    }
}

// Singleton instance
const clientService = new ClientService();

module.exports = clientService;
