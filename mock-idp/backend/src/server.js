require('dotenv').config();
const express = require('express');
const cors = require('cors');
const keyService = require('./services/keys.service');
const authRoutes = require('./routes/auth');
const callbackRoutes = require('./routes/callback');
const utilityRoutes = require('./routes/utility');

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
app.use('/', authRoutes);      // /verify
app.use('/', callbackRoutes);  // /select
app.use('/', utilityRoutes);   // /health, /.well-known/jwks.json

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        service: 'Mock Identity Provider',
        version: '1.0.0',
        endpoints: {
            verify: 'GET /verify?client_id=...&transaction_id=...&callback_token=...&state=...&redirect_uri=...',
            select: 'POST /select (JSON body)',
            health: 'GET /health',
            jwks: 'GET /.well-known/jwks.json'
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
            console.log('========================================');
            console.log(`Server running on port ${PORT}`);
            console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
            console.log(`Issuer URL: ${process.env.ISSUER_URL}`);
            console.log('========================================');
            console.log('Endpoints:');
            console.log(`  - Verify:  http://localhost:${PORT}/verify`);
            console.log(`  - Health:  http://localhost:${PORT}/health`);
            console.log(`  - JWKS:    http://localhost:${PORT}/.well-known/jwks.json`);
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
