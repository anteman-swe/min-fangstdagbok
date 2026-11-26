-- Schemat ägs av Flyway (Hibernate ändrar inget, ddl-auto=none).
-- Nya ändringar läggs i nya filer: V2__..., V3__... – ändra aldrig en körd migrering.

-- PostGIS-bilderna skapar redan tillägget i databasen som POSTGRES_DB pekar på.
-- Finns det kräver raden nedan inga särskilda rättigheter; annars måste en superanvändare
-- köra den en gång:  CREATE EXTENSION postgis;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE anvandare (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    epost          VARCHAR(120)  NOT NULL UNIQUE,
    losenord_hash  VARCHAR(100)  NOT NULL,
    skapad         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE fangst (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agare_id     BIGINT           NOT NULL REFERENCES anvandare (id) ON DELETE CASCADE,
    art          VARCHAR(60)      NOT NULL,
    vikt_kg      DOUBLE PRECISION CHECK (vikt_kg > 0),
    langd_cm     INTEGER          CHECK (langd_cm > 0),
    plats        VARCHAR(80)      NOT NULL,
    datum        DATE             NOT NULL,
    -- WGS 84 (SRID 4326), x = longitud, y = latitud – samma som Leaflet/OpenStreetMap
    punkt        geometry(Point, 4326) NOT NULL,
    bild_nyckel  VARCHAR(255)
);

-- Listan på kartan hämtar en användares fångster, nyast först
CREATE INDEX fangst_agare_datum_idx ON fangst (agare_id, datum DESC, id DESC);

-- För spatiala frågor, t.ex. "fångster inom 5 km"
CREATE INDEX fangst_punkt_gist_idx ON fangst USING GIST (punkt);
