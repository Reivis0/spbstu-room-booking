package com.github.MadyarovGleb.booking_mvp.repository;

import com.github.MadyarovGleb.booking_mvp.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
}

