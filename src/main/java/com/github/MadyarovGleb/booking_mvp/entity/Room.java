package com.github.MadyarovGleb.booking_mvp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Integer capacity;

    @Column(columnDefinition = "jsonb")
    private String features; // например: '{"projector":true,"whiteboard":true}'

    @PrePersist
    public void pre() {
        if (id == null) id = UUID.randomUUID();
        if (features == null) features = "{}";
    }
}

