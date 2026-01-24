# Keycloak Extension Development

Entwicklungsumgebung für Keycloak Extensions (SPIs) mit Podman, Maven, und Remote Debugging.

## Schnellstart

### 1. Voraussetzungen installieren

```bash
# Podman (Container-Runtime)
brew install podman

# Java 17+ und Maven
brew install openjdk@17 maven

# Optional: httpie für API-Tests
brew install httpie
```

### 2. Podman Machine initialisieren

```bash
podman machine init
podman machine start
```

### 3. Keycloak mit PostgreSQL starten

**Option A: Mit podman-compose (empfohlen)**

```bash
podman-compose up -d
```

**Option B: Einfacher Container (nur Keycloak mit H2)**

```bash
podman run -d --name keycloak-dev \
  -p 8080:8080 -p 5005:5005 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -e DEBUG=true \
  -e DEBUG_PORT='*:5005' \
  -v $(pwd)/providers:/opt/keycloak/providers:z \
  quay.io/keycloak/keycloak:24.0.5 \
  start-dev
```

### 4. Keycloak Admin Console öffnen

```bash
open http://localhost:8080
```

**Login**: `admin` / `admin`

### 5. Extension entwickeln

Siehe **[AGENTS.md](AGENTS.md)** für vollständige Dokumentation zu:

- Extension-Entwicklung (SPIs, Providers, Factories)
- Build & Deployment Workflow
- Debugging Setup (VS Code, IntelliJ)
- Realm & Client-Konfiguration via Admin CLI
- Testing mit OAuth2/OIDC
- Troubleshooting

## Projekt-Struktur

```
keycloak/
├── AGENTS.md                 # Vollständige Entwicklungs-Dokumentation
├── README.md                 # Diese Datei (Schnellstart)
├── pom.xml                   # Root POM (Multi-Module)
├── podman-compose.yml        # Keycloak + PostgreSQL Setup
│
├── providers/                # Deployed Extensions (git-ignored)
├── scripts/                  # Automatisierungs-Scripts
└── .vscode/                  # VS Code Debug-Konfiguration
```

## Häufige Befehle

### Container-Management

```bash
# Container starten
podman-compose up -d

# Container stoppen
podman-compose down

# Logs anzeigen
podman logs -f keycloak-dev

# Container Status
podman ps
```

### Extension bauen & deployen

```bash
# Build
mvn clean package

# Deploy (Rebuild triggern)
podman exec keycloak-dev /opt/keycloak/bin/kc.sh build

# Container neu starten
podman restart keycloak-dev
```

### Debugging

```bash
# VS Code: F5 drücken
# oder
# IntelliJ: Run → Debug 'Keycloak Debug'
```

Debug-Port: `localhost:5005`

## Admin CLI verwenden

```bash
# Alias setzen
alias kcadm='podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh'

# Login
kcadm config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Realm erstellen
kcadm create realms -s realm=test-realm -s enabled=true
```

Siehe [AGENTS.md - Realm & Client Konfiguration](AGENTS.md#realm--client-konfiguration) für weitere Beispiele.

## Nächste Schritte

1. **[AGENTS.md lesen](AGENTS.md)** - Vollständige Entwicklungs-Dokumentation
2. **Extension erstellen** - Beispiel in AGENTS.md
3. **Admin CLI Scripts** - Siehe `scripts/` Verzeichnis

## Ressourcen

- **Keycloak Dokumentation**: https://www.keycloak.org/docs/24.0.5/
- **Server Developer Guide**: https://www.keycloak.org/docs/24.0.5/server_development/
- **API JavaDocs**: https://www.keycloak.org/docs-api/24.0.5/javadocs/

## Tech-Stack

- **Keycloak**: 24.0.5 (Quarkus-basiert)
- **Build-Tool**: Maven 3.8+
- **Java**: 17+ (LTS)
- **Container**: Podman 4.0+
- **Datenbank**: PostgreSQL 15 oder H2 (Dev-Mode)
- **IDE**: VS Code / IntelliJ IDEA Ultimate

## Lizenz

Dieses Projekt ist für Entwicklungszwecke.
