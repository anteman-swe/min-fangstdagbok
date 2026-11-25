# FiskeExperten 🎣

Fångstdagbok på kartan. Grupp E.
Spring Boot 4.2 + Thymeleaf. Designen kommer från Lovable-prototypen, översatt från React/Tailwind till Thymeleaf-mallar och vanlig CSS.

## Kör igång

Krav: Java 21+ och Maven (eller IntelliJ, som har Maven inbyggt).

```sh
mvn spring-boot:run
```

Öppna sedan http://localhost:8080

I IntelliJ: öppna mappen → högerklicka `FiskeExpertenApplication` → **Run**.

## Sidor

| URL | Mall | Vad |
|---|---|---|
| `/` | `login.html` | Inloggning (fejkad – skickar bara vidare till kartan) |
| `/karta` | `karta.html` | Karta + lista med fångster + panel för ny fångst |
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
├── model/Fangst.java            ← fångst (blir JPA-entitet senare)
├── service/FangstService.java   ← fångster i minnet + exempeldata
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

- **Databas:** fångsterna ligger i minnet och försvinner vid omstart → PostgreSQL/PostGIS
- **Bilder:** sparas i minnet → skapa `S3BildLagring implements BildLagring`
- **AI:** knappen "Identifiera art med AI" är avstängd → Spring AI
- **Inloggning:** fejkad → Spring Security
- 
