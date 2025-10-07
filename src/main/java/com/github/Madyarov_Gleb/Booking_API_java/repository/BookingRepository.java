package com.github.Madyarov_Gleb.Booking_API_java.repository;

import com.github.Madyarov_Gleb.Booking_API_java.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {
}
