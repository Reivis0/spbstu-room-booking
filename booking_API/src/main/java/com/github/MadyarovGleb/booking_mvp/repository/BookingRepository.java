package com.github.MadyarovGleb.booking_mvp.repository;

import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("select b from Booking b where b.roomId = :roomId and b.status in ('pending','confirmed') and not (b.endsAt <= :startsAt or b.startsAt >= :endsAt)")
    List<Booking> findConflicts(@Param("roomId") UUID roomId, @Param("startsAt") OffsetDateTime startsAt, @Param("endsAt") OffsetDateTime endsAt);

    List<Booking> findByUserId(UUID userId);

    @Query("select count(b) from Booking b where b.userId = :userId and b.status in ('pending','confirmed') and b.startsAt >= :from")
    long countActiveByUserId(@Param("userId") UUID userId, @Param("from") OffsetDateTime from);
}
