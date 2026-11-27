# Fiskeexperten
En fiskekarta med AI-analys av fiskebilder

## Kör igång

Krav: Java (se `java.version` i `pom.xml`) och Docker.

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- `spring-boot-docker-compose` startar PostGIS och Garage (S3) från `compose.yaml` automatiskt.
- Profilen `dev` lägger in ett demokonto: **demo@fiskeexperten.se / demo1234** med fyra exempelfångster.
  Utan profilen skapar du ett konto på `/registrera`.
- Öppna http://localhost:8080

Tailwind byggs av `tailwind-maven-plugin` vid `./mvnw compile` (`static/css/input.css` → `output.css`).
Kör det efter att du ändrat klasser i mallarna.

## Funktioner

- Inloggning och registrering (Spring Security, BCrypt, sessioner i databasen)
- Fångster sparas i PostgreSQL/PostGIS; varje användare ser bara sina egna
- Karta med Leaflet + OpenStreetMap, sök, redigera och radera fångster, bilduppladdning
- Ctrl + scroll för att zooma kartan

## Databas

Flyway äger schemat (`src/main/resources/db/migration`). Ändra aldrig en körd migrering –
lägg till en ny fil (`V3__...sql`). Hibernate kontrollerar bara att entiteterna matchar (`ddl-auto=validate`).

## Tester

```sh
./mvnw test                                # Java-tester (startar PostGIS i Docker via Testcontainers)
node --test "src/test/js/**/*.test.js"     # tester för karta.js
```

## Inte klart än

- **Bilder** sparas i minnet (`MinnesBildLagring`). Skapa `S3BildLagring implements BildLagring` mot Garage.
- **AI** – knappen "Identifiera art med AI" är avstängd tills Spring AI är inkopplat.
