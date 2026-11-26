package se.fiskeexperten;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Demokontot är avstängt som standard, så testet sätter sina egna uppgifter (samma som konstanterna nedan).
@SpringBootTest(properties = {
        "fiskeexperten.demo.email=demo@fiskeexperten.se",
        "fiskeexperten.demo.losenord=demo1234"
})
@AutoConfigureMockMvc
class InloggningTest {

    private static final String DEMO_EPOST = "demo@fiskeexperten.se";
    private static final String DEMO_LOSENORD = "demo1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService anvandare;

    @Autowired
    private PasswordEncoder losenordsKodare;

    @Test
    void inloggningssidanArOppen() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registreringssidanArOppen() throws Exception {
        mockMvc.perform(get("/registrera"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrera"));
    }

    @Test
    void skyddadeSidorSkickarOinloggadeTillInloggningen() throws Exception {
        for (String url : new String[]{"/karta", "/om", "/fangster/1/bild"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }
    }

    @Test
    void ingenKanSkapaFangstUtanInloggning() throws Exception {
        mockMvc.perform(post("/fangster").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void inloggadAnvandareKommerInTillKartan() throws Exception {
        mockMvc.perform(get("/karta").with(user("nagon@exempel.se").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("karta"));
    }

    @Test
    void rattLosenordLoggarIn() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", DEMO_EPOST)
                        .param("losenord", DEMO_LOSENORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/karta"))
                .andExpect(authenticated().withUsername(DEMO_EPOST));
    }

    @Test
    void felLosenordNekas() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", DEMO_EPOST)
                        .param("losenord", "fel-losenord"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?fel"))
                .andExpect(unauthenticated());
    }

    @Test
    void inloggningUtanCsrfTokenNekas() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", DEMO_EPOST)
                        .param("losenord", DEMO_LOSENORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void utloggningKraverPostOchSkickarTillInloggningen() throws Exception {
        mockMvc.perform(post("/logga-ut").with(csrf()).with(user(DEMO_EPOST).roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?utloggad"));
    }

    @Test
    void nyttKontoSkapasOchLosenordetSparasKrypterat() throws Exception {
        String epost = "ny.fiskare@exempel.se";
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", "  Ny.Fiskare@Exempel.se ")
                        .param("losenord", "hemligt-nog")
                        .param("losenordIgen", "hemligt-nog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?skapad"));

        var skapad = anvandare.loadUserByUsername(epost);
        assertThat(skapad.getPassword()).isNotEqualTo("hemligt-nog");
        assertThat(losenordsKodare.matches("hemligt-nog", skapad.getPassword())).isTrue();

        // ...och det nya kontot kan logga in
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", epost)
                        .param("losenord", "hemligt-nog"))
                .andExpect(redirectedUrl("/karta"))
                .andExpect(authenticated().withUsername(epost));
    }

    @Test
    void registreringMedOlikaLosenordVisarFel() throws Exception {
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", "olika@exempel.se")
                        .param("losenord", "hemligt-nog")
                        .param("losenordIgen", "nagot-annat"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrera"))
                .andExpect(model().attributeHasFieldErrors("registreringForm", "losenordIgen"));
    }

    @Test
    void losenordSomArForLangtIByteVisarFelPaLosenordetInnanDetKodas() throws Exception {
        // 40 × "å" är bara 40 tecken men 80 byte i UTF-8 – över BCrypts gräns på 72 byte
        String forLangt = "å".repeat(40);
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", "bytegrans@exempel.se")
                        .param("losenord", forLangt)
                        .param("losenordIgen", forLangt))
                .andExpect(status().isOk())
                .andExpect(view().name("registrera"))
                .andExpect(model().attributeHasFieldErrors("registreringForm", "losenord"));

        assertThat(anvandareFinns("bytegrans@exempel.se")).isFalse();
    }

    @Test
    void losenordPaExakt72ByteAccepteras() throws Exception {
        // 36 × "å" = 36 tecken men exakt 72 byte
        String exakt = "å".repeat(36);
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", "exakt72@exempel.se")
                        .param("losenord", exakt)
                        .param("losenordIgen", exakt))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?skapad"));
    }

    @Test
    void demokontotArAvstangtSomStandard() throws Exception {
        // application.properties lämnar demouppgifterna tomma; bara dev-profilen (eller testets
        // egna egenskaper ovan) slår på dem.
        Properties standard = new Properties();
        try (InputStream in = InloggningTest.class.getResourceAsStream("/application.properties")) {
            standard.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        assertThat(standard.getProperty("fiskeexperten.demo.email", "").trim()).isEmpty();
        assertThat(standard.getProperty("fiskeexperten.demo.losenord", "").trim()).isEmpty();
    }

    private boolean anvandareFinns(String epost) {
        try {
            anvandare.loadUserByUsername(epost);
            return true;
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }

    @Test
    void registreringMedForKortLosenordVisarFel() throws Exception {
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", "kort@exempel.se")
                        .param("losenord", "kort")
                        .param("losenordIgen", "kort"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registreringForm", "losenord"));
    }

    @Test
    void registreringMedUpptagenEpostVisarFel() throws Exception {
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", DEMO_EPOST.toUpperCase())
                        .param("losenord", "hemligt-nog")
                        .param("losenordIgen", "hemligt-nog"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registreringForm", "email"));
    }

    @Test
    void registreringMedOgiltigEpostVisarFel() throws Exception {
        mockMvc.perform(post("/registrera").with(csrf())
                        .param("email", "inte-en-epost")
                        .param("losenord", "hemligt-nog")
                        .param("losenordIgen", "hemligt-nog"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registreringForm", "email"));
    }
}
