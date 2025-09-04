package com.loopers.repository;

import com.loopers.domain.event.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHandledRepository extends JpaRepository<EventHandled, Long> {
    boolean existsByEventIdAndConsumerType(String eventId, EventHandled.ConsumerType consumerType);
    
    EventHandled findByEventIdAndConsumerType(String eventId, EventHandled.ConsumerType consumerType);
}