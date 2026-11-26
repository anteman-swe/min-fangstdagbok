package se.fiskeexperten.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.fiskeexperten.model.Anvandare;

import java.util.Optional;

public interface AnvandareRepository extends JpaRepository<Anvandare, Long> {

    Optional<Anvandare> findByEpost(String epost);

    boolean existsByEpost(String epost);
}
