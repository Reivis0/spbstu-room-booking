package com.github.MadyarovGleb.booking_mvp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;
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
    private String features;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "university_code")
    private String universityCode;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (features == null) features = "{}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Room(id=" + id + ", name=" + name + ")";
    }
}
