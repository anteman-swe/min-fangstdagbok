package se.fiskeexperten.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * En registrerad fångst.
 * Just nu en vanlig klass i minnet – blir en JPA-entitet när databasen är på plats
 * (Trello: "Datamodell: Fångst").
 */
public class Fangst {

    private static final Locale SVENSKA = Locale.of("sv", "SE");
    private static final String[] MANADER =
            {"jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"};

    private final Long id;
    private final String art;
    private final Double viktKg;
    private final Integer langdCm;
    private final String plats;
    private final LocalDate datum;
    private final double lat;
    private final double lng;
    /** Nyckel till bilden i BildLagring (null = ingen bild). */
    private final String bildNyckel;

    public Fangst(Long id, String art, Double viktKg, Integer langdCm, String plats,
                  LocalDate datum, double lat, double lng, String bildNyckel) {
        this.id = id;
        this.art = art;
        this.viktKg = viktKg;
        this.langdCm = langdCm;
        this.plats = plats;
        this.datum = datum;
        this.lat = lat;
        this.lng = lng;
        this.bildNyckel = bildNyckel;
    }

    public Long getId() { return id; }
    public String getArt() { return art; }
    public Double getViktKg() { return viktKg; }
    public Integer getLangdCm() { return langdCm; }
    public String getPlats() { return plats; }
    public LocalDate getDatum() { return datum; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
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
