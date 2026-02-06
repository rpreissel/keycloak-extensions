# Mock Identity Provider

Ein simulierter Identity Proofing Service für Keycloak-Tests.

## Überblick

Dieser Mock Service simuliert einen externen Identity-Provider, der via Redirect aufgerufen wird, eine Testperson-Auswahl anzeigt, ein signiertes JWT generiert und an Keycloak zurücksendet.

## Architektur

```
┌──────────────┐         ┌─────────────────┐         ┌──────────────┐
│   Keycloak   │────1───▶│   Mock IDP      │────3───▶│   Keycloak   │
│ Authenticator│         │   /verify       │         │   Callback   │
└──────────────┘         └─────────────────┘         └──────────────┘
                                 │
                                 2
                                 ▼
                         ┌───────────────┐
                         │  User selects │
                         │    Person     │
                         └───────────────┘
```

### Flow

1. **Keycloak** sendet Redirect zu `/verify` mit Query Params:
   - `client_id`: Client-Identifikation
   - `transaction_id`: Transaktions-ID
   - `callback_token`: Token für Callback
   - `state`: State-Parameter
   - `redirect_uri`: Rücksprung-URL

2. **Mock IDP** zeigt UI mit Testpersonen-Auswahl

3. **User** wählt eine Person aus

4. **Mock IDP**:
   - Generiert signiertes JWT (RS256)
   - Sendet JWT + Transaction-ID + Token an Keycloak Callback
   - Redirect zurück zu `redirect_uri`

## Technologie-Stack

- **Backend**: Node.js + Express
- **JWT**: RS256 mit automatisch generierten RSA Keys
- **Frontend**: Embedded HTML/JavaScript (Single-Page)
- **Container**: Podman/Docker

## Schnellstart

### Starten

```bash
./scripts/start-mock-idp.sh
```

Service verfügbar unter: http://localhost:3001

### Stoppen

```bash
./scripts/stop-mock-idp.sh
```

### Manuell testen

```bash
# Health Check
curl http://localhost:3001/health

# JWKS anzeigen
curl http://localhost:3001/.well-known/jwks.json

# UI öffnen
open "http://localhost:3001/verify?client_id=test-client&transaction_id=tx-123&callback_token=tok-456&state=abc&redirect_uri=http://localhost:8081/callback"
```

## Konfiguration

### Clients (`mock-idp/backend/src/config/clients.json`)

```json
{
  "test-client": {
    "callback_url": "http://keycloak-dev:8080/realms/test-realm/broker/mock-idp/endpoint",
    "allowed_redirect_uris": [
      "http://localhost:8081/*",
      "http://keycloak-dev:8080/*"
    ]
  }
}
```

### Testpersonen (`mock-idp/backend/src/config/testPersons.json`)

```json
[
  {
    "id": "person-001",
    "firstName": "Max",
    "lastName": "Mustermann",
    "birthdate": "1990-01-15",
    "status": "verified"
  }
]
```

**Status-Werte:**
- `verified`: Erfolgreich verifiziert
- `rejected`: Abgelehnt
- `pending`: Ausstehend

## API Endpoints

### `GET /verify`

Entry-Point für Identity Verification.

**Query Parameters:**
- `client_id` (required): Client ID
- `transaction_id` (required): Transaction ID
- `callback_token` (required): Callback Token
- `state` (required): State Parameter
- `redirect_uri` (required): Redirect URI

**Response:** HTML-Seite mit Testpersonen-Auswahl

### `POST /select`

Person-Auswahl verarbeiten.

**Request Body:**
```json
{
  "personId": "person-001",
  "transactionId": "tx-123",
  "callbackToken": "tok-456",
  "clientId": "test-client",
  "redirectUri": "http://localhost:8081/callback"
}
```

**Response:**
```json
{
  "success": true,
  "redirect_uri": "http://localhost:8081/callback",
  "person": {
    "id": "person-001",
    "name": "Max Mustermann",
    "status": "verified"
  }
}
```

### `GET /health`

Health Check für Container-Orchestrierung.

**Response:**
```json
{
  "status": "ok",
  "service": "mock-identity-provider",
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

### `GET /.well-known/jwks.json`

Public Key Distribution (JWKS).

Keycloak kann diesen Endpoint nutzen, um JWTs zu verifizieren.

## JWT Format

**Claims:**
```json
{
  "sub": "person-001",
  "aud": "test-client",
  "iss": "http://mock-idp-backend:3001",
  "iat": 1234567890,
  "exp": 1234571490,
  "tid": "transaction-id",
  "name": "Max Mustermann",
  "given_name": "Max",
  "family_name": "Mustermann",
  "birthdate": "1990-01-15",
  "verification_status": "verified"
}
```

**Signierung:** RS256 mit automatisch generierten Keys (`mock-idp/backend/keys/`)

## Entwicklung

### Lokaler Start (ohne Container)

```bash
cd mock-idp/backend
npm install
npm start
```

### Logs anzeigen

```bash
podman logs -f mock-idp-backend
```

### Keys regenerieren

```bash
rm -rf mock-idp/backend/keys/*.pem
./scripts/restart-mock-idp.sh
```

Neue Keys werden automatisch beim Start generiert.

## Integration mit Keycloak

### 1. Custom Authenticator erstellen

Der Keycloak Authenticator muss:
- Transaction-ID und Callback-Token generieren
- Redirect zu Mock IDP durchführen
- Callback-Endpoint bereitstellen

### 2. Callback-Endpoint implementieren

Der Endpoint empfängt:
```json
{
  "jwt": "eyJhbGci...",
  "transaction_id": "tx-123",
  "callback_token": "tok-456"
}
```

Und muss:
- JWT mit JWKS verifizieren (`/.well-known/jwks.json`)
- Claims extrahieren
- User-Attribute setzen
- Authentication Flow fortsetzen

### 3. JWKS-Konfiguration

Keycloak kann den Public Key abrufen von:
```
http://mock-idp-backend:3001/.well-known/jwks.json
```

## Troubleshooting

### Container startet nicht

```bash
podman logs mock-idp-backend
```

### Health Check fehlgeschlagen

```bash
curl http://localhost:3001/health
```

### Keys fehlen

Keys werden automatisch generiert. Bei Problemen:
```bash
podman exec -it mock-idp-backend ls -la /app/keys/
```

### Callback zu Keycloak schlägt fehl

Normal in Entwicklung, wenn Keycloak noch nicht konfiguriert ist.
Logs zeigen: `Continuing despite callback failure (development mode)`

## Sicherheitshinweise

⚠️ **Nur für Testing/Development!**

- Keine echte User-Authentifizierung
- Callback-Token wird nicht validiert
- Keine Session-Timeouts
- Einfache In-Memory-Konfiguration

Für Production benötigen Sie:
- Echte Token-Validierung
- Persistente Storage
- Rate Limiting
- HTTPS/TLS
- Audit Logging

## Dateien & Verzeichnisse

```
mock-idp/
├── backend/
│   ├── src/
│   │   ├── server.js              # Express Server
│   │   ├── routes/
│   │   │   ├── auth.js            # /verify Route
│   │   │   ├── callback.js        # /select Route
│   │   │   └── utility.js         # /health, /jwks
│   │   ├── services/
│   │   │   ├── keys.service.js    # RSA Key Management
│   │   │   ├── jwt.service.js     # JWT Generierung
│   │   │   └── client.service.js  # Client-Konfiguration
│   │   ├── config/
│   │   │   ├── clients.json       # Client-Mappings
│   │   │   └── testPersons.json   # Testpersonen
│   │   └── middleware/
│   │       └── validator.js       # Request Validation
│   ├── keys/                      # RSA Keys (auto-generiert)
│   ├── Dockerfile
│   ├── package.json
│   └── .env
└── (frontend/)                    # Embedded in /verify HTML
```

## Support

Bei Fragen oder Problemen:
1. Logs prüfen: `podman logs mock-idp-backend`
2. Health Check: `curl http://localhost:3001/health`
3. Container Status: `podman ps -a | grep mock-idp`
