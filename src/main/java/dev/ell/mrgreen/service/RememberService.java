package dev.ell.mrgreen.service;

import dev.ell.mrgreen.entity.RememberedEntry;
import dev.ell.mrgreen.repository.RememberedEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RememberService {

    private final RememberedEntryRepository repository;

    public RememberService(RememberedEntryRepository repository) {
        this.repository = repository;
    }

    public RememberedEntry addEntry(String guildId, String key, String value, String createdBy) {
        var entry = new RememberedEntry(key, value, guildId, createdBy, Instant.now());
        return repository.save(entry);
    }

    public List<RememberedEntry> getEntries(String guildId, String key) {
        return repository.findByGuildIdAndKeyOrderByCreatedAtAsc(guildId, key);
    }

    public Optional<RememberedEntry> getEntry(String guildId, String key, int oneBasedIndex) {
        var entries = getEntries(guildId, key);
        var idx = oneBasedIndex - 1;
        if (idx < 0 || idx >= entries.size()) return Optional.empty();
        return Optional.of(entries.get(idx));
    }

    @Transactional
    public Optional<RememberedEntry> deleteEntry(String guildId, String key, int oneBasedIndex) {
        var entries = getEntries(guildId, key);
        var idx = oneBasedIndex - 1;
        if (idx < 0 || idx >= entries.size()) return Optional.empty();
        var entry = entries.get(idx);
        repository.delete(entry);
        return Optional.of(entry);
    }

    @Transactional
    public int deleteAllEntries(String guildId, String key) {
        var entries = getEntries(guildId, key);
        if (entries.isEmpty()) return 0;
        repository.deleteByGuildIdAndKey(guildId, key);
        return entries.size();
    }
}
