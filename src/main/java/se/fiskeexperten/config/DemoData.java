package se.fiskeexperten.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.fiskeexperten.model.Anvandare;
import se.fiskeexperten.model.Fangst;
import se.fiskeexperten.repository.FangstRepository;
import se.fiskeexperten.service.AnvandarService;

import java.time.LocalDate;

/**
 * Lägger in ett demokonto med fyra exempelfångster första gången appen startar mot en tom databas
 * (så att kartan inte är tom under utveckling). Görs ingenting om kontot redan finns.
 * Avstängt som standard (fiskeexperten.demo.email och .losenord är tomma i application.properties);
 * slås på med profilen "dev" (application-dev.properties).
 */
@Component
public class DemoData implements ApplicationRunner {

    private final AnvandarService anvandare;
    private final FangstRepository fangster;
    private final PasswordEncoder losenordsKodare;
    private final String epost;
    private final String losenord;

    public DemoData(AnvandarService anvandare,
                    FangstRepository fangster,
                    PasswordEncoder losenordsKodare,
                    @Value("${fiskeexperten.demo.email:}") String epost,
                    @Value("${fiskeexperten.demo.losenord:}") String losenord) {
        this.anvandare = anvandare;
        this.fangster = fangster;
        this.losenordsKodare = losenordsKodare;
        this.epost = epost;
        this.losenord = losenord;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (epost.isBlank() || losenord.isBlank() || anvandare.finns(epost)) {
            return;
        }
        Anvandare demo = anvandare.skapa(epost, losenordsKodare.encode(losenord));
        Long id = demo.getId();

        // Samma exempelfångster som i Lovable-versionen, med riktiga koordinater
        fangster.save(new Fangst(null, id, "Gädda", 4.2, 82, "Mälaren, Västerås", LocalDate.of(2026, 11, 12), 59.585, 16.585, null));
        fangster.save(new Fangst(null, id, "Abborre", 0.9, 38, "Vättern, Hjo", LocalDate.of(2026, 11, 8), 58.305, 14.245, null));
        fangster.save(new Fangst(null, id, "Gös", 2.7, 64, "Hjälmaren", LocalDate.of(2026, 11, 2), 59.230, 15.700, null));
        fangster.save(new Fangst(null, id, "Öring", 1.6, 51, "Klarälven", LocalDate.of(2026, 10, 28), 60.170, 13.470, null));
    }
}
