package org.example.fiskeexperten;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.example.fiskeexperten.model.Fangst;
import org.example.fiskeexperten.service.AnvandarService;
import org.example.fiskeexperten.service.FangstService;
import org.example.fiskeexperten.controller.FangstForm;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testar fångster mot en riktig PostGIS: sparande, positioner, sökning och ägarskap. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostGisTestConfig.class)
class FangstDatabasTest {

    @Autowired
    private FangstService fangstService;

    @Autowired
    private AnvandarService anvandarService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long nyAnvandare(String epost) {
        return anvandarService.skapa(epost, "{noop}hemligt").getId();
    }

    private FangstForm form(String art, String plats, double lat, double lng) {
        FangstForm form = FangstForm.ny();
        form.setArt(art);
        form.setPlats(plats);
        form.setVikt(2.5);
        form.setLangd(60);
        form.setDatum(LocalDate.of(2026, 11, 1));
        form.setLat(lat);
        form.setLng(lng);
        return form;
    }

    @Test
    void positionenSparasSomPostGisPunktMedRattKoordinatsystem() {
        Long agare = nyAnvandare("punkt@exempel.se");

        Fangst sparad = fangstService.spara(agare, form("Gädda", "Mälaren", 59.585, 16.585), null);

        Fangst hamtad = fangstService.hitta(agare, sparad.getId()).orElseThrow();
        assertThat(hamtad.getLat()).isEqualTo(59.585);
        assertThat(hamtad.getLng()).isEqualTo(16.585);

        // Kolla i databasen att det verkligen är en geometry-punkt i WGS 84 (x = lng, y = lat)
        Integer srid = jdbc.queryForObject("select ST_SRID(punkt) from fangst where id = ?",
                Integer.class, sparad.getId());
        Double lng = jdbc.queryForObject("select ST_X(punkt) from fangst where id = ?",
                Double.class, sparad.getId());
        Double lat = jdbc.queryForObject("select ST_Y(punkt) from fangst where id = ?",
                Double.class, sparad.getId());
        assertThat(srid).isEqualTo(4326);
        assertThat(lng).isEqualTo(16.585);
        assertThat(lat).isEqualTo(59.585);
    }

    @Test
    void enAnvandareSerBaraSinaEgnaFangster() {
        Long anna = nyAnvandare("anna@exempel.se");
        Long bertil = nyAnvandare("bertil@exempel.se");

        Fangst annas = fangstService.spara(anna, form("Gädda", "Mälaren", 59.5, 16.5), null);
        fangstService.spara(bertil, form("Abborre", "Vättern", 58.3, 14.2), null);

        assertThat(fangstService.sok(anna, null)).extracting(Fangst::getArt).containsExactly("Gädda");
        assertThat(fangstService.sok(bertil, null)).extracting(Fangst::getArt).containsExactly("Abborre");

        // Bertil kan varken hämta, ändra eller radera Annas fångst
        assertThat(fangstService.hitta(bertil, annas.getId())).isEmpty();
        assertThat(fangstService.uppdatera(bertil, annas.getId(), form("Gös", "Hjälmaren", 59.2, 15.7), null)).isEmpty();
        assertThat(fangstService.radera(bertil, annas.getId())).isEmpty();
        assertThat(fangstService.hitta(anna, annas.getId()).orElseThrow().getArt()).isEqualTo("Gädda");
    }

    @Test
    void sokningMatcharArtOchPlatsUtanAttBryUppPaStorlek() {
        Long agare = nyAnvandare("sok@exempel.se");
        fangstService.spara(agare, form("Gädda", "Mälaren, Västerås", 59.5, 16.5), null);
        fangstService.spara(agare, form("Abborre", "Vättern, Hjo", 58.3, 14.2), null);
        fangstService.spara(agare, form("Öring", "Klarälven", 60.1, 13.4), null);

        assertThat(fangstService.sok(agare, "GÄDDA")).extracting(Fangst::getArt).containsExactly("Gädda");
        assertThat(fangstService.sok(agare, "  hjo ")).extracting(Fangst::getArt).containsExactly("Abborre");
        assertThat(fangstService.sok(agare, "öring")).extracting(Fangst::getArt).containsExactly("Öring");
        assertThat(fangstService.sok(agare, "")).hasSize(3);
        assertThat(fangstService.sok(agare, null)).hasSize(3);
        assertThat(fangstService.sok(agare, "finnsinte")).isEmpty();
        // % och _ är vanliga tecken, inga jokertecken
        assertThat(fangstService.sok(agare, "%")).isEmpty();
    }

    @Test
    void nyastFangstKommerForst() {
        Long agare = nyAnvandare("ordning@exempel.se");
        FangstForm gammal = form("Gammal", "A", 59.0, 16.0);
        gammal.setDatum(LocalDate.of(2026, 1, 1));
        FangstForm ny = form("Ny", "B", 59.0, 16.0);
        ny.setDatum(LocalDate.of(2026, 6, 1));
        fangstService.spara(agare, gammal, null);
        fangstService.spara(agare, ny, null);

        List<Fangst> lista = fangstService.sok(agare, null);

        assertThat(lista).extracting(Fangst::getArt).containsExactly("Ny", "Gammal");
    }

    @Test
    void uppdateraAndrarRadenOchBehallerBildNyckelOmIngenNyBild() {
        Long agare = nyAnvandare("uppdatera@exempel.se");
        Fangst sparad = fangstService.spara(agare, form("Gädda", "Mälaren", 59.5, 16.5), "bild-123");

        Fangst uppdaterad = fangstService.uppdatera(agare, sparad.getId(),
                form("Gös", "Hjälmaren", 59.2, 15.7), null).orElseThrow();

        assertThat(uppdaterad.getId()).isEqualTo(sparad.getId());
        Fangst fran = fangstService.hitta(agare, sparad.getId()).orElseThrow();
        assertThat(fran.getArt()).isEqualTo("Gös");
        assertThat(fran.getLat()).isEqualTo(59.2);
        assertThat(fran.getLng()).isEqualTo(15.7);
        assertThat(fran.getBildNyckel()).isEqualTo("bild-123");
        assertThat(fangstService.sok(agare, null)).hasSize(1); // uppdaterad, inte duplicerad
    }

    @Test
    void raderaTarBortRadenOchReturnerarFangstenSaAttBildenKanRaderas() {
        Long agare = nyAnvandare("radera@exempel.se");
        Fangst sparad = fangstService.spara(agare, form("Gädda", "Mälaren", 59.5, 16.5), "bild-456");

        Fangst borta = fangstService.radera(agare, sparad.getId()).orElseThrow();

        assertThat(borta.getBildNyckel()).isEqualTo("bild-456");
        assertThat(fangstService.hitta(agare, sparad.getId())).isEmpty();
    }

    @Test
    void fangsterRaderasNarAnvandarenRaderas() {
        Long agare = nyAnvandare("cascade@exempel.se");
        Fangst sparad = fangstService.spara(agare, form("Gädda", "Mälaren", 59.5, 16.5), null);

        jdbc.update("delete from anvandare where id = ?", agare);

        Integer kvar = jdbc.queryForObject("select count(*) from fangst where id = ?",
                Integer.class, sparad.getId());
        assertThat(kvar).isZero();
    }
}
