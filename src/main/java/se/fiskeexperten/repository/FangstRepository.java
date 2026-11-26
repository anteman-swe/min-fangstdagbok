package se.fiskeexperten.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.fiskeexperten.model.Fangst;

import java.util.List;
import java.util.Optional;

public interface FangstRepository extends JpaRepository<Fangst, Long> {

    /** Hämtar bara om fångsten tillhör den angivna användaren. */
    Optional<Fangst> findByIdAndAgareId(Long id, Long agareId);

    /**
     * Användarens fångster, nyast först. Sökordet (gemener) matchas mot art och plats.
     * locate() letar efter en bokstavlig delsträng, så % och _ i sökordet blir inga jokertecken,
     * och ett tomt sökord matchar allt.
     */
    @Query("""
            select f from Fangst f
            where f.agareId = :agareId
              and (locate(:sokord, lower(f.art)) > 0 or locate(:sokord, lower(f.plats)) > 0)
            order by f.datum desc, f.id desc
            """)
    List<Fangst> sok(@Param("agareId") Long agareId, @Param("sokord") String sokord);
}
