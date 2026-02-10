const express = require('express');
const session = require('express-session');
const axios = require('axios');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3002;

// Keycloak configuration
const KEYCLOAK_URL = process.env.KEYCLOAK_URL || 'http://localhost';
const KEYCLOAK_INTERNAL_URL = process.env.KEYCLOAK_INTERNAL_URL || 'http://reverse-proxy'; // Use Nginx for internal calls
const REALM = process.env.KEYCLOAK_REALM || 'test-realm';
const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || 'web-test-client';
const CLIENT_SECRET = process.env.KEYCLOAK_CLIENT_SECRET || 'your-client-secret'; // Will be set via terraform
const REDIRECT_URI = process.env.REDIRECT_URI || 'http://localhost/app/';

// Keycloak endpoints
// Use public URL for browser redirects
const KEYCLOAK_AUTH_URL = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth`;
const KEYCLOAK_LOGOUT_URL = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`;
// Use internal URL (reverse-proxy) for backend API calls
// This goes through Nginx which routes to Keycloak, preserving the correct hostname
const KEYCLOAK_TOKEN_URL = `${KEYCLOAK_INTERNAL_URL}/realms/${REALM}/protocol/openid-connect/token`;

// Middleware
app.use(express.json());
app.use(express.static(path.join(__dirname, '../public')));

// Session configuration
app.use(session({
    secret: 'your-session-secret-change-in-production',
    resave: false,
    saveUninitialized: false,
    cookie: {
        secure: false, // Set to true in production with HTTPS
        httpOnly: true,
        maxAge: 24 * 60 * 60 * 1000 // 24 hours
    }
}));

// CORS for local development
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', req.headers.origin || '*');
    res.header('Access-Control-Allow-Credentials', 'true');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
    
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

/**
 * Initiate login - redirect to Keycloak
 */
app.get('/login', (req, res) => {
    const authUrl = new URL(KEYCLOAK_AUTH_URL);
    authUrl.searchParams.append('client_id', CLIENT_ID);
    authUrl.searchParams.append('redirect_uri', REDIRECT_URI);
    authUrl.searchParams.append('response_type', 'code');
    authUrl.searchParams.append('scope', 'openid profile email');
    
    // Generate state for CSRF protection
    const state = Math.random().toString(36).substring(7);
    req.session.oauth_state = state;
    authUrl.searchParams.append('state', state);
    
    console.log('🔐 Redirecting to Keycloak login:', authUrl.toString());
    res.redirect(authUrl.toString());
});

/**
 * OAuth callback - exchange code for tokens
 */
app.get('/callback', async (req, res) => {
    const { code, state } = req.query;
    
    console.log('📥 OAuth callback received - code:', code?.substring(0, 20) + '...');
    
    if (!code) {
        return res.json({ error: 'No authorization code received' });
    }
    
    // Verify state (CSRF protection)
    if (state && req.session.oauth_state && state !== req.session.oauth_state) {
        return res.json({ error: 'Invalid state parameter' });
    }
    delete req.session.oauth_state;
    
    try {
        // Exchange code for tokens
        const tokenResponse = await axios.post(KEYCLOAK_TOKEN_URL, new URLSearchParams({
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: REDIRECT_URI,
            client_id: CLIENT_ID,
            client_secret: CLIENT_SECRET
        }), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        });
        
        const tokens = tokenResponse.data;
        console.log('✅ Tokens received successfully');
        
        // Parse ID token to get user info
        const idTokenPayload = parseJwt(tokens.id_token);
        
        // Store tokens in session
        req.session.tokens = tokens;
        req.session.user = {
            sub: idTokenPayload.sub,
            username: idTokenPayload.preferred_username,
            email: idTokenPayload.email,
            name: idTokenPayload.name
        };
        
        res.json({
            tokens: tokens,
            user: req.session.user
        });
        
    } catch (error) {
        console.error('❌ Token exchange failed:', error.response?.data || error.message);
        res.json({ 
            error: 'Token exchange failed',
            details: error.response?.data || error.message
        });
    }
});

/**
 * Check current session
 */
app.get('/session', (req, res) => {
    if (req.session.tokens && req.session.user) {
        res.json({
            loggedIn: true,
            tokens: req.session.tokens,
            user: req.session.user
        });
    } else {
        res.json({ loggedIn: false });
    }
});

/**
 * Refresh tokens
 */
app.post('/refresh', async (req, res) => {
    if (!req.session.tokens?.refresh_token) {
        return res.json({ error: 'No refresh token available' });
    }
    
    try {
        const tokenResponse = await axios.post(KEYCLOAK_TOKEN_URL, new URLSearchParams({
            grant_type: 'refresh_token',
            refresh_token: req.session.tokens.refresh_token,
            client_id: CLIENT_ID,
            client_secret: CLIENT_SECRET
        }), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        });
        
        const tokens = tokenResponse.data;
        console.log('🔄 Tokens refreshed successfully');
        
        // Update session
        req.session.tokens = tokens;
        
        res.json({
            tokens: tokens,
            user: req.session.user
        });
        
    } catch (error) {
        console.error('❌ Token refresh failed:', error.response?.data || error.message);
        res.json({ 
            error: 'Token refresh failed',
            details: error.response?.data || error.message
        });
    }
});

/**
 * Logout
 */
app.post('/logout', async (req, res) => {
    const refreshToken = req.session.tokens?.refresh_token;
    
    // Clear session first
    req.session.destroy((err) => {
        if (err) {
            console.error('Session destroy error:', err);
        }
    });
    
    // Logout from Keycloak
    if (refreshToken) {
        try {
            await axios.post(KEYCLOAK_LOGOUT_URL, new URLSearchParams({
                client_id: CLIENT_ID,
                client_secret: CLIENT_SECRET,
                refresh_token: refreshToken
            }), {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            });
            console.log('👋 Logged out from Keycloak');
        } catch (error) {
            console.error('Keycloak logout error:', error.response?.data || error.message);
        }
    }
    
    res.json({ success: true });
});

/**
 * Health check
 */
app.get('/health', (req, res) => {
    res.json({ 
        status: 'ok',
        keycloak: KEYCLOAK_URL,
        realm: REALM
    });
});

/**
 * Parse JWT token
 */
function parseJwt(token) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(Buffer.from(base64, 'base64').toString().split('').map(c => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    
    return JSON.parse(jsonPayload);
}

// Start server
app.listen(PORT, () => {
    console.log('╔════════════════════════════════════════════════════════╗');
    console.log('║         Keycloak Test Client Started                  ║');
    console.log('╚════════════════════════════════════════════════════════╝');
    console.log('');
    console.log(`🌐 Application: http://localhost:${PORT}`);
    console.log(`🔐 Keycloak:    ${KEYCLOAK_URL}`);
    console.log(`📋 Realm:       ${REALM}`);
    console.log(`🔑 Client ID:   ${CLIENT_ID}`);
    console.log('');
    console.log('Ready for testing! Open http://localhost:3002 in your browser');
    console.log('');
});
