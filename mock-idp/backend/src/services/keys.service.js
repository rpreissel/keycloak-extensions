const fs = require('fs');
const path = require('path');
const jose = require('node-jose');

class KeyService {
    constructor() {
        this.keyPath = process.env.KEY_PATH || path.join(__dirname, '../../keys');
        this.privateKeyFile = path.join(this.keyPath, 'private.pem');
        this.publicKeyFile = path.join(this.keyPath, 'public.pem');
        this.keyStore = null;
        this.publicKey = null;
        this.privateKey = null;
    }

    async initialize() {
        // Ensure keys directory exists
        if (!fs.existsSync(this.keyPath)) {
            fs.mkdirSync(this.keyPath, { recursive: true });
            console.log(`Created keys directory: ${this.keyPath}`);
        }

        // Check if keys exist
        if (this.keysExist()) {
            console.log('Loading existing RSA keys...');
            await this.loadKeys();
        } else {
            console.log('Generating new RSA key pair...');
            await this.generateKeys();
        }

        console.log('Key Service initialized successfully');
    }

    keysExist() {
        return fs.existsSync(this.privateKeyFile) && fs.existsSync(this.publicKeyFile);
    }

    async generateKeys() {
        try {
            // Generate RSA key pair (2048-bit)
            const keyStore = jose.JWK.createKeyStore();
            const key = await keyStore.generate('RSA', 2048, {
                alg: 'RS256',
                use: 'sig'
            });

            // Export keys
            const privateKeyPem = key.toPEM(true);  // true = include private key
            const publicKeyPem = key.toPEM(false);  // false = only public key

            // Save to files
            fs.writeFileSync(this.privateKeyFile, privateKeyPem);
            fs.writeFileSync(this.publicKeyFile, publicKeyPem);

            this.keyStore = keyStore;
            this.privateKey = key;
            this.publicKey = key;

            console.log('RSA key pair generated and saved');
        } catch (error) {
            console.error('Error generating keys:', error);
            throw error;
        }
    }

    async loadKeys() {
        try {
            const privateKeyPem = fs.readFileSync(this.privateKeyFile, 'utf8');
            const publicKeyPem = fs.readFileSync(this.publicKeyFile, 'utf8');

            const keyStore = jose.JWK.createKeyStore();
            const key = await keyStore.add(privateKeyPem, 'pem');

            this.keyStore = keyStore;
            this.privateKey = key;
            this.publicKey = key;

            console.log('RSA keys loaded successfully');
        } catch (error) {
            console.error('Error loading keys:', error);
            throw error;
        }
    }

    getPrivateKey() {
        if (!this.privateKey) {
            throw new Error('Keys not initialized. Call initialize() first.');
        }
        return this.privateKey;
    }

    getPublicKey() {
        if (!this.publicKey) {
            throw new Error('Keys not initialized. Call initialize() first.');
        }
        return this.publicKey;
    }

    async getJWKS() {
        if (!this.keyStore) {
            throw new Error('Key store not initialized. Call initialize() first.');
        }
        
        // Export public key as JWKS
        return this.keyStore.toJSON();
    }
}

// Singleton instance
const keyService = new KeyService();

module.exports = keyService;
