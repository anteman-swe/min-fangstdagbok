package se.fiskeexperten.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * En registrerad fångst, sparad i tabellen "fangst".
 * Positionen ligger som en PostGIS-punkt (geometry(Point, 4326)); getLat()/getLng()
 * plockar ut koordinaterna så att mallar och controllers kan fortsätta använda dem som förut.
 */
@Entity
@Table(name = "fangst")
public class Fangst {

    private static final Locale SVENSKA = Locale.of("sv", "SE");
    private static final String[] MANADER =
            {"jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"};
    /** WGS 84 – samma koordinatsystem som Leaflet och OpenStreetMap. */
    private static final GeometryFactory GEOMETRI = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Användaren som äger fångsten (främmande nyckel till anvandare.id). */
    @Column(name = "agare_id", nullable = false)
    private Long agareId;

    @Column(nullable = false, length = 60)
    private String art;

    @Column(name = "vikt_kg")
    private Double viktKg;

    @Column(name = "langd_cm")
    private Integer langdCm;

    @Column(nullable = false, length = 80)
    private String plats;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(name = "punkt", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point punkt;

    /** Nyckel till bilden i BildLagring (null = ingen bild). */
    @Column(name = "bild_nyckel")
    private String bildNyckel;

    /** Krävs av JPA. */
    protected Fangst() {
    }

    /** id är null för en ny fångst – databasen delar ut det. */
    public Fangst(Long id, Long agareId, String art, Double viktKg, Integer langdCm, String plats,
                  LocalDate datum, double lat, double lng, String bildNyckel) {
        this.id = id;
        this.agareId = agareId;
        this.art = art;
        this.viktKg = viktKg;
        this.langdCm = langdCm;
        this.plats = plats;
        this.datum = datum;
        this.punkt = GEOMETRI.createPoint(new Coordinate(lng, lat)); // x = longitud, y = latitud
        this.bildNyckel = bildNyckel;
    }

    public Long getId() { return id; }
    public Long getAgareId() { return agareId; }
    public String getArt() { return art; }
    public Double getViktKg() { return viktKg; }
    public Integer getLangdCm() { return langdCm; }
    public String getPlats() { return plats; }
    public LocalDate getDatum() { return datum; }
    public double getLat() { return punkt.getY(); }
    public double getLng() { return punkt.getX(); }
    public String getBildNyckel() { return bildNyckel; }

    // ---- Hjälpmetoder för visning i Thymeleaf ----

    public boolean isHarBild() {
        return bildNyckel != null;
    }

    /** "4,2 kg" */
    public String getViktText() {
        return viktKg == null ? null : String.format(SVENSKA, "%.1f kg", viktKg);
    }

    /** "82 cm" */
    public String getLangdText() {
        return langdCm == null ? null : langdCm + " cm";
    }

    /** "12 nov" */
    public String getDatumText() {
        return datum.getDayOfMonth() + " " + MANADER[datum.getMonthValue() - 1];
    }

    /** "4,2 kg · 82 cm" (hoppar över det som saknas) */
    public String getMatt() {
        List<String> delar = new ArrayList<>();
        if (getViktText() != null) delar.add(getViktText());
        if (getLangdText() != null) delar.add(getLangdText());
        return delar.isEmpty() ? "Inga mått angivna" : String.join(" · ", delar);
    }

    /** CSS-klass för färgtoningen som visas när det inte finns någon bild (grad-1 … grad-4). */
    public String getStil() {
        return "grad-" + (Math.floorMod(id - 1, 4) + 1);
    }
}
