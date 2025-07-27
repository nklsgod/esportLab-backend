# task.md – Backend Esports Planner (Spring Boot)

> **Arbeitsanweisung für den Claude-Code-Agenten**
>
> * **Immer** nach jedem Auftrag in dieser Datei den Fortschritt markieren.
> * Hake erledigte Punkte mit `[x]` ab und füge dahinter **Datum & Uhrzeit (Europe/Berlin)** ein, z. B. `— erledigt am 2025-07-27 14:35`.
> * Wenn teilweise erledigt: `[~]` verwenden und kurz notieren, was fehlt.
> * Wichtige Entscheidungen dokumentieren ("Decision:"), offene Punkte als "TODO:" eintragen.
> * Verlinke geänderte Dateien/Commits (Kurz-Hash) in einer **Änderungschronik** am Ende.
> * Erstelle bei Bedarf neue Unteraufgaben direkt unter dem entsprechenden Abschnitt.

---

## 0) Projekt-Basis

* [x] **Projekt anlegen** – Spring Boot 3, Java 21, Gradle/Maven. — erledigt am 2025-07-27 15:00
* [x] **Dependencies** – Web, Security, OAuth2 Client, Data JPA, Validation, Actuator, Flyway, PostgreSQL; optional Quartz, Lombok, springdoc-openapi. — erledigt am 2025-07-27 15:00
* [x] **Konfiguration** – `application.yml` (Ports, DB, CORS, Security, OAuth, Cloudinary), Profile `dev`/`prod`. — erledigt am 2025-07-27 15:00
* [x] **Docker** – `docker-compose.yml` mit PostgreSQL. Healthcheck. — erledigt am 2025-07-27 15:00
* [x] **.env.example** – alle benötigten Variablen eintragen. — erledigt am 2025-07-27 15:00
* [x] **Startprüfung** – App startet, `/actuator/health` ist UP, Flyway läuft. — erledigt am 2025-07-27 15:01

**Prompt-Vorschlag**

```
Erzeuge ein neues Spring‑Boot‑3 (Java 21) Projekt mit Web, Security, OAuth2 Client, Data JPA, Validation, Actuator, Flyway, PostgreSQL, optional Quartz und springdoc.
Lege application.yml mit Platzhaltern für DB, Discord OAuth (authorization-uri, token-uri, user-info-uri), Cloudinary und CORS an. Erzeuge docker-compose.yml (PostgreSQL + Healthcheck) und .env.example.
Starte die App lokal, prüfe /actuator/health und logge das Ergebnis.
```

---

## 1) Datenmodell & Migrationen

**Ziel:** UTC-basierte Speicherung; klare Indizes; Grundlage für Planner & Jobs.

* [ ] **Flyway V1\_\_init.sql** erstellen mit Tabellen:

    * `team(id, name, discord_guild_id, reminder_channel_id, tz, min_players, min_duration_minutes, reminder_hours TEXT, created_at)`
    * `member(id, team_id FK, discord_user_id UNIQUE, display_name, avatar_url, tz, roles, created_at)`
    * `availability(id, member_id FK, starts_at_utc timestamptz, ends_at_utc timestamptz, available boolean, note, created_at)`
    * `training_session(id, team_id FK, starts_at_utc timestamptz, ends_at_utc timestamptz, source VARCHAR(16) CHECK (source IN ('AUTO','MANUAL')), title, created_by_member_id FK, created_at)`
    * `job_lock(key varchar primary key, until timestamptz)`
* [ ] **Constraints** – `starts_at_utc < ends_at_utc`.
* [ ] **Indizes** – `availability(member_id, starts_at_utc)`, `training_session(team_id, starts_at_utc)`.
* [ ] **JPA-Entities & Repositories** anlegen.
* [ ] **Testcontainers**-Test: Migration läuft, Grund-CRUD funktioniert.

**Prompt-Vorschlag**

```
Erstelle Flyway V1__init.sql mit allen oben gelisteten Tabellen, Indizes und Checks. Implementiere die dazu passenden JPA-Entities (records/Builder ok) und Spring Data Repositories.
Füge einen Integrationstest mit Testcontainers (PostgreSQL) hinzu, der Migrationen ausführt und einfache CRUD-Operationen pro Tabelle prüft.
```

---

## 2) Discord OAuth2 – Login & Registrierung

**Flow:** Authorization Code → Token → `/users/@me` → `Member` anlegen/aktualisieren → HttpOnly Session-Cookie.

* [ ] **SecurityConfig** mit Session-Cookies (HttpOnly, Secure, SameSite=Lax).
* [ ] **Custom Provider**: `authorization-uri`, `token-uri`, `user-info-uri`, Scopes `identify` (+ optional `email`).
* [ ] **Endpoints**: `/auth/discord/login`, `/auth/discord/callback`, `/auth/logout`.
* [ ] **User-Linking**: `discord_user_id` als stabiler Identifier; `Member` mit `team` verknüpfen (vorerst ein Team, später multi-team-fähig).
* [ ] **API**: `GET /api/me` gibt Profil (id, discordUserId, displayName, avatarUrl, tz, roles, teamIds).
* [ ] **Tests**: Security-Flow (Mock OAuth), Cookie-Eigenschaften, Fehlerpfade.

**Prompt-Vorschlag**

```
Implementiere Discord OAuth2 als Custom Provider in Spring Security. Endpoints: /auth/discord/login, /auth/discord/callback, /auth/logout. Tausche Code gegen Token, rufe /users/@me ab, lege/aktualisiere Member (discord_user_id als Unique). Lege HttpOnly+Secure Cookie an.
Erstelle GET /api/me. Schreibe WebMvcTests für den Flow (inkl. Fehlerfällen) und prüfe Cookie-Flags.
```

---

## 3) Profile: Name & Avatar (Cloudinary)

* [ ] **PUT /api/me/profile** – `displayName`, `tz` ändern (Validation).
* [ ] **POST /api/me/avatar** – Multipart Upload → Cloudinary → `avatar_url` speichern. Typen: JPEG/PNG/WebP; Größe ≤ 2 MB.
* [ ] **Service**: `CloudinaryService` (Signierter Upload), optional altes Bild entfernen.
* [ ] **Tests**: Upload-Validierung, Fehlerpfade, Maxgröße.

**Prompt-Vorschlag**

```
Baue Cloudinary-Upload. Implementiere POST /api/me/avatar (Multipart) mit Validierung von MIME-Type und Größe. Nutze einen CloudinaryService, der die resultierende URL zurückgibt. Ergänze PUT /api/me/profile.
Schreibe Tests für gültige und ungültige Uploads.
```

---

## 4) Verfügbarkeiten (Kann / Kann nicht)

* [ ] **GET /api/teams/{teamId}/availability?from\&to** – aggregierte Übersicht je Member.
* [ ] **POST /api/availability** – erstellen/aktualisieren (Owner); Eingaben in User‑TZ → Server konvertiert zu UTC.
* [ ] **DELETE /api/availability/{id}\`** – Owner/Admin.
* [ ] **Businessregeln** – keine Überlappungen je Member; max Dauer 24h; `available=false` für „kann nicht“ zulassen.
* [ ] **DST-Tests** – Europe/Berlin Kantenfälle.

**Prompt-Vorschlag**

```
Implementiere Availability-Endpoints inkl. Validierungen. Konvertiere Eingaben aus einer übergebenen Zeitzone (default Europe/Berlin) nach UTC. Verhindere überlappende Slots je Member. Schreibe Unit-Tests für DST-Kantenfälle.
```

---

## 5) Automatische Termingenerierung (Intersection)

**Ziel:** Aus den `available=true`-Fenstern gemeinschaftliche Trainingsslots erzeugen.

* [ ] **Parameter pro Team**: `minPlayers`, `minDurationMinutes`, `weekStart` (Mo 00:00 Europe/Berlin).
* [ ] **Service**: `TrainingPlannerService.intersectWeekly(teamId, monday)` – berechnet Schnittmengen und erzeugt/aktualisiert `training_session` mit `source='AUTO'`.
* [ ] **Idempotenz** – wiederholtes Ausführen ändert nur, was nötig ist.
* [ ] **Tests** – mehrere Spieler, überlappende/teilweise überschneidende Zeitfenster; Mindestdauer/-anzahl.

**Prompt-Vorschlag**

```
Implementiere TrainingPlannerService, der für eine Woche die Schnittmengen verfügbarer Zeitfenster berechnet. Berücksichtige minPlayers und minDurationMinutes. Erzeuge/aktualisiere training_session (source='AUTO'). Schreibe umfangreiche Unit-Tests mit realistischen Beispielen.
```

---

## 6) Team-Übersicht & Admin-Settings

* [ ] **GET /api/teams/{teamId}\`** – Teamdetails + Settings.
* [ ] **GET /api/teams/{teamId}/members\`** – Mitgliederliste.
* [ ] \*\*PUT /api/teams/{teamId}/settings`** – Admin: `reminderChannelId`, `minPlayers`, `minDurationMinutes`, `reminderHours=\[0,6,12,18]\`.
* [ ] **Security** – Rollenprüfung teamgebunden (`ROLE_ADMIN`).
* [ ] **Tests** – Autorisierung & Validierung.

**Prompt-Vorschlag**

```
Erstelle Team-Endpoints inklusive Admin-Settings. Implementiere eine teambezogene Rollenprüfung und Tests für erlaubte/verbotene Zugriffe.
```

---

## 7) Scheduling & Erinnerungen

### 7.1 Pings „Tragt Zeiten ein“ – alle 6 Stunden

* [ ] **Zeitpunkte**: 00:00, 06:00, 12:00, 18:00 Europe/Berlin (pro Team konfigurierbar via `reminderHours`).
* [ ] **Job**: finde Member, die **für die laufende Woche** noch keine Availability gesetzt haben.
* [ ] **Locking**: verteiltes Lock über `job_lock` (oder Quartz JDBC Store).
* [ ] **Nachricht**: gruppierte Mentions `<@discordUserId>` im `reminderChannelId`.
* [ ] **Metriken**: Anzahl gesendeter Mentions, Dauer, Fehler.

**Prompt-Vorschlag**

```
Implementiere ReminderJobService, der zu 00/06/12/18 Europe/Berlin pro Team alle Member ohne Availability für die laufende Woche pingt. Verwende verteiltes Locking über job_lock. Baue strukturierte Logs und Micrometer-Metriken ein.
```

### 7.2 Wöchentliche Trainingsübersicht

* [ ] **Montag 09:00 Europe/Berlin** – Post mit allen `training_session` der Woche.
* [ ] **Format** – Datum/Uhrzeit (Berlin), Dauer, erwartete Spieleranzahl.

**Prompt-Vorschlag**

```
Baue einen WeeklySummaryJob (Mo 09:00 Europe/Berlin), der alle training_session der Woche zusammenfasst und in den Discord-Kanal postet.
```

### 7.3 Erinnerungen vor Trainings

* [ ] **T‑1 Tag 20:00** – Vorab-Erinnerung.
* [ ] **T‑Day 2 Stunden vorher** – Kurzvorher-Erinnerung.

**Prompt-Vorschlag**

```
Implementiere zwei Jobs: 1) 1 Tag vorher um 20:00 Europe/Berlin, 2) 2 Stunden vor Beginn. Nachricht mit Titel, Uhrzeit, Dauer, Teilnehmeranzahl.
```

---

## 8) Discord-Outbound (Adapter)

* [ ] **Port**: `DiscordNotifier` Interface.
* [ ] **Adapter 1**: `DiscordWebhookNotifier` (einfacher Kanalpost, keine Interaktionen).
* [ ] **Adapter 2**: `DiscordBotNotifier` (HTTP an separaten Bot-Service; HMAC-Signaturen; Retry+Backoff).
* [ ] **Konfiguration**: Auswahl über Properties.

**Prompt-Vorschlag**

```
Erstelle ein DiscordNotifier-Interface und zwei Implementierungen (Webhook, Bot-Service). Füge HMAC-Signaturprüfung für eingehende Webhooks und Exponential-Backoff für ausgehende Requests hinzu.
```

---

## 9) API-Dokumentation & Fehlerformat

* [ ] **springdoc-openapi** einbinden.
* [ ] **/v3/api-docs**, **/swagger-ui.html** bereitstellen.
* [ ] **RFC 7807** Problem Details – zentrales Exception-Handling.

**Prompt-Vorschlag**

```
Integriere springdoc-openapi und dokumentiere alle Endpoints/DTOs. Implementiere ein @ControllerAdvice, das RFC-7807 Problem Details zurückgibt.
```

---

## 10) Tests, Observability, Sicherheit

* [ ] **Unit-Tests** – Services (Planner, Reminder, Validation).
* [ ] **Slice-Tests** – `@DataJpaTest`, `@WebMvcTest`.
* [ ] **Integration** – Testcontainers Postgres, kompletter OAuth-Login-Mock.
* [ ] **Metriken** – Micrometer Timer/Counter für Jobs.
* [ ] **Logs** – strukturierte JSON-Logs, Korrelation-ID Middleware.
* [ ] **Security-Härtung** – CORS nur Frontend-Origin, Rate Limits für Outbound, Cookie-Flags, Limits (Max Request Size), Pagination.

**Prompt-Vorschlag**

```
Erzeuge Testsuites (Unit, Slice, Integration) mit Testcontainers. Füge Micrometer-Metriken für Jobs hinzu und strukturierte JSON-Logs mit Korrelations-ID. Härte Security (CORS, Cookies, Limits).
```

---

## 11) CI/CD & Konfiguration

* [ ] **GitHub Actions** – Build, Tests, Testcontainers, Docker Build & Push.
* [ ] **Flyway** – Migrationen automatisch ausführen.
* [ ] **Secrets** – DB, Discord, Cloudinary als Repo- oder Org-Secrets dokumentieren.
* [ ] **Staging-Env** – Dummy-Discord-App/Server.

**Prompt-Vorschlag**

```
Baue eine CI-Pipeline (GitHub Actions) mit Java 21 Setup, Gradle/Maven Cache, Testcontainers-Run, Docker Build & Push. Dokumentiere benötigte Secrets und Umgebungsvariablen.
```

---

## Beispiel-Umgebungsvariablen (.env.example)

```
APP_BASE_URL=https://api.example.com
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:postgresql://localhost:5432/esports
DB_USER=esports
DB_PASSWORD=secret

DISCORD_CLIENT_ID=...
DISCORD_CLIENT_SECRET=...
DISCORD_REDIRECT_URI=http://localhost:8080/auth/discord/callback
DISCORD_BOT_TOKEN=...            # falls direkt gepostet wird
DISCORD_WEBHOOK_URL=...          # Alternative ohne Bot

CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...

REMINDER_HOURS=0,6,12,18
TIMEZONE=Europe/Berlin
MIN_PLAYERS=4
MIN_DURATION_MINUTES=90
```

---

## Änderungschronik

> Hier dokumentiert der Agent nach jedem Commit kurz die Änderungen.

* 2025-07-27 15:02 – Projekt-Basis eingerichtet: Spring Boot 3.5.4, Dependencies, application.yml, docker-compose.yml, .env.example; build.gradle, src/main/resources/

