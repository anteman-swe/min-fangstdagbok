package se.fiskeexperten.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** En användare, sparad i tabellen "anvandare". Lösenordet lagras bara som hash. */
@Entity
@Table(name = "anvandare")
public class Anvandare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String epost;

    @Column(name = "losenord_hash", nullable = false, length = 100)
    private String losenordHash;

    @Column(nullable = false)
    private Instant skapad;

    /** Krävs av JPA. */
    protected Anvandare() {
    }

    public Anvandare(String epost, String losenordHash) {
        this.epost = epost;
        this.losenordHash = losenordHash;
        this.skapad = Instant.now();
    }

    public Long getId() { return id; }
    public String getEpost() { return epost; }
    public String getLosenordHash() { return losenordHash; }
    public Instant getSkapad() { return skapad; }
}
