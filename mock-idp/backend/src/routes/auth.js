const express = require('express');
const router = express.Router();
const { validateVerifyParams } = require('../middleware/validator');
const clientService = require('../services/client.service');
const testPersons = require('../config/testPersons.json');

/**
 * GET /verify
 * Entry point - Display person selection UI
 * Query params: client_id, transaction_id, callback_token, state, redirect_uri
 */
router.get('/verify', validateVerifyParams, (req, res) => {
    const { client_id, transaction_id, callback_token, state, redirect_uri, callback_url } = req.query;

    // Validate client exists
    const client = clientService.getClient(client_id);
    if (!client) {
        return res.status(400).json({
            error: 'invalid_client',
            message: `Unknown client: ${client_id}`
        });
    }

    // Validate redirect URI
    if (!clientService.isValidRedirectUri(client_id, redirect_uri)) {
        return res.status(400).json({
            error: 'invalid_redirect_uri',
            message: `Redirect URI not allowed for client ${client_id}`
        });
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
            <p><strong>Transaction ID:</strong> ${transaction_id}</p>
            <p><strong>Client:</strong> ${client_id}</p>
        </div>

        <div class="persons-grid" id="personsGrid"></div>
        
        <div class="loading" id="loading">
            <p>Processing verification...</p>
        </div>

        <div class="error" id="error"></div>
    </div>

    <script>
        const persons = ${JSON.stringify(testPersons)};
        const data = {
            clientId: ${JSON.stringify(client_id)},
            transactionId: ${JSON.stringify(transaction_id)},
            callbackToken: ${JSON.stringify(callback_token)},
            redirectUri: ${JSON.stringify(redirect_uri)},
            callbackUrl: ${JSON.stringify(callback_url)},
            state: ${JSON.stringify(state)}
        };

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
                const response = await fetch('/select', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        personId,
                        transactionId: data.transactionId,
                        callbackToken: data.callbackToken,
                        clientId: data.clientId,
                        redirectUri: data.redirectUri,
                        callbackUrl: data.callbackUrl
                    })
                });

                const result = await response.json();

                if (!response.ok) {
                    throw new Error(result.message || 'Selection failed');
                }

                // Redirect to Keycloak
                console.log('Redirecting to:', result.redirect_uri);
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

module.exports = router;
