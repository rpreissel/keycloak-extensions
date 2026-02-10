# 🚀 Container-basiertes Setup - Korrigierte Konfiguration

## ✅ Was wurde korrigiert:

### Problem
Der Browser konnte nicht auf `localhost:3001` (Mock IDP) und `localhost:3002` (Test Client) zugreifen, da diese Ports nur Container-intern verfügbar waren.

### Lösung
Alle Services laufen jetzt über **Nginx Reverse Proxy** auf **Port 80**:

```
http://localhost/
├── /app          → Test Client
├── /admin        → Keycloak Admin Console
├── /realms       → Keycloak OIDC Endpoints
├── /mock-idp     → Mock IDP Service
└── /health       → Health Check
```

## 🎯 Aktualisierte URLs

### Für Browser-Zugriff:
- **Test Client**: http://localhost/app
- **Keycloak Admin**: http://localhost/admin
- **Mock IDP**: http://localhost/mock-idp

### Direkt (nur für Entwicklung):
- **Keycloak**: http://localhost:8081
- **PostgreSQL**: localhost:5432
- **Debug**: localhost:5005

## 🔧 Vorgenommene Änderungen

### 1. Nginx Konfiguration (`nginx/nginx.conf`)
- Korrekte Pfad-Rewrites für `/app/` und `/mock-idp/`
- `/admin` statt `/auth` (Keycloak 26.x)
- Health Checks für alle Services

### 2. Podman Compose (`podman-compose.yml`)
- Mock IDP verwendet nun `http://localhost/mock-idp` als ISSUER_URL
- Test Client verwendet `http://localhost` als KEYCLOAK_URL
- Alle Container im gleichen Netzwerk

### 3. OpenTofu (`terraform/main.tf`)
- Redirect URIs angepasst: `http://localhost/app/*`
- Web Origins angepasst: `http://localhost`

## 📋 Testen

1. **Status prüfen:**
   ```bash
   ./scripts/check-all.sh
   ```

2. **Test Client öffnen:**
   - Öffne: http://localhost/app
   - Klicke "Mit Keycloak anmelden"
   - Login: `testuser` / `test123`
   - Siehe Access & ID Tokens als JSON

3. **Admin Console:**
   - Öffne: http://localhost/admin
   - Login: `admin` / `admin`

## 🐛 Debugging

### Nginx Logs:
```bash
podman logs reverse-proxy
```

### Test Client Logs:
```bash
podman logs test-client
```

### Keycloak Logs:
```bash
./scripts/logs.sh
```

### Container Status:
```bash
podman ps
```

## ✨ Features

- ✅ Alle Services über einen Port (80)
- ✅ Rate Limiting (Schutz vor Brute-Force)
- ✅ Security Headers
- ✅ Health Checks
- ✅ Automatisches Routing
- ✅ CORS-Konfiguration
- ✅ Container-Isolation

## 🔄 Neustart bei Änderungen

### Nginx-Konfiguration geändert:
```bash
cd /path/to/keycloak
podman-compose build nginx
podman stop reverse-proxy && podman rm reverse-proxy
podman-compose up -d nginx
```

### Alle Container neu starten:
```bash
./scripts/stop-keycloak.sh
./scripts/start-keycloak.sh
./scripts/tf-apply.sh
```
