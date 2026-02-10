const express = require('express');
const router = express.Router();
const jwtService = require('../services/jwt.service');
const keyService = require('../services/keys.service');
const testPersons = require('../config/testPersons.json');

// In-memory storage for authorization codes and tokens
const authorizationCodes = new Map();
const accessTokens = new Map();

/**
 * GET /.well-known/openid-configuration
 * OpenID Connect Discovery endpoint
 */
router.get('/.well-known/openid-configuration', (req, res) => {
    const issuer = process.env.ISSUER_URL || 'http://localhost/mock-idp';
    
    res.json({
        issuer: issuer,
        authorization_endpoint: `${issuer}/authorize`,
        token_endpoint: `${issuer}/token`,
        userinfo_endpoint: `${issuer}/userinfo`,
        jwks_uri: `${issuer}/.well-known/jwks.json`,
        response_types_supported: ['code'],
        subject_types_supported: ['public'],
        id_token_signing_alg_values_supported: ['RS256'],
        scopes_supported: ['openid', 'profile', 'email'],
        token_endpoint_auth_methods_supported: ['client_secret_post', 'client_secret_basic', 'none'],
        claims_supported: [
            'sub',
            'name',
            'given_name',
            'family_name',
            'email',
            'birthdate',
            'verification_status'
        ]
    });
});

/**
 * GET /authorize
 * OAuth 2.0 Authorization Endpoint
 * Redirects to person selection UI
 */
router.get('/authorize', (req, res) => {
    const {
        client_id,
        redirect_uri,
        response_type,
        scope,
        state,
        nonce
    } = req.query;

    // Validate required parameters
    if (!client_id || !redirect_uri || !response_type) {
        return res.status(400).json({
            error: 'invalid_request',
            error_description: 'Missing required parameters: client_id, redirect_uri, response_type'
        });
    }

    if (response_type !== 'code') {
        return res.status(400).json({
            error: 'unsupported_response_type',
            error_description: 'Only response_type=code is supported'
        });
    }

    // Store authorization request in session
    const authRequestId = generateRandomString(32);
    authorizationCodes.set(authRequestId, {
        client_id,
        redirect_uri,
        scope: scope || 'openid',
        state,
        nonce,
        createdAt: Date.now()
    });

    // Redirect to person selection UI (with /mock-idp prefix for Nginx routing)
    const baseUrl = process.env.ISSUER_URL || 'http://localhost/mock-idp';
    res.redirect(`${baseUrl}/verify-oidc?auth_request_id=${authRequestId}`);
});

/**
 * GET /verify-oidc
 * Person selection UI for OIDC flow
 */
router.get('/verify-oidc', (req, res) => {
    const { auth_request_id } = req.query;

    if (!auth_request_id) {
        return res.status(400).send('Missing auth_request_id');
    }

    const authRequest = authorizationCodes.get(auth_request_id);
    if (!authRequest) {
        return res.status(400).send('Invalid or expired auth_request_id');
    }

    // Return HTML page with embedded data
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mock Identity Provider - Verification</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            padding: 40px;
            max-width: 900px;
            width: 100%;
        }
        h1 {
            color: #333;
            margin-bottom: 10px;
            font-size: 28px;
        }
        .subtitle {
            color: #666;
            margin-bottom: 30px;
            font-size: 14px;
        }
        .info {
            background: #f7f9fc;
            border-left: 4px solid #667eea;
            padding: 15px;
            margin-bottom: 30px;
            border-radius: 4px;
        }
        .info p {
            margin: 5px 0;
            font-size: 13px;
            color: #555;
        }
        .info strong {
            color: #333;
        }
        .persons-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        .person-card {
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            padding: 20px;
            cursor: pointer;
            transition: all 0.3s ease;
            background: white;
        }
        .person-card:hover {
            border-color: #667eea;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
            transform: translateY(-2px);
        }
        .person-card.verified {
            border-color: #10b981;
        }
        .person-card.rejected {
            border-color: #ef4444;
        }
        .person-card.pending {
            border-color: #f59e0b;
        }
        .person-name {
            font-size: 18px;
            font-weight: 600;
            color: #333;
            margin-bottom: 8px;
        }
        .person-id {
            font-size: 12px;
            color: #999;
            margin-bottom: 8px;
        }
        .person-birthdate {
            font-size: 14px;
            color: #666;
            margin-bottom: 10px;
        }
        .person-status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
        }
        .status-verified {
            background: #d1fae5;
            color: #065f46;
        }
        .status-rejected {
            background: #fee2e2;
            color: #991b1b;
        }
        .status-pending {
            background: #fef3c7;
            color: #92400e;
        }
        .loading {
            display: none;
            text-align: center;
            padding: 20px;
            color: #667eea;
        }
        .error {
            background: #fee2e2;
            border-left: 4px solid #ef4444;
            color: #991b1b;
            padding: 15px;
            border-radius: 4px;
            margin-top: 20px;
            display: none;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔐 Identity Verification</h1>
        <p class="subtitle">Mock Identity Provider - Select a test person to continue</p>
        
        <div class="info">
            <p><strong>Client:</strong> ${authRequest.client_id}</p>
            <p><strong>Scope:</strong> ${authRequest.scope}</p>
        </div>

        <div class="persons-grid" id="personsGrid"></div>
        
        <div class="loading" id="loading">
            <p>Processing verification...</p>
        </div>

        <div class="error" id="error"></div>
    </div>

    <script>
        const persons = ${JSON.stringify(testPersons)};
        const authRequestId = ${JSON.stringify(auth_request_id)};

        const grid = document.getElementById('personsGrid');
        const loading = document.getElementById('loading');
        const errorDiv = document.getElementById('error');

        // Render persons
        persons.forEach(person => {
            const card = document.createElement('div');
            card.className = \`person-card \${person.status}\`;
            card.innerHTML = \`
                <div class="person-name">\${person.firstName} \${person.lastName}</div>
                <div class="person-id">ID: \${person.id}</div>
                <div class="person-birthdate">📅 \${person.birthdate}</div>
                <span class="person-status status-\${person.status}">\${person.status.toUpperCase()}</span>
            \`;
            card.onclick = () => selectPerson(person.id);
            grid.appendChild(card);
        });

        async function selectPerson(personId) {
            loading.style.display = 'block';
            grid.style.opacity = '0.5';
            grid.style.pointerEvents = 'none';
            errorDiv.style.display = 'none';

            try {
                const response = await fetch('/select-oidc', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        auth_request_id: authRequestId,
                        person_id: personId
                    })
                });

                const result = await response.json();

                if (!response.ok) {
                    throw new Error(result.error_description || result.error || 'Selection failed');
                }

                // Redirect back to client
                window.location.href = result.redirect_uri;

            } catch (error) {
                console.error('Error:', error);
                errorDiv.textContent = 'Error: ' + error.message;
                errorDiv.style.display = 'block';
                loading.style.display = 'none';
                grid.style.opacity = '1';
                grid.style.pointerEvents = 'auto';
            }
        }
    </script>
</body>
</html>
    `;

    res.setHeader('Content-Type', 'text/html');
    res.send(html);
});

/**
 * POST /select-oidc
 * Handle person selection and generate authorization code
 */
router.post('/select-oidc', express.json(), (req, res) => {
    const { auth_request_id, person_id } = req.body;

    if (!auth_request_id || !person_id) {
        return res.status(400).json({
            error: 'invalid_request',
            error_description: 'Missing auth_request_id or person_id'
        });
    }

    // Get authorization request
    const authRequest = authorizationCodes.get(auth_request_id);
    if (!authRequest) {
        return res.status(400).json({
            error: 'invalid_request',
            error_description: 'Invalid or expired auth_request_id'
        });
    }

    // Find person
    const person = testPersons.find(p => p.id === person_id);
    if (!person) {
        return res.status(400).json({
            error: 'invalid_request',
            error_description: 'Invalid person_id'
        });
    }

    // Generate authorization code
    const code = generateRandomString(32);
    
    // Store authorization code with person data
    authorizationCodes.set(code, {
        ...authRequest,
        person,
        createdAt: Date.now(),
        used: false
    });

    // Remove auth request
    authorizationCodes.delete(auth_request_id);

    // Build redirect URI
    const redirectUrl = new URL(authRequest.redirect_uri);
    redirectUrl.searchParams.set('code', code);
    if (authRequest.state) {
        redirectUrl.searchParams.set('state', authRequest.state);
    }

    console.log(`Authorization code issued: ${code.substring(0, 8)}... for person ${person.firstName} ${person.lastName}`);

    res.json({
        redirect_uri: redirectUrl.toString()
    });
});

/**
 * POST /token
 * OAuth 2.0 Token Endpoint
 */
router.post('/token', express.urlencoded({ extended: true }), (req, res) => {
    const { grant_type, code, redirect_uri, client_id, client_secret } = req.body;

    console.log('Token request:', { grant_type, code: code?.substring(0, 8) + '...', redirect_uri, client_id });

    // Validate grant type
    if (grant_type !== 'authorization_code') {
        return res.status(400).json({
            error: 'unsupported_grant_type',
            error_description: 'Only grant_type=authorization_code is supported'
        });
    }

    // Validate required parameters
    if (!code || !redirect_uri) {
        return res.status(400).json({
            error: 'invalid_request',
            error_description: 'Missing required parameters: code, redirect_uri'
        });
    }

    // Get authorization code data
    const authData = authorizationCodes.get(code);
    if (!authData) {
        return res.status(400).json({
            error: 'invalid_grant',
            error_description: 'Invalid or expired authorization code'
        });
    }

    // Check if already used
    if (authData.used) {
        authorizationCodes.delete(code);
        return res.status(400).json({
            error: 'invalid_grant',
            error_description: 'Authorization code already used'
        });
    }

    // Validate redirect URI matches
    if (authData.redirect_uri !== redirect_uri) {
        return res.status(400).json({
            error: 'invalid_grant',
            error_description: 'Redirect URI mismatch'
        });
    }

    // Check expiration (5 minutes)
    const codeAge = Date.now() - authData.createdAt;
    if (codeAge > 5 * 60 * 1000) {
        authorizationCodes.delete(code);
        return res.status(400).json({
            error: 'invalid_grant',
            error_description: 'Authorization code expired'
        });
    }

    // Mark code as used
    authData.used = true;

    // Generate tokens
    const person = authData.person;
    const issuer = process.env.ISSUER_URL || 'http://localhost/mock-idp';
    
    const claims = {
        sub: person.id,
        name: `${person.firstName} ${person.lastName}`,
        given_name: person.firstName,
        family_name: person.lastName,
        email: person.email,
        birthdate: person.birthdate,
        verification_status: person.status
    };

    // Generate ID token
    const idToken = jwtService.generateToken({
        ...claims,
        aud: authData.client_id,
        nonce: authData.nonce
    });

    // Generate access token
    const accessToken = generateRandomString(48);
    accessTokens.set(accessToken, {
        claims,
        client_id: authData.client_id,
        scope: authData.scope,
        createdAt: Date.now()
    });

    console.log(`Tokens issued for person: ${person.firstName} ${person.lastName} (${person.id})`);

    // Return tokens
    res.json({
        access_token: accessToken,
        token_type: 'Bearer',
        expires_in: 3600,
        id_token: idToken,
        scope: authData.scope
    });

    // Clean up authorization code after short delay
    setTimeout(() => {
        authorizationCodes.delete(code);
    }, 10000);
});

/**
 * GET /userinfo
 * OAuth 2.0 UserInfo Endpoint
 */
router.get('/userinfo', (req, res) => {
    // Extract access token from Authorization header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({
            error: 'invalid_token',
            error_description: 'Missing or invalid Authorization header'
        });
    }

    const accessToken = authHeader.substring(7);
    
    // Get token data
    const tokenData = accessTokens.get(accessToken);
    if (!tokenData) {
        return res.status(401).json({
            error: 'invalid_token',
            error_description: 'Invalid or expired access token'
        });
    }

    // Check expiration (1 hour)
    const tokenAge = Date.now() - tokenData.createdAt;
    if (tokenAge > 60 * 60 * 1000) {
        accessTokens.delete(accessToken);
        return res.status(401).json({
            error: 'invalid_token',
            error_description: 'Access token expired'
        });
    }

    console.log(`UserInfo request for: ${tokenData.claims.name} (${tokenData.claims.sub})`);

    // Return user claims
    res.json(tokenData.claims);
});

/**
 * Helper function to generate random string
 */
function generateRandomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

// Cleanup expired entries every 5 minutes
setInterval(() => {
    const now = Date.now();
    const maxAge = 5 * 60 * 1000; // 5 minutes

    // Clean authorization codes
    for (const [key, value] of authorizationCodes.entries()) {
        if (now - value.createdAt > maxAge) {
            authorizationCodes.delete(key);
        }
    }

    // Clean access tokens (1 hour)
    for (const [key, value] of accessTokens.entries()) {
        if (now - value.createdAt > 60 * 60 * 1000) {
            accessTokens.delete(key);
        }
    }
}, 5 * 60 * 1000);

module.exports = router;
