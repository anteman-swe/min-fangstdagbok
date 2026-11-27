package org.example.fiskeexperten.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Inloggningssidan och Om-sidan.
 * Själva inloggningen och utloggningen sköts av Spring Security (se SecurityConfig),
 * och registreringen ligger i RegistreringController.
 */
@Controller
public class SidController {

    /** Inloggningssidan. Den som redan är inloggad skickas vidare till kartan. */
    @GetMapping("/")
    public String login(Authentication inloggad) {
        if (inloggad != null && !(inloggad instanceof AnonymousAuthenticationToken)
                && inloggad.isAuthenticated()) {
            return "redirect:/karta";
        }
        return "login";
    }

    @GetMapping("/om")
    public String om() {
        return "om";
    }
}
