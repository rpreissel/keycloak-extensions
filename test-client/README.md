# Keycloak Test Client

Einfacher Web-Client zum Testen der Keycloak OAuth 2.0 / OpenID Connect Integration.

## Features

- 🔐 Login über Keycloak
- 📋 Anzeige von Access Token und ID Token (als JSON)
- 🔄 Token Refresh
- 👋 Logout

## Deployment

Der Test-Client läuft als **Container** via Podman Compose und ist über den **Nginx Reverse Proxy** erreichbar.

### URL-Struktur

Alle Services sind über `http://localhost` erreichbar:

- **Test-Client**: `http://localhost/app`
- **Keycloak Admin**: `http://localhost/auth`
- **Keycloak Realms**: `http://localhost/realms/...`
- **Mock IDP**: `http://localhost/mock-idp`

### Container-Architektur

```
┌─────────────────┐
│  Nginx (Port 80)│  ← Reverse Proxy
└────────┬────────┘
         │
    ┌────┴─────┬──────────┬────────────┐
    │          │          │            │
┌───▼───┐  ┌──▼───┐  ┌───▼────┐  ┌────▼────┐
│Test   │  │Keycloak│  │Mock IDP│  │PostgreSQL
│Client │  │        │  │        │  │         │
│:3002  │  │:8080   │  │:3001   │  │:5432    │
└───────┘  └────────┘  └────────┘  └─────────┘
```

## Verwendung

### 1. Alle Services starten
```bash
cd ..
./scripts/setup-dev-env.sh
```

### 2. Browser öffnen
```
http://localhost/app
```

### 3. Login
- Username: `testuser`
- Password: `test123`

## Lokale Entwicklung

Für lokale Entwicklung **außerhalb** von Docker:

```bash
npm install
npm run dev
```

Dann ist der Client auf `http://localhost:3002` erreichbar (direkter Zugriff, ohne Nginx).

**Hinweis**: Bei lokaler Entwicklung musst du die Umgebungsvariablen anpassen:

```bash
export KEYCLOAK_URL=http://localhost:8081
export REDIRECT_URI=http://localhost:3002/
npm run dev
```

## Konfiguration

Umgebungsvariablen (werden durch `podman-compose.yml` gesetzt):

```bash
NODE_ENV=production
PORT=3002
KEYCLOAK_URL=http://localhost
KEYCLOAK_REALM=test-realm
KEYCLOAK_CLIENT_ID=web-test-client
KEYCLOAK_CLIENT_SECRET=web-test-client-secret-change-in-production
REDIRECT_URI=http://localhost/app/
```

## Architektur

- **Frontend**: Vanilla JavaScript Single Page Application
- **Backend**: Express.js Server für OAuth-Flow
- **Session**: Server-side session storage für Tokens
- **Container**: Node.js 20 Alpine
- **Reverse Proxy**: Nginx (SSL-Terminierung, Routing, Rate-Limiting)

## API Endpoints (intern)

- `GET /` - Frontend Application
- `GET /login` - Initiiert OAuth-Flow
- `GET /callback` - OAuth Callback (Code → Token Exchange)
- `GET /session` - Prüft aktuelle Session
- `POST /refresh` - Erneuert Access Token
- `POST /logout` - Logout (Session + Keycloak)
- `GET /health` - Health Check

## Token-Anzeige

Die Anwendung zeigt folgende Informationen:

1. **Benutzer-Info**: Username, Email, Name, Subject
2. **Access Token**: Vollständige JWT Claims
3. **ID Token**: Vollständige JWT Claims
4. **Refresh Token**: Auszug (aus Sicherheitsgründen gekürzt)

## Nginx Reverse Proxy

Der Nginx Proxy bietet:

- ✅ Zentraler Einstiegspunkt (Port 80)
- ✅ URL-basiertes Routing (`/app`, `/auth`, `/realms`)
- ✅ Rate Limiting (Schutz vor Brute-Force)
- ✅ Security Headers
- ✅ Health Check Endpoint
- ✅ Caching für statische Ressourcen

### Routing-Regeln

```nginx
/app        → test-client:3002
/auth       → keycloak:8080/auth
/realms     → keycloak:8080/realms
/resources  → keycloak:8080/resources (cached)
/mock-idp   → mock-idp-backend:3001
```
