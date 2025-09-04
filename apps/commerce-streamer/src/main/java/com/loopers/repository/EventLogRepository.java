package com.loopers.repository;

import com.loopers.domain.event.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.ZonedDateTime;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    boolean existsByEventId(String eventId);
    
    EventLog findByEventIdAndEventType(String eventId, String eventType);
    
    List<EventLog> findByEventTypeAndProcessedAtBetween(
        String eventType,
        ZonedDateTime startTime,
        ZonedDateTime endTime
    );
}