package com.github.MadyarovGleb.booking_mvp.repository;

import com.github.MadyarovGleb.booking_mvp.entity.SagaCompensationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaCompensationOutboxRepository extends JpaRepository<SagaCompensationOutbox, UUID> {
    List<SagaCompensationOutbox> findByStatus(String status);
}
