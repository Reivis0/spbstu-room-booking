package com.github.MadyarovGleb.booking_mvp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.pending;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum BookingStatus { pending, confirmed, cancelled, rejected }

    @PrePersist
    public void pre() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { updatedAt = OffsetDateTime.now(); }
}

