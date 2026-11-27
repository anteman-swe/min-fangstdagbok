package org.example.fiskeexperten.model;

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

    /** Samma som VARCHAR(120) i migreringen V1. */
    public static final int MAX_EPOST_LANGD = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = MAX_EPOST_LANGD)
    private String epost;

    @Column(name = "losenord_hash", nullable = false, length = 100)
    private String losenordHash;

    // columnDefinition så att Hibernates schemakontroll (ddl-auto=validate) godtar PostgreSQL:s timestamptz
    @Column(nullable = false, columnDefinition = "timestamptz")
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
