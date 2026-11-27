package org.example.fiskeexperten.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Sparar och hämtar fångstbilder.
 * Just nu finns bara MinnesBildLagring (bilder i minnet).
 * TODO (Trello: "Spara fiskbild i S3"): skapa en S3BildLagring som implementerar samma interface
 * – då behöver inget annat i koden ändras.
 */
public interface BildLagring {

    /** Sparar bilden och returnerar en nyckel som används för att hämta den igen. */
    String spara(MultipartFile fil) throws IOException;

    Optional<Bild> hamta(String nyckel);

    void radera(String nyckel);

    record Bild(byte[] data, String contentType) {
    }
}
