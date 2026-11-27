package org.example.fiskeexperten.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.fiskeexperten.model.Fangst;
import org.example.fiskeexperten.repository.FangstRepository;
import org.example.fiskeexperten.controller.FangstForm;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Fångster i databasen (PostgreSQL/PostGIS). Varje fångst tillhör en användare:
 * alla metoder tar emot ägarens id, så ingen kan se eller ändra någon annans fångster.
 */
@Service
public class FangstService {

    private final FangstRepository repository;

    public FangstService(FangstRepository repository) {
        this.repository = repository;
    }

    /** Användarens fångster, nyast först. Filtrerar på art eller plats om sökord finns. */
    @Transactional(readOnly = true)
    public List<Fangst> sok(Long agareId, String sokord) {
        String q = sokord == null ? "" : sokord.trim().toLowerCase(Locale.ROOT);
        return repository.sok(agareId, q);
    }

    @Transactional(readOnly = true)
    public Optional<Fangst> hitta(Long agareId, Long id) {
        return repository.findByIdAndAgareId(id, agareId);
    }

    /** Ny fångst. bildNyckel får vara null. */
    @Transactional
    public Fangst spara(Long agareId, FangstForm form, String bildNyckel) {
        return repository.save(franForm(null, agareId, form, bildNyckel));
    }

    /** Uppdaterar en fångst. Är bildNyckel null behålls den gamla bilden. */
    @Transactional
    public Optional<Fangst> uppdatera(Long agareId, Long id, FangstForm form, String bildNyckel) {
        return repository.findByIdAndAgareId(id, agareId).map(gammal -> {
            String nyckel = bildNyckel != null ? bildNyckel : gammal.getBildNyckel();
            // save() med befintligt id uppdaterar raden
            return repository.save(franForm(id, agareId, form, nyckel));
        });
    }

    /** Tar bort fångsten och returnerar den (så att bilden kan raderas). */
    @Transactional
    public Optional<Fangst> radera(Long agareId, Long id) {
        Optional<Fangst> fangst = repository.findByIdAndAgareId(id, agareId);
        fangst.ifPresent(repository::delete);
        return fangst;
    }

    private Fangst franForm(Long id, Long agareId, FangstForm form, String bildNyckel) {
        String plats = form.getPlats() == null || form.getPlats().isBlank()
                ? String.format(Locale.ROOT, "%.4f, %.4f", form.getLat(), form.getLng())
                : form.getPlats().trim();
        return new Fangst(id, agareId, form.getArt().trim(), form.getVikt(), form.getLangd(), plats,
                form.getDatum(), form.getLat(), form.getLng(), bildNyckel);
    }
}
