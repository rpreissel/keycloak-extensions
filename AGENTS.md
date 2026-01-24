# Keycloak Extension Development

## Projektübersicht

Dieses Projekt dient der Entwicklung von Keycloak Extensions (SPIs). Der komplette Workflow umfasst:

- **Extension Code** schreiben (Java)
- **Build** mit Maven
- **Deployment** in lokalen Keycloak
- **Debugging** mit Remote Debug (VS Code/IntelliJ)
- **Konfiguration** via Admin CLI Scripts
- **Testing** mit OAuth2/OIDC Testclients

### Tech-Stack

- **Keycloak**: 24.0.5 (Quarkus-basiert)
- **Build-Tool**: Maven (Multi-Module)
- **Java**: 17+ (LTS)
- **Container**: Podman (macOS-kompatibel)
- **Datenbank**: PostgreSQL 15 oder H2 (Dev-Mode)
- **IDE**: VS Code (primär), IntelliJ IDEA Ultimate (sekundär)

---

## Voraussetzungen

Stelle sicher, dass folgende Tools installiert sind:

```bash
# Podman (Container-Runtime)
brew install podman

# Java Development Kit (JDK 17+)
brew install openjdk@17

# Maven (Build-Tool)
brew install maven

# Optional: Admin CLI Tools (httpie für REST API Tests)
brew install httpie
```

**Versionen prüfen:**

```bash
podman --version          # >= 4.0
java -version             # >= 17
mvn -version              # >= 3.8
```

---

## Entwicklungsumgebung Setup

### Podman Machine initialisieren (einmalig)

```bash
# Podman Machine erstellen und starten
podman machine init
podman machine start

# Podman Machine Status prüfen
podman machine list
```

### Keycloak Container starten (Dev-Mode mit Debug)

**Einfaches Setup mit H2 (In-Memory):**

```bash
podman run -d --name keycloak-dev \
  -p 8080:8080 \
  -p 5005:5005 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -e DEBUG=true \
  -e DEBUG_PORT='*:5005' \
  -v $(pwd)/providers:/opt/keycloak/providers:z \
  quay.io/keycloak/keycloak:24.0.5 \
  start-dev
```

**Setup mit PostgreSQL (via podman-compose.yml):**

Siehe `podman-compose.yml` im Projekt-Root für vollständiges Setup mit:
- Keycloak Container
- PostgreSQL Container
- Persistente Volumes
- Netzwerk-Konfiguration

**Wichtige Ports:**
- `8080`: Keycloak HTTP (Admin Console + API)
- `5005`: Remote Debug Port (JDWP)

**Admin Console Zugriff:**

```
URL:      http://localhost:8080
Username: admin
Password: admin
```

---

## Projekt-Struktur

```
keycloak/
├── pom.xml                          # Root POM (Multi-Module)
├── podman-compose.yml               # Container Setup
├── AGENTS.md                        # Diese Datei
│
├── example-authenticator/           # Extension-Modul (Beispiel)
│   ├── pom.xml                      # Modul-POM
│   └── src/main/
│       ├── java/                    # Provider-Implementierungen
│       └── resources/META-INF/
│           ├── services/            # SPI-Registrierung
│           └── beans.xml            # CDI-Aktivierung (leer)
│
├── providers/                       # Deployed JARs (git-ignored)
│
├── .vscode/
│   └── launch.json                  # Debug-Konfiguration
│
├── scripts/                         # Automatisierungs-Scripts
│   ├── start-keycloak.sh
│   ├── build-deploy.sh
│   ├── setup-realm.sh
│   └── create-test-client.sh
│
└── config/                          # Konfigurationsdateien
    └── keycloak.conf
```

---

## Extension Entwicklung

### Grundkonzepte

Keycloak Extensions basieren auf dem **Service Provider Interface (SPI)** Pattern:

1. **SPI**: Definiert den Erweiterungspunkt (z.B. `AuthenticatorSPI`)
2. **Provider**: Implementiert die Business-Logik (z.B. `MyAuthenticator`)
3. **ProviderFactory**: Erstellt Provider-Instanzen (z.B. `MyAuthenticatorFactory`)

### Maven Dependencies

**Wichtig**: Alle Keycloak-Dependencies mit Scope `provided` (bereits im Container vorhanden):

```xml
<dependencies>
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-core</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-server-spi</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-server-spi-private</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### SPI-Registrierung

Extensions werden via **Java Service Loader** registriert:

**Datei**: `src/main/resources/META-INF/services/org.keycloak.authentication.AuthenticatorFactory`

```
com.example.keycloak.MyAuthenticatorFactory
```

**Wichtig**: 
- Dateiname = Fully Qualified Interface Name
- Inhalt = Fully Qualified Factory Class Name(s)

### CDI-Aktivierung

**Datei**: `src/main/resources/META-INF/beans.xml`

```xml
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee 
                           https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd"
       version="3.0" bean-discovery-mode="all">
</beans>
```

---

## Build & Deployment Workflow

### 1. Extension bauen

```bash
# Von Root-Verzeichnis (alle Module)
mvn clean package

# Oder einzelnes Modul
cd example-authenticator
mvn clean package
```

**Output**: `target/example-authenticator-1.0.0-SNAPSHOT.jar`

### 2. Extension deployen

**Option A: Volume-Mount (automatisch)**

Wenn Container mit Volume gestartet wurde:

```bash
# JAR wird automatisch erkannt
mvn clean package

# Keycloak Rebuild triggern
podman exec keycloak-dev /opt/keycloak/bin/kc.sh build
```

**Option B: Manuelles Kopieren**

```bash
# JAR in Container kopieren
podman cp target/example-authenticator-1.0.0-SNAPSHOT.jar \
  keycloak-dev:/opt/keycloak/providers/

# Rebuild triggern
podman exec keycloak-dev /opt/keycloak/bin/kc.sh build

# Container neu starten (für Produktions-Modus)
podman restart keycloak-dev
```

### 3. Deployment verifizieren

```bash
# Logs prüfen
podman logs -f keycloak-dev

# Nach folgender Meldung suchen:
# "Deployed extension: com.example.keycloak.MyAuthenticatorFactory"
```

### Typischer Workflow

```bash
# 1. Code ändern
# 2. Build
mvn clean package

# 3. Deploy (automatisch via Volume oder manuell kopieren)
podman exec keycloak-dev /opt/keycloak/bin/kc.sh build

# 4. Testen
# 5. Wiederholen
```

---

## Debugging

### Remote Debug Setup

Keycloak lauscht auf Port `5005` (JDWP Protocol).

### VS Code Konfiguration

**Datei**: `.vscode/launch.json`

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Keycloak",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

**Debugging starten:**

1. Stelle sicher, dass Keycloak läuft (`podman ps`)
2. VS Code: `F5` oder Run → Start Debugging
3. Breakpoints setzen
4. Extension-Code triggern (z.B. via Admin Console oder Test-Login)

### IntelliJ IDEA Konfiguration

**Run → Edit Configurations → Add New → Remote JVM Debug**

- **Name**: Keycloak Debug
- **Host**: localhost
- **Port**: 5005
- **Use module classpath**: (dein Extension-Modul)

**Debugging starten:**

1. Breakpoints setzen
2. Run → Debug 'Keycloak Debug'
3. Extension-Code triggern

### Debugging-Tipps

- **Breakpoints in Factory**: `createXxx()` Methoden werden beim Provider-Load aufgerufen
- **Breakpoints in Provider**: Business-Logik (z.B. `authenticate()`)
- **Logs parallel beobachten**: `podman logs -f keycloak-dev`
- **Hot Code Replace**: Funktioniert bei kleineren Änderungen (Java 17+ HotSwap)

---

## Realm & Client Konfiguration

### Admin CLI Setup (`kcadm.sh`)

Der Admin CLI ist der empfohlene Weg, um reproduzierbare Konfigurationen zu erstellen.

**1. Admin CLI in Container ausführen:**

```bash
# Alias für einfachere Verwendung
alias kcadm='podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh'

# Login als Admin
kcadm config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin
```

**2. Realm erstellen:**

```bash
# Neuen Realm erstellen
kcadm create realms \
  -s realm=test-realm \
  -s enabled=true \
  -s displayName="Test Realm"
```

**3. Client registrieren (OAuth2/OIDC):**

```bash
# Public Client (für SPAs, Native Apps)
kcadm create clients -r test-realm \
  -s clientId=test-client \
  -s enabled=true \
  -s publicClient=true \
  -s 'redirectUris=["http://localhost:3000/*"]' \
  -s 'webOrigins=["http://localhost:3000"]' \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=true
```

```bash
# Confidential Client (für Backend-Services)
kcadm create clients -r test-realm \
  -s clientId=backend-service \
  -s enabled=true \
  -s publicClient=false \
  -s 'redirectUris=["http://localhost:8081/*"]' \
  -s serviceAccountsEnabled=true \
  -s authorizationServicesEnabled=true

# Client Secret abrufen
kcadm get clients -r test-realm --fields id,clientId | grep backend-service
kcadm get clients/<CLIENT-ID>/client-secret -r test-realm
```

**4. User anlegen:**

```bash
# User erstellen
kcadm create users -r test-realm \
  -s username=testuser \
  -s enabled=true \
  -s email=testuser@example.com \
  -s firstName=Test \
  -s lastName=User

# Password setzen
kcadm set-password -r test-realm \
  --username testuser \
  --new-password test123
```

**5. Rollen zuweisen:**

```bash
# Realm-Role erstellen
kcadm create roles -r test-realm \
  -s name=user-role \
  -s description="Standard User Role"

# Role zu User zuweisen
kcadm add-roles -r test-realm \
  --uusername testuser \
  --rolename user-role
```

### Scripts für wiederkehrende Setups

Erstelle Shell-Scripts in `scripts/` für häufige Konfigurationen:

**Beispiel**: `scripts/setup-test-realm.sh`

```bash
#!/bin/bash
set -e

REALM="test-realm"
CLIENT="test-client"

echo "Creating realm: $REALM"
kcadm create realms -s realm=$REALM -s enabled=true

echo "Creating client: $CLIENT"
kcadm create clients -r $REALM \
  -s clientId=$CLIENT \
  -s enabled=true \
  -s publicClient=true \
  -s 'redirectUris=["http://localhost:3000/*"]'

echo "Creating test user"
kcadm create users -r $REALM \
  -s username=testuser \
  -s enabled=true

kcadm set-password -r $REALM \
  --username testuser \
  --new-password test123

echo "Setup complete!"
```

**Ausführen:**

```bash
chmod +x scripts/setup-test-realm.sh
./scripts/setup-test-realm.sh
```

---

## Testing

### OAuth2/OIDC Flow testen

**1. Authorization Code Flow (Browser-basiert):**

```bash
# 1. Authorization URL öffnen
open "http://localhost:8080/realms/test-realm/protocol/openid-connect/auth?\
client_id=test-client&\
redirect_uri=http://localhost:3000/callback&\
response_type=code&\
scope=openid"

# 2. Nach Login: Code aus Redirect URI extrahieren
# 3. Token austauschen
http POST http://localhost:8080/realms/test-realm/protocol/openid-connect/token \
  grant_type=authorization_code \
  client_id=test-client \
  code=<CODE> \
  redirect_uri=http://localhost:3000/callback
```

**2. Resource Owner Password Credentials (Direct Grant):**

```bash
# Direkt Token holen (für Testing)
http --form POST http://localhost:8080/realms/test-realm/protocol/openid-connect/token \
  grant_type=password \
  client_id=test-client \
  username=testuser \
  password=test123 \
  scope=openid
```

**3. Token validieren:**

```bash
# JWT Token dekodieren (jwt.io oder cli tool)
# Introspection Endpoint
http --form POST http://localhost:8080/realms/test-realm/protocol/openid-connect/token/introspect \
  token=<ACCESS_TOKEN> \
  client_id=test-client
```

### Extension-Verhalten testen

- **Custom Authenticator**: Login-Flow triggern und Breakpoints setzen
- **Event Listener**: Event triggern (Login, Logout, etc.) und Logs prüfen
- **Protocol Mapper**: Token holen und Claims inspizieren

---

## Hilfreiche Befehle

### Container-Management

```bash
# Container starten
podman start keycloak-dev

# Container stoppen
podman stop keycloak-dev

# Container neu starten
podman restart keycloak-dev

# Logs anzeigen (follow)
podman logs -f keycloak-dev

# Container Status
podman ps -a

# In Container einsteigen (Shell)
podman exec -it keycloak-dev /bin/bash
```

### Quick Build & Deploy

```bash
# Build + Deploy in einem Schritt
mvn clean package && \
podman exec keycloak-dev /opt/keycloak/bin/kc.sh build

# Oder mit Script (wenn erstellt)
./scripts/build-deploy.sh
```

### Datenbank zurücksetzen

```bash
# Container stoppen und löschen
podman stop keycloak-dev
podman rm keycloak-dev

# Volume löschen (falls PostgreSQL verwendet)
podman volume rm keycloak_postgres_data

# Container neu starten (siehe Setup-Befehl oben)
```

### Keycloak Admin CLI

```bash
# Realms auflisten
kcadm get realms

# Clients in Realm auflisten
kcadm get clients -r test-realm --fields clientId,id

# Users auflisten
kcadm get users -r test-realm

# Realm-Konfiguration exportieren
kcadm get realms/test-realm > realm-export.json
```

---

## Troubleshooting

### Extension wird nicht geladen

**Problem**: Extension erscheint nicht in Admin Console oder wird nicht ausgeführt.

**Lösungen:**

1. **Deployment verifizieren:**
   ```bash
   podman exec keycloak-dev ls -la /opt/keycloak/providers/
   # JAR muss vorhanden sein
   ```

2. **Rebuild triggern:**
   ```bash
   podman exec keycloak-dev /opt/keycloak/bin/kc.sh build
   podman restart keycloak-dev
   ```

3. **Logs prüfen:**
   ```bash
   podman logs keycloak-dev | grep -i error
   podman logs keycloak-dev | grep -i "MyAuthenticatorFactory"
   ```

4. **Service-Datei prüfen:**
   - Pfad korrekt? `META-INF/services/org.keycloak.xxx.XxxFactory`
   - Fully Qualified Class Name korrekt?
   - Datei im JAR vorhanden? `jar tf target/my-extension.jar | grep META-INF`

5. **Dependencies prüfen:**
   - Scope `provided` für alle Keycloak-Dependencies
   - Keine Konflikte mit Container-Libraries

### Debug-Verbindung schlägt fehl

**Problem**: VS Code/IntelliJ kann nicht zu Port 5005 verbinden.

**Lösungen:**

1. **Port-Mapping prüfen:**
   ```bash
   podman port keycloak-dev
   # 5005/tcp -> 0.0.0.0:5005
   ```

2. **Debug-Port im Container prüfen:**
   ```bash
   podman exec keycloak-dev netstat -tlnp | grep 5005
   ```

3. **Container neu starten mit explizitem Debug-Flag:**
   ```bash
   podman stop keycloak-dev
   podman run -d --name keycloak-dev \
     -p 8080:8080 -p 5005:5005 \
     -e DEBUG=true \
     -e DEBUG_PORT='*:5005' \
     -e JAVA_OPTS_KC_HEAP="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
     -v $(pwd)/providers:/opt/keycloak/providers:z \
     quay.io/keycloak/keycloak:24.0.5 \
     start-dev
   ```

4. **Firewall/Netzwerk prüfen:**
   ```bash
   nc -zv localhost 5005
   # Connection succeeded
   ```

### Container startet nicht

**Problem**: Keycloak Container startet nicht oder crasht sofort.

**Lösungen:**

1. **Logs prüfen:**
   ```bash
   podman logs keycloak-dev
   ```

2. **Podman Machine Status:**
   ```bash
   podman machine list
   # STATE sollte "running" sein
   podman machine start
   ```

3. **Port-Konflikte:**
   ```bash
   lsof -i :8080
   lsof -i :5005
   # Andere Prozesse beenden oder andere Ports verwenden
   ```

4. **Volume-Mount Probleme (macOS):**
   ```bash
   # SELinux :z flag entfernen (nur bei Problemen)
   -v $(pwd)/providers:/opt/keycloak/providers
   ```

### Maven Build-Fehler

**Problem**: Compilation-Fehler oder Dependency-Probleme.

**Lösungen:**

1. **Keycloak BOM verwenden:**
   ```xml
   <dependencyManagement>
     <dependencies>
       <dependency>
         <groupId>org.keycloak</groupId>
         <artifactId>keycloak-parent</artifactId>
         <version>24.0.5</version>
         <type>pom</type>
         <scope>import</scope>
       </dependency>
     </dependencies>
   </dependencyManagement>
   ```

2. **Java Version prüfen:**
   ```bash
   java -version  # >= 17
   mvn -version   # sollte Java 17+ verwenden
   ```

3. **Clean Build:**
   ```bash
   mvn clean install -U
   ```

---

## Nächste Schritte

### Weitere Dateien im Projekt

- **`pom.xml`**: Root POM mit Multi-Module Setup
- **`podman-compose.yml`**: Vollständiges Container-Setup mit PostgreSQL
- **`.vscode/launch.json`**: Debug-Konfiguration für VS Code
- **`scripts/`**: Automatisierungs-Scripts für Build, Deploy, Setup
- **`.gitignore`**: Git-Ignore für Java/Maven/Keycloak Projekte
- **`README.md`**: Schnellstart-Guide

### Ressourcen

- **Keycloak Docs**: https://www.keycloak.org/docs/24.0.5/
- **Server Developer Guide**: https://www.keycloak.org/docs/24.0.5/server_development/
- **API JavaDocs**: https://www.keycloak.org/docs-api/24.0.5/javadocs/
- **GitHub Examples**: https://github.com/keycloak/keycloak/tree/24.0.5/examples

### Best Practices

1. **Immer `provided` Scope**: Keycloak-Dependencies nie bundlen
2. **Service-Registrierung**: Fully Qualified Names verwenden
3. **Logging**: SLF4J Logger verwenden (bereits in Keycloak)
4. **Testing**: Unit-Tests für Provider-Logik schreiben
5. **Versionierung**: Extension-Version in `pom.xml` pflegen
6. **Scripts**: Wiederkehrende Tasks automatisieren
7. **Git**: `providers/` und `target/` nicht committen

---

**Viel Erfolg bei der Keycloak Extension Entwicklung!**