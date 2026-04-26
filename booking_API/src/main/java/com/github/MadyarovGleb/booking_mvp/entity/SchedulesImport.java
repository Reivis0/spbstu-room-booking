package com.github.MadyarovGleb.booking_mvp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "schedules_import")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchedulesImport {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String hash;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (metadata == null) metadata = "{}";
    }
}
