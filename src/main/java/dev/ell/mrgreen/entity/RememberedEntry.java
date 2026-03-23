package dev.ell.mrgreen.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "remembered_entries")
public class RememberedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(name = "\"key\"", nullable = false)
    private String key;

    @Column(name = "\"value\"", nullable = false)
    private String value;

    @Column(name = "guild_id", nullable = false)
    private String guildId;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, columnDefinition = "text")
    private Instant createdAt;

    protected RememberedEntry() {}

    public RememberedEntry(String key, String value, String guildId, String createdBy, Instant createdAt) {
        this.key = key;
        this.value = value;
        this.guildId = guildId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getGuildId() { return guildId; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
