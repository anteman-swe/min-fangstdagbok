package se.fiskeexperten.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Håller bilderna i minnet – de försvinner när appen startas om. Ersätts av S3 senare. */
@Service
public class MinnesBildLagring implements BildLagring {

    private final Map<String, Bild> bilder = new ConcurrentHashMap<>();

    @Override
    public String spara(MultipartFile fil) throws IOException {
        String nyckel = UUID.randomUUID().toString();
        bilder.put(nyckel, new Bild(fil.getBytes(), fil.getContentType()));
        return nyckel;
    }

    @Override
    public Optional<Bild> hamta(String nyckel) {
        return Optional.ofNullable(bilder.get(nyckel));
    }

    @Override
    public void radera(String nyckel) {
        if (nyckel != null) {
            bilder.remove(nyckel);
        }
    }
}
