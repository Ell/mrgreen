package dev.ell.mrgreen.repository;

import dev.ell.mrgreen.entity.RememberedEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RememberedEntryRepository extends JpaRepository<RememberedEntry, Long> {
    List<RememberedEntry> findByGuildIdAndKeyOrderByCreatedAtAsc(String guildId, String key);
    void deleteByGuildIdAndKey(String guildId, String key);
}
