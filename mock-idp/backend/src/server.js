require('dotenv').config();
const express = require('express');
const cors = require('cors');
const keyService = require('./services/keys.service');
const authRoutes = require('./routes/auth');
const callbackRoutes = require('./routes/callback');
const utilityRoutes = require('./routes/utility');
const oidcRoutes = require('./routes/oidc');

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors({
    origin: process.env.FRONTEND_URL || 'http://localhost:3002',
    credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Request logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// Routes
app.use('/', oidcRoutes);      // OIDC endpoints (/.well-known/openid-configuration, /authorize, /token, /userinfo)
app.use('/', authRoutes);      // /verify (legacy)
app.use('/', callbackRoutes);  // /select (legacy)
app.use('/', utilityRoutes);   // /health, /.well-known/jwks.json

// Root endpoint
app.get('/', (req, res) => {
    const issuer = process.env.ISSUER_URL || 'http://localhost/mock-idp';
    res.json({
        service: 'Mock Identity Provider',
        version: '2.0.0',
        mode: 'OIDC Provider',
        issuer: issuer,
        endpoints: {
            // OIDC endpoints
            discovery: `${issuer}/.well-known/openid-configuration`,
            authorize: `${issuer}/authorize`,
            token: `${issuer}/token`,
            userinfo: `${issuer}/userinfo`,
            jwks: `${issuer}/.well-known/jwks.json`,
            // Legacy endpoints
            verify: 'GET /verify?client_id=...&transaction_id=...&callback_token=...&state=...&redirect_uri=...',
            select: 'POST /select (JSON body)',
            health: 'GET /health'
        }
    });
});

// Error handler
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(500).json({
        error: 'server_error',
        message: err.message
    });
});

// Initialize and start server
async function start() {
    try {
        console.log('========================================');
        console.log('Mock Identity Provider');
        console.log('========================================');
        
        // Initialize key service
        console.log('Initializing RSA key service...');
        await keyService.initialize();
        
        // Start server
        app.listen(PORT, () => {
            const issuer = process.env.ISSUER_URL || 'http://localhost/mock-idp';
            console.log('========================================');
            console.log(`Server running on port ${PORT}`);
            console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
            console.log(`Issuer URL: ${issuer}`);
            console.log('========================================');
            console.log('OIDC Endpoints:');
            console.log(`  - Discovery:   ${issuer}/.well-known/openid-configuration`);
            console.log(`  - Authorize:   ${issuer}/authorize`);
            console.log(`  - Token:       ${issuer}/token`);
            console.log(`  - UserInfo:    ${issuer}/userinfo`);
            console.log(`  - JWKS:        ${issuer}/.well-known/jwks.json`);
            console.log('');
            console.log('Legacy Endpoints:');
            console.log(`  - Verify:      http://localhost:${PORT}/verify`);
            console.log(`  - Health:      http://localhost:${PORT}/health`);
            console.log('========================================');
        });
    } catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}

// Handle graceful shutdown
process.on('SIGTERM', () => {
    console.log('SIGTERM received, shutting down gracefully...');
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('SIGINT received, shutting down gracefully...');
    process.exit(0);
});

// Start the server
start();
