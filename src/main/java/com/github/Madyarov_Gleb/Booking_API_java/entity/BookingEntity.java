package com.github.Madyarov_Gleb.Booking_API_java.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String roomId;
    private String userId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String purpose;
    private String note;

    @Enumerated(EnumType.STRING)
    private Status status;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public enum Status { PENDING, CONFIRMED, CANCELLED }
}
