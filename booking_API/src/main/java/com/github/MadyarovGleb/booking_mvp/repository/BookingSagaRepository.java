package com.github.MadyarovGleb.booking_mvp.repository;

import com.github.MadyarovGleb.booking_mvp.entity.BookingSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingSagaRepository extends JpaRepository<BookingSaga, UUID> {

    Optional<BookingSaga> findByBookingId(UUID bookingId);

    @Query(value = """
            SELECT *
            FROM booking_saga
            WHERE state IN ('PENDING', 'PROCESSING')
              AND next_retry_at <= now()
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<BookingSaga> lockDueForProcessing(@Param("batchSize") int batchSize);
}
