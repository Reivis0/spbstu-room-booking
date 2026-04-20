package com.github.MadyarovGleb.booking_mvp.repository;

import com.github.MadyarovGleb.booking_mvp.entity.SchedulesImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SchedulesImportRepository extends JpaRepository<SchedulesImport, UUID> {
    @Query("select s from SchedulesImport s where s.roomId = :roomId and s.startsAt >= :from and s.startsAt < :to")
    List<SchedulesImport> findByRoomIdAndDate(@Param("roomId") UUID roomId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
