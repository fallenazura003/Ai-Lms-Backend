package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByOrderByTimestampDesc(Pageable pageable);
    List<Event> findAllByOrderByTimestampDesc();
}
