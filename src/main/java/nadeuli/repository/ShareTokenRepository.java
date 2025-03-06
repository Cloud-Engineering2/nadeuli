package nadeuli.repository;

import nadeuli.entity.ShareToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareTokenRepository extends JpaRepository<ShareToken, Long> {
    Optional<ShareToken> findByItineraryId(Long itineraryId);
    Optional<ShareToken> findByUuid(String uuid);
    boolean existsByItineraryId(Long itineraryId);
}