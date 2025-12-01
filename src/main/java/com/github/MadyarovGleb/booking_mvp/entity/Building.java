package com.github.MadyarovGleb.booking_mvp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "buildings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Building {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @PrePersist
    public void pre() {
        if (id == null) id = UUID.randomUUID();
    }
}
