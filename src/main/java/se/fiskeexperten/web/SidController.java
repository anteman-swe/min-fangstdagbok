package se.fiskeexperten.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/** Inloggning, utloggning och Om-sidan. */
@Controller
public class SidController {

    @GetMapping("/")
    public String login() {
        return "login";
    }

    /**
     * Fejkad inloggning – skickar bara vidare till kartan.
     * TODO (Trello: "Inloggning och användare"): ersätt med Spring Security.
     */
    @PostMapping("/login")
    public String loggaIn() {
        return "redirect:/karta";
    }

    @GetMapping("/logga-ut")
    public String loggaUt() {
        return "redirect:/";
    }

    @GetMapping("/om")
    public String om() {
        return "om";
    }
}
