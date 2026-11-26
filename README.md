# FiskeExperten 🎣

Fångstdagbok på kartan. Grupp E.
Spring Boot 4.2 + Thymeleaf. Designen kommer från Lovable-prototypen, översatt från React/Tailwind till Thymeleaf-mallar och vanlig CSS.

## Kör igång

Krav: Java 21+, Maven (eller IntelliJ, som har Maven inbyggt) och en PostgreSQL med PostGIS (se Databas nedan).

```sh
export POSTGIS_PASSWORD=...   # lösenordet för appuser
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Öppna sedan http://localhost:8080

I IntelliJ: öppna mappen → högerklicka `FiskeExpertenApplication` → **Run**, skriv `dev` under
*Active profiles* och lägg `POSTGIS_PASSWORD` under *Environment variables* i run-konfigurationen.

Profilen `dev` lägger in ett demokonto (`application-dev.properties`). Utan den finns inget demokonto, och
man skapar ett konto på `/registrera`. Använd inte `dev` i en riktig miljö.

## Databas

Allt (användare, fångster och positioner) ligger i **en** PostGIS-databas. Positionen är en
`geometry(Point, 4326)`-kolumn på `fangst`, så en fångst och dess plats sparas i samma transaktion.

| Miljövariabel | Standardvärde |
|---|---|
| `POSTGIS_HOST` | `localhost` |
| `POSTGIS_PORT` | `5432` |
| `POSTGIS_DB` | `gis` |
| `POSTGIS_USER` | `appuser` |
| `POSTGIS_PASSWORD` | (ingen – måste sättas) |

Tabellerna skapas av **Flyway** vid start (`src/main/resources/db/migration`). Ändra aldrig en migrering som
redan körts – lägg ändringar i en ny fil (`V2__...sql`). Kraven på databasen:

- `appuser` ska äga databasen (eller ha `CREATE` på schemat `public`), annars kan Flyway inte skapa tabellerna.
- PostGIS-tillägget måste finnas i databasen. PostGIS-containern skapar det i databasen som `POSTGRES_DB` pekar
  på; annars kör en superanvändare `CREATE EXTENSION postgis;` en gång.
- Med profilen `dev` läggs demokontot in med fyra exempelfångster första gången appen startar mot en tom
  databas (se `DemoData`).

Testerna startar en egen tillfällig PostGIS i Docker (Testcontainers), så **Docker måste köra** när du kör
`mvn test`. De rör aldrig din riktiga databas.

## Sidor

| URL | Mall | Vad |
|---|---|---|
| `/` | `login.html` | Inloggning (Spring Security). Med profilen `dev`: `demo@fiskeexperten.se` / `demo1234` |
| `/registrera` | `registrera.html` | Skapa konto |
| `POST /logga-ut` | | Loggar ut |
| `/karta` | `karta.html` | Karta + lista med den inloggade användarens fångster + panel för ny fångst |
| `/karta?q=gädda` | | Sök på art eller plats |
| `POST /fangster` | | Sparar en ny fångst (med validering) |
| `/karta?redigera=5` | | Öppnar panelen med fångst 5 ifylld |
| `POST /fangster/{id}` | | Sparar ändringar |
| `POST /fangster/{id}/radera` | | Raderar fångsten och dess bild |
| `/fangster/{id}/bild` | | Fångstens bild |
| `/om` | `om.html` | Om-sidan |

## Struktur

```
src/main/java/se/fiskeexperten/
├── FiskeExpertenApplication.java
├── config/SecurityConfig.java   ← inloggning (Spring Security)
├── config/DemoData.java         ← demokonto + exempelfångster i en tom databas
├── model/Fangst.java            ← JPA-entitet, position som PostGIS-punkt
├── model/Anvandare.java         ← JPA-entitet, lösenordet som hash
├── repository/                  ← Spring Data-repositories
├── service/FangstService.java   ← fångster i databasen, alltid per ägare
├── service/AnvandarService.java ← användare i databasen (UserDetailsService)
├── service/BildLagring.java     ← interface för bilder (S3-versionen implementerar detta)
├── service/MinnesBildLagring.java ← bilder i minnet tills S3 är på plats
└── web/
    ├── KartaController.java     ← /karta och POST /fangster
    ├── SidController.java       ← /, /login, /logga-ut, /om
    └── FangstForm.java          ← formulärdata + validering

src/main/resources/
├── templates/
│   ├── fragments/layout.html    ← <head> och toppmenyn (återanvänds på alla sidor)
│   ├── fragments/icons.html     ← Lucide-ikoner som Thymeleaf-fragment
│   ├── login.html, karta.html, om.html
│   ├── error.html, error/404.html
├── static/css/app.css           ← all stil (ersätter Tailwind)
├── static/js/karta.js           ← Leaflet-kartan, panelen, bildförhandsvisning
└── messages.properties          ← svenska felmeddelanden
```

## Kartan

Lovable-versionen hade en ritad låtsaskarta. Här används **Leaflet + OpenStreetMap** med riktiga koordinater,
och i panelen "Registrera fångst" klickar man på kartan för att välja position.

## Inte klart än (finns på Trello)

- **Bilder:** sparas i minnet och försvinner vid omstart (databasen minns bara bildnyckeln) → skapa `S3BildLagring implements BildLagring`
- **AI:** knappen "Identifiera art med AI" är avstängd → Spring AI
