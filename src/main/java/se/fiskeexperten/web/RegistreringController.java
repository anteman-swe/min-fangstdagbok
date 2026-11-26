package se.fiskeexperten.web;

import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Locale;

/** Skapa konto. */
@Controller
public class RegistreringController {

    private final UserDetailsManager anvandare;
    private final PasswordEncoder losenordsKodare;

    public RegistreringController(UserDetailsManager anvandare, PasswordEncoder losenordsKodare) {
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

        String epost = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!result.hasFieldErrors("email") && anvandare.userExists(epost)) {
            result.rejectValue("email", "email.upptagen", "Det finns redan ett konto med den e-postadressen");
        }

        if (result.hasErrors()) {
            return "registrera";
        }

        anvandare.createUser(User.withUsername(epost)
                .password(losenordsKodare.encode(form.getLosenord()))
                .roles("USER")
                .build());

        return "redirect:/?skapad";
    }
}
