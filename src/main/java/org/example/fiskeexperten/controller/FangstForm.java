package org.example.fiskeexperten.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import org.example.fiskeexperten.model.Fangst;

import java.time.LocalDate;

/** Formulärdata för "Registrera fångst" och "Redigera fångst". */
public class FangstForm {

    @NotBlank(message = "Ange vilken art det är")
    @Size(max = 60, message = "Högst 60 tecken")
    private String art;

    @Positive(message = "Vikten måste vara större än 0")
    private Double vikt;

    @Positive(message = "Längden måste vara större än 0")
    private Integer langd;

    @NotNull(message = "Ange datum")
    @PastOrPresent(message = "Datumet kan inte vara i framtiden")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datum;

    @Size(max = 80, message = "Högst 80 tecken")
    private String plats;

    @NotNull(message = "Klicka på kartan för att välja position")
    @DecimalMin("-90") @DecimalMax("90")
    private Double lat;

    @NotNull(message = "Klicka på kartan för att välja position")
    @DecimalMin("-180") @DecimalMax("180")
    private Double lng;

    /** Vald bild (frivillig). Kontrolleras i controllern att det är en bild. */
    private MultipartFile bild;

    /** Satt när en befintlig fångst redigeras, annars null. */
    private Long id;

    /** Tomt formulär med dagens datum ifyllt. */
    public static FangstForm ny() {
        FangstForm form = new FangstForm();
        form.setDatum(LocalDate.now());
        return form;
    }

    /** Formulär ifyllt med en befintlig fångst (för redigering). */
    public static FangstForm fran(Fangst f) {
        FangstForm form = new FangstForm();
        form.setId(f.getId());
        form.setArt(f.getArt());
        form.setVikt(f.getViktKg());
        form.setLangd(f.getLangdCm());
        form.setDatum(f.getDatum());
        form.setPlats(f.getPlats());
        form.setLat(f.getLat());
        form.setLng(f.getLng());
        return form;
    }

    public boolean isRedigerar() { return id != null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MultipartFile getBild() { return bild; }
    public void setBild(MultipartFile bild) { this.bild = bild; }

    public String getArt() { return art; }
    public void setArt(String art) { this.art = art; }

    public Double getVikt() { return vikt; }
    public void setVikt(Double vikt) { this.vikt = vikt; }

    public Integer getLangd() { return langd; }
    public void setLangd(Integer langd) { this.langd = langd; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    public String getPlats() { return plats; }
    public void setPlats(String plats) { this.plats = plats; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
}
