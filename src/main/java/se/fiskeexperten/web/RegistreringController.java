package se.fiskeexperten.web;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import se.fiskeexperten.service.AnvandarService;

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

        if (!result.hasFieldErrors("email") && anvandare.finns(form.getEmail())) {
            result.rejectValue("email", "email.upptagen", UPPTAGEN);
        }

        if (result.hasErrors()) {
            return "registrera";
        }

        try {
            anvandare.skapa(form.getEmail(), losenordsKodare.encode(form.getLosenord()));
        } catch (DataIntegrityViolationException e) {
            // Någon hann registrera samma adress mellan kontrollen och sparandet (unik nyckel i databasen)
            result.rejectValue("email", "email.upptagen", UPPTAGEN);
            return "registrera";
        }

        return "redirect:/?skapad";
    }
}
