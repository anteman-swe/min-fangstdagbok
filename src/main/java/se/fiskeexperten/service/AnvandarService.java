package se.fiskeexperten.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.fiskeexperten.model.Anvandare;
import se.fiskeexperten.repository.AnvandareRepository;

import java.util.Locale;

/**
 * Användare i databasen. Spring Security använder den här klassen för att slå upp
 * den som loggar in (UserDetailsService); e-postadressen är användarnamnet.
 */
@Service
public class AnvandarService implements UserDetailsService {

    private final AnvandareRepository repository;

    public AnvandarService(AnvandareRepository repository) {
        this.repository = repository;
    }

    /** Gemener och utan blanksteg, så att "Anna@Exempel.se " och "anna@exempel.se" blir samma konto. */
    public static String normalisera(String epost) {
        return epost == null ? "" : epost.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String epost) throws UsernameNotFoundException {
        Anvandare a = repository.findByEpost(normalisera(epost))
                .orElseThrow(() -> new UsernameNotFoundException("Okänd användare"));
        return User.withUsername(a.getEpost())
                .password(a.getLosenordHash())
                .roles("USER")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean finns(String epost) {
        return repository.existsByEpost(normalisera(epost));
    }

    /** Skapar kontot. losenordHash ska redan vara hashat (PasswordEncoder). */
    @Transactional
    public Anvandare skapa(String epost, String losenordHash) {
        return repository.saveAndFlush(new Anvandare(normalisera(epost), losenordHash));
    }

    /** Id för den inloggade användaren (Principal.getName() är e-postadressen). */
    @Transactional(readOnly = true)
    public Long idFor(String epost) {
        return repository.findByEpost(normalisera(epost))
                .map(Anvandare::getId)
                .orElseThrow(() -> new IllegalStateException("Inloggad användare saknas i databasen"));
    }
}
