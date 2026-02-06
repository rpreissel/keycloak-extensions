/**
 * Middleware to validate required query parameters
 */
function validateVerifyParams(req, res, next) {
    const { client_id, transaction_id, callback_token, state, redirect_uri, callback_url } = req.query;

    const missing = [];
    if (!client_id) missing.push('client_id');
    if (!transaction_id) missing.push('transaction_id');
    if (!callback_token) missing.push('callback_token');
    if (!state) missing.push('state');
    if (!redirect_uri) missing.push('redirect_uri');
    if (!callback_url) missing.push('callback_url');

    if (missing.length > 0) {
        return res.status(400).json({
            error: 'missing_parameters',
            message: `Missing required parameters: ${missing.join(', ')}`
        });
    }

    next();
}

/**
 * Middleware to validate request body for person selection
 */
function validateSelectBody(req, res, next) {
    const { personId, transactionId, callbackToken, clientId, redirectUri, callbackUrl } = req.body;

    const missing = [];
    if (!personId) missing.push('personId');
    if (!transactionId) missing.push('transactionId');
    if (!callbackToken) missing.push('callbackToken');
    if (!clientId) missing.push('clientId');
    if (!redirectUri) missing.push('redirectUri');
    if (!callbackUrl) missing.push('callbackUrl');

    if (missing.length > 0) {
        return res.status(400).json({
            error: 'missing_fields',
            message: `Missing required fields: ${missing.join(', ')}`
        });
    }

    next();
}

module.exports = {
    validateVerifyParams,
    validateSelectBody
};
