# Authentication Flow Logging

## Übersicht

Die Mock IDP Authenticator-Implementierung enthält jetzt **detailliertes Logging**, um den Authentication Flow transparent zu machen.

## Log-Format

Alle Logs verwenden visuelle Marker für bessere Lesbarkeit:

- `========================================` - Flow-Grenzen
- `✓` - Erfolgreiche Operation
- `✗` - Fehlgeschlagene Operation
- `→` - Aktion wird ausgeführt
- `⚠️` - Warnung (unerwarteter Zustand)

## Komponenten mit Logging

### 1. Mock IDP Authenticator
**Log-Abschnitte:**
- `Mock IDP Authenticator - START` - Authenticator wird aufgerufen
- `Mock IDP Authenticator - END` - Authenticator abgeschlossen
- `Mock IDP Authenticator - ACTION` - User kehrt vom Mock IDP zurück

**Wichtige Checks:**
- Prüft, ob bereits ein User authentifiziert ist (sollte NICHT der Fall sein!)
- Zeigt Callback-Status
- Loggt Redirect-URL zum Mock IDP

### 2. Mock IDP Callback Endpoint
**Log-Abschnitte:**
- `Mock IDP Callback Endpoint - START` - Callback empfangen
- `Mock IDP Callback Endpoint - END` - Callback verarbeitet

**Wichtige Validierungen:**
- Transaction ID Validierung
- Callback Token Validierung
- JWT Validierung

### 3. Complete Authentication
**Detaillierte Schritte:**
- Callback-Success Prüfung
- Transaction ID Validierung
- JWT Claims Parsing
- User Creation/Update
- Authentication Success

## Live-Monitoring

### Logs in Echtzeit anzeigen
```bash
./scripts/logs.sh
```

### Nur Authentication Flow Logs
```bash
./scripts/logs.sh | grep -E "(Mock IDP|========)"
```

### Nur Fehler anzeigen
```bash
./scripts/logs.sh | grep "✗"
```

### Nur erfolgreiche Operationen
```bash
./scripts/logs.sh | grep "✓"
```

## Test-Szenarien

### Szenario 1: Erster Login (kein SSO Cookie)

**Erwartete Logs:**
```
========================================
Mock IDP Authenticator - START
Session ID: abc123...
Client: web-test-client
✓ No authenticated user yet - proceeding with Mock IDP authentication
→ No callback yet - redirecting to Mock IDP
→ Redirecting to Mock IDP: http://localhost/mock-idp
Mock IDP Authenticator - END (redirecting)
========================================

[User wählt Person im Mock IDP]

========================================
Mock IDP Callback Endpoint - START
→ Processing callback - transaction_id: xyz789...
✓ Authentication session found
✓ Transaction ID validated
✓ Callback token validated
✓ JWT validated successfully - sub: person-001
✓ Mock IDP callback processed successfully - sub: person-001
Mock IDP Callback Endpoint - END (success)
========================================

========================================
Mock IDP Authenticator - ACTION (user returned from Mock IDP)
Session ID: abc123...
→ Completing authentication...
✓ Callback was successful
✓ Transaction ID validated: xyz789...
✓ JWT claims found
✓ JWT claims parsed - subject: person-001
✓ User created/updated: mock-idp-person-001
✓ Mock IDP authentication successful for user: mock-idp-person-001
Mock IDP Authenticator - ACTION END
========================================
```

### Szenario 2: Zweiter Login (SSO Cookie vorhanden)

**Erwartete Logs:**
```
[Keine Mock IDP Logs!]
```

**Warum?** Der Cookie-Authenticator erkennt den SSO Cookie und gibt `context.success()` zurück. Der Mock IDP Authenticator wird **gar nicht** aufgerufen.

**Wenn doch Mock IDP Logs erscheinen:**
```
========================================
Mock IDP Authenticator - START
Session ID: def456...
Client: web-test-client
⚠️  User already authenticated: user-id (username: mock-idp-person-001)
⚠️  This should NOT happen if cookie authenticator succeeded!
⚠️  Cookie authenticator likely returned ATTEMPTED instead of SUCCESS
```

Dies deutet auf ein Problem mit dem Cookie-Authenticator hin!

## Debugging

### Problem: Mock IDP wird immer aufgerufen, auch mit SSO Cookie

**Prüfen:**
1. **Cookie vorhanden?**
   ```bash
   # Browser DevTools → Application → Cookies
   # Suche nach: KEYCLOAK_SESSION
   ```

2. **Authentication Flow korrekt?**
   ```bash
   curl -s -H "Authorization: Bearer $(curl -s -X POST http://localhost:8081/realms/master/protocol/openid-connect/token -d 'client_id=admin-cli' -d 'username=admin' -d 'password=admin' -d 'grant_type=password' | jq -r '.access_token')" \
   'http://localhost:8081/admin/realms/test-realm/authentication/flows/mock-idp-browser/executions' | jq '.'
   ```
   
   Erwartete Reihenfolge:
   - Index 0: `auth-cookie` (ALTERNATIVE)
   - Index 1: `mock-idp-authenticator` (ALTERNATIVE)

3. **User Session aktiv?**
   ```bash
   # Admin Console → Realm: test-realm → Sessions
   # Prüfe User Sessions und Client Sessions
   ```

### Problem: "⚠️ User already authenticated" Warnung

**Bedeutung:** Der Mock IDP Authenticator wird aufgerufen, obwohl bereits ein User authentifiziert ist.

**Mögliche Ursachen:**
1. Cookie-Authenticator gibt `context.attempted()` statt `context.success()` zurück
2. SSO Cookie ist ungültig/abgelaufen
3. Authentication Flow ist falsch konfiguriert

**Lösung:**
- Prüfe Cookie-Gültigkeit
- Prüfe Flow-Konfiguration  
- Lösche alle Sessions und teste erneut

## Zusammenfassung

Mit diesem Logging kannst du genau nachvollziehen:

1. **Wird der Cookie-Auth übersprungen?** → Kein Mock IDP Log = Cookie erfolgreich
2. **Wird Mock IDP aufgerufen?** → Siehst du START/END Logs
3. **Gibt es Fehler?** → Siehst du `✗` Marker
4. **Ist ein User schon authentifiziert?** → Siehst du `⚠️` Warnungen

**Best Practice:** Behalte `./scripts/logs.sh` in einem separaten Terminal offen während der Entwicklung!
