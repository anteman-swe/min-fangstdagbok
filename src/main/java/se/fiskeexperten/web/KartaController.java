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
import se.fiskeexperten.service.BildLagring;
import se.fiskeexperten.service.FangstService;

import java.io.IOException;
import java.util.Optional;

@Controller
public class KartaController {

    private final FangstService fangstService;
    private final BildLagring bildLagring;

    public KartaController(FangstService fangstService, BildLagring bildLagring) {
        this.fangstService = fangstService;
        this.bildLagring = bildLagring;
    }

    /**
     * Kartan + listan med fångster.
     * ?q=      söker på art/plats
     * ?vald=   öppnar en fångst på kartan
     * ?redigera= öppnar panelen med en fångst ifylld
     */
    @GetMapping("/karta")
    public String karta(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Long vald,
                        @RequestParam(required = false) Long redigera,
                        Model model) {
        Optional<Fangst> attRedigera = redigera == null ? Optional.empty() : fangstService.hitta(redigera);

        if (attRedigera.isPresent()) {
            model.addAttribute("fangstForm", FangstForm.fran(attRedigera.get()));
            model.addAttribute("redigerad", attRedigera.get());
            model.addAttribute("formOppet", true);
        } else {
            model.addAttribute("fangstForm", FangstForm.ny());
        }
        model.addAttribute("vald", vald);
        fyllModell(model, q);
        return "karta";
    }

    /** Ny fångst. */
    @PostMapping("/fangster")
    public String sparaFangst(@Valid @ModelAttribute("fangstForm") FangstForm form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirect) throws IOException {
        form.setId(null);
        kontrolleraBild(form, result);
        if (result.hasErrors()) {
            return visaFormularIgen(model, null);
        }

        // TODO (Trello: "Att kolla fiskart från bilden med AI"): skicka bilden till Spring AI
        String bildNyckel = sparaBildOmFinns(form);
        Fangst ny = fangstService.spara(form, bildNyckel);

        redirect.addFlashAttribute("meddelande", ny.getArt() + " sparad!");
        redirect.addAttribute("vald", ny.getId());
        return "redirect:/karta";
    }

    /** Spara ändringar på en befintlig fångst. */
    @PostMapping("/fangster/{id}")
    public String uppdateraFangst(@PathVariable Long id,
                                  @Valid @ModelAttribute("fangstForm") FangstForm form,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirect) throws IOException {
        Optional<Fangst> gammal = fangstService.hitta(id);
        if (gammal.isEmpty()) {
            redirect.addFlashAttribute("meddelande", "Fångsten finns inte längre");
            return "redirect:/karta";
        }

        form.setId(id);
        kontrolleraBild(form, result);
        if (result.hasErrors()) {
            return visaFormularIgen(model, gammal.get());
        }

        String nyBild = sparaBildOmFinns(form);
        if (nyBild != null) {
            bildLagring.radera(gammal.get().getBildNyckel()); // ta bort den gamla bilden
        }
        Fangst uppdaterad = fangstService.uppdatera(id, form, nyBild).orElseThrow();

        redirect.addFlashAttribute("meddelande", uppdaterad.getArt() + " uppdaterad!");
        redirect.addAttribute("vald", id);
        return "redirect:/karta";
    }

    /** Radera en fångst (och dess bild). */
    @PostMapping("/fangster/{id}/radera")
    public String raderaFangst(@PathVariable Long id, RedirectAttributes redirect) {
        fangstService.radera(id).ifPresent(f -> {
            bildLagring.radera(f.getBildNyckel());
            redirect.addFlashAttribute("meddelande", f.getArt() + " raderad");
        });
        return "redirect:/karta";
    }

    /** Skickar fångstens bild till webbläsaren (används i <img src="...">). */
    @GetMapping("/fangster/{id}/bild")
    public ResponseEntity<byte[]> bild(@PathVariable Long id) {
        return fangstService.hitta(id)
                .map(Fangst::getBildNyckel)
                .flatMap(bildLagring::hamta)
                .map(b -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(b.contentType()))
                        .cacheControl(CacheControl.noCache())
                        .body(b.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Hjälpmetoder ----

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
    private String visaFormularIgen(Model model, Fangst redigerad) {
        fyllModell(model, null);
        model.addAttribute("formOppet", true);
        model.addAttribute("redigerad", redigerad);
        return "karta";
    }

    private void fyllModell(Model model, String q) {
        model.addAttribute("q", q);
        model.addAttribute("fangster", fangstService.sok(q));
    }
}
