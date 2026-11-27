package org.example.fiskeexperten.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.example.fiskeexperten.model.Anvandare;
import org.example.fiskeexperten.service.AnvandarService;

/** Skapa konto. */
@Controller
public class RegistreringController {

    private static final String UPPTAGEN = "Det finns redan ett konto med den e-postadressen";

    private final AnvandarService anvandare;
    private final PasswordEncoder losenordsKodare;

    public RegistreringController(AnvandarService anvandare, PasswordEncoder losenordsKodare) {
        this.anvandare = anvandare;
        this.losenordsKodare = losenordsKodare;
    }

    @GetMapping("/registrera")
    public String visa(@ModelAttribute("registreringForm") RegistreringForm form) {
        return "registrera";
    }

    @PostMapping("/registrera")
    public String skapa(@Valid @ModelAttribute("registreringForm") RegistreringForm form,
                        BindingResult result) {
        if (form.getLosenord() != null && form.getLosenordIgen() != null
                && !form.getLosenord().equals(form.getLosenordIgen())) {
            result.rejectValue("losenordIgen", "losenord.olika", "Lösenorden är inte likadana");
        }

        // Samma normalisering som vid sparandet (trimmad, gemener), så att längd- och dubblettkontrollen
        // gäller exakt det värde som hamnar i databasen.
        String epost = AnvandarService.normalisera(form.getEmail());
        if (!result.hasFieldErrors("email")) {
            if (epost.length() > Anvandare.MAX_EPOST_LANGD) {
                result.rejectValue("email", "email.langt", "Högst " + Anvandare.MAX_EPOST_LANGD + " tecken");
            } else if (anvandare.finns(epost)) {
                result.rejectValue("email", "email.upptagen", UPPTAGEN);
            }
        }

        if (result.hasErrors()) {
            return "registrera";
        }

        try {
            anvandare.skapa(epost, losenordsKodare.encode(form.getLosenord()));
        } catch (DataIntegrityViolationException e) {
            // Visa "upptagen" bara om kontot faktiskt finns nu, dvs. någon hann registrera samma adress
            // mellan kontrollen och sparandet. Alla andra databasfel är riktiga fel och ska inte döljas.
            if (!anvandare.finns(epost)) {
                throw e;
            }
            result.rejectValue("email", "email.upptagen", UPPTAGEN);
            return "registrera";
        }

        return "redirect:/?skapad";
    }
}
