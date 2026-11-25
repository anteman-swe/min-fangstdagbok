package se.fiskeexperten.service;

import org.springframework.stereotype.Service;
import se.fiskeexperten.model.Fangst;
import se.fiskeexperten.web.FangstForm;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Håller fångsterna i minnet (försvinner vid omstart).
 * Byts mot ett JPA-repository + PostGIS när databasen är uppsatt.
 */
@Service
public class FangstService {

    private final Map<Long, Fangst> fangster = new ConcurrentHashMap<>();
    private final AtomicLong nastaId = new AtomicLong(1);

    public FangstService() {
        // Exempeldata – samma fångster som i Lovable-versionen, men med riktiga koordinater
        lagg("Gädda", 4.2, 82, "Mälaren, Västerås", LocalDate.of(2026, 11, 12), 59.585, 16.585);
        lagg("Abborre", 0.9, 38, "Vättern, Hjo", LocalDate.of(2026, 11, 8), 58.305, 14.245);
        lagg("Gös", 2.7, 64, "Hjälmaren", LocalDate.of(2026, 11, 2), 59.230, 15.700);
        lagg("Öring", 1.6, 51, "Klarälven", LocalDate.of(2026, 10, 28), 60.170, 13.470);
    }

    /** Alla fångster, nyast först. Filtrerar på art eller plats om sökord finns. */
    public List<Fangst> sok(String sokord) {
        String q = sokord == null ? "" : sokord.trim().toLowerCase(Locale.ROOT);
        return fangster.values().stream()
                .filter(f -> q.isEmpty()
                        || f.getArt().toLowerCase(Locale.ROOT).contains(q)
                        || f.getPlats().toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing(Fangst::getDatum).reversed()
                        .thenComparing(Fangst::getId, Comparator.reverseOrder()))
                .toList();
    }

    public Optional<Fangst> hitta(Long id) {
        return Optional.ofNullable(fangster.get(id));
    }

    /** Ny fångst. bildNyckel får vara null. */
    public Fangst spara(FangstForm form, String bildNyckel) {
        Fangst f = franForm(nastaId.getAndIncrement(), form, bildNyckel);
        fangster.put(f.getId(), f);
        return f;
    }

    /** Uppdaterar en fångst. Är bildNyckel null behålls den gamla bilden. */
    public Optional<Fangst> uppdatera(Long id, FangstForm form, String bildNyckel) {
        Fangst gammal = fangster.get(id);
        if (gammal == null) {
            return Optional.empty();
        }
        Fangst ny = franForm(id, form, bildNyckel != null ? bildNyckel : gammal.getBildNyckel());
        fangster.put(id, ny);
        return Optional.of(ny);
    }

    /** Tar bort fångsten och returnerar den (så att bilden kan raderas). */
    public Optional<Fangst> radera(Long id) {
        return Optional.ofNullable(fangster.remove(id));
    }

    private Fangst franForm(Long id, FangstForm form, String bildNyckel) {
        String plats = form.getPlats() == null || form.getPlats().isBlank()
                ? String.format(Locale.ROOT, "%.4f, %.4f", form.getLat(), form.getLng())
                : form.getPlats().trim();
        return new Fangst(id, form.getArt().trim(), form.getVikt(), form.getLangd(), plats,
                form.getDatum(), form.getLat(), form.getLng(), bildNyckel);
    }

    private void lagg(String art, Double vikt, Integer langd, String plats,
                      LocalDate datum, double lat, double lng) {
        long id = nastaId.getAndIncrement();
        fangster.put(id, new Fangst(id, art, vikt, langd, plats, datum, lat, lng, null));
    }
}
