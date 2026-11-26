package se.fiskeexperten.web;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import se.fiskeexperten.model.Fangst;
import se.fiskeexperten.service.AnvandarService;
import se.fiskeexperten.service.BildLagring;
import se.fiskeexperten.service.FangstService;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Controller
public class KartaController {

    private final FangstService fangstService;
    private final AnvandarService anvandarService;
    private final BildLagring bildLagring;

    public KartaController(FangstService fangstService, AnvandarService anvandarService, BildLagring bildLagring) {
        this.fangstService = fangstService;
        this.anvandarService = anvandarService;
        this.bildLagring = bildLagring;
    }

    /**
     * Kartan + listan med den inloggade användarens fångster.
     * ?q=      söker på art/plats
     * ?vald=   öppnar en fångst på kartan
     * ?redigera= öppnar panelen med en fångst ifylld
     */
    @GetMapping("/karta")
    public String karta(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Long vald,
                        @RequestParam(required = false) Long redigera,
                        Principal principal,
                        Model model) {
        Long agare = agare(principal);
        Optional<Fangst> attRedigera = redigera == null ? Optional.empty() : fangstService.hitta(agare, redigera);

        if (attRedigera.isPresent()) {
            model.addAttribute("fangstForm", FangstForm.fran(attRedigera.get()));
            model.addAttribute("redigerad", attRedigera.get());
            model.addAttribute("formOppet", true);
        } else {
            model.addAttribute("fangstForm", FangstForm.ny());
        }
        model.addAttribute("vald", vald);
        fyllModell(model, agare, q);
        return "karta";
    }

    /** Ny fångst. */
    @PostMapping("/fangster")
    public String sparaFangst(@Valid @ModelAttribute("fangstForm") FangstForm form,
                              BindingResult result,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirect) throws IOException {
        Long agare = agare(principal);
        form.setId(null);
        kontrolleraBild(form, result);
        if (result.hasErrors()) {
            return visaFormularIgen(model, agare, null);
        }

        // TODO (Trello: "Att kolla fiskart från bilden med AI"): skicka bilden till Spring AI
        String bildNyckel = sparaBildOmFinns(form);
        Fangst ny;
        try {
            ny = fangstService.spara(agare, form, bildNyckel);
        } catch (RuntimeException e) {
            // Sparandet misslyckades: släng den nyuppladdade bilden så att den inte blir hängande utan fångst
            if (bildNyckel != null) {
                bildLagring.radera(bildNyckel);
            }
            throw e;
        }

        redirect.addFlashAttribute("meddelande", ny.getArt() + " sparad!");
        redirect.addAttribute("vald", ny.getId());
        return "redirect:/karta";
    }

    /** Spara ändringar på en befintlig fångst. */
    @PostMapping("/fangster/{id}")
    public String uppdateraFangst(@PathVariable Long id,
                                  @Valid @ModelAttribute("fangstForm") FangstForm form,
                                  BindingResult result,
                                  Principal principal,
                                  Model model,
                                  RedirectAttributes redirect) throws IOException {
        Long agare = agare(principal);
        Optional<Fangst> gammal = fangstService.hitta(agare, id);
        if (gammal.isEmpty()) {
            redirect.addFlashAttribute("meddelande", "Fångsten finns inte längre");
            return "redirect:/karta";
        }

        form.setId(id);
        kontrolleraBild(form, result);
        if (result.hasErrors()) {
            return visaFormularIgen(model, agare, gammal.get());
        }

        String nyBild = sparaBildOmFinns(form);
        Optional<Fangst> uppdaterad;
        try {
            uppdaterad = fangstService.uppdatera(agare, id, form, nyBild);
        } catch (RuntimeException e) {
            // Sparandet misslyckades: behåll den gamla bilden och släng den nyuppladdade
            if (nyBild != null) {
                bildLagring.radera(nyBild);
            }
            throw e;
        }
        if (uppdaterad.isEmpty()) { // fångsten raderades medan formuläret var öppet
            if (nyBild != null) {
                bildLagring.radera(nyBild);
            }
            redirect.addFlashAttribute("meddelande", "Fångsten finns inte längre");
            return "redirect:/karta";
        }
        if (nyBild != null) {
            // Först nu, när databasen pekar på den nya bilden, är den gamla överflödig
            bildLagring.radera(gammal.get().getBildNyckel());
        }

        redirect.addFlashAttribute("meddelande", uppdaterad.get().getArt() + " uppdaterad!");
        redirect.addAttribute("vald", id);
        return "redirect:/karta";
    }

    /** Radera en fångst (och dess bild). */
    @PostMapping("/fangster/{id}/radera")
    public String raderaFangst(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        fangstService.radera(agare(principal), id).ifPresent(f -> {
            bildLagring.radera(f.getBildNyckel());
            redirect.addFlashAttribute("meddelande", f.getArt() + " raderad");
        });
        return "redirect:/karta";
    }

    /** Skickar fångstens bild till webbläsaren (används i <img src="...">). Bara för fångstens ägare. */
    @GetMapping("/fangster/{id}/bild")
    public ResponseEntity<byte[]> bild(@PathVariable Long id, Principal principal) {
        return fangstService.hitta(agare(principal), id)
                .map(Fangst::getBildNyckel)
                .flatMap(bildLagring::hamta)
                .map(b -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(b.contentType()))
                        .cacheControl(CacheControl.noCache())
                        .body(b.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Hjälpmetoder ----

    /** Id för den inloggade användaren. */
    private Long agare(Principal principal) {
        return anvandarService.idFor(principal.getName());
    }

    private void kontrolleraBild(FangstForm form, BindingResult result) {
        MultipartFile bild = form.getBild();
        if (bild != null && !bild.isEmpty()
                && (bild.getContentType() == null || !bild.getContentType().startsWith("image/"))) {
            result.rejectValue("bild", "bild.typ", "Filen måste vara en bild (jpg, png …)");
        }
    }

    private String sparaBildOmFinns(FangstForm form) throws IOException {
        MultipartFile bild = form.getBild();
        return bild == null || bild.isEmpty() ? null : bildLagring.spara(bild);
    }

    /** Visa sidan igen med panelen öppen och felmeddelanden. */
    private String visaFormularIgen(Model model, Long agare, Fangst redigerad) {
        fyllModell(model, agare, null);
        model.addAttribute("formOppet", true);
        model.addAttribute("redigerad", redigerad);
        return "karta";
    }

    private void fyllModell(Model model, Long agare, String q) {
        model.addAttribute("q", q);
        model.addAttribute("fangster", fangstService.sok(agare, q));
    }
}
