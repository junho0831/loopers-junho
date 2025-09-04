package com.loopers.service;

import com.loopers.domain.event.EventHandled;
import com.loopers.repository.EventHandledRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;

/**
 * 멱등성 이벤트 처리를 담당하는 서비스
 */
@Service
public class IdempotentEventService {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotentEventService.class);
    
    private final EventHandledRepository eventHandledRepository;

    public IdempotentEventService(EventHandledRepository eventHandledRepository) {
        this.eventHandledRepository = eventHandledRepository;
    }

    /**
     * 해당 컨슈머 타입에서 이벤트가 이미 처리되었는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isEventAlreadyHandled(String eventId, EventHandled.ConsumerType consumerType) {
        return eventHandledRepository.existsByEventIdAndConsumerType(eventId, consumerType);
    }

    /**
     * 해당 컨슈머 타입에서 이벤트를 처리됨으로 표시
     * 성공적으로 표시되면 true 반환 (첫 번째 처리)
     * 이미 표시되어 있으면 false 반환 (중복 처리 시도)
     */
    @Transactional
    public boolean markEventAsHandled(String eventId, EventHandled.ConsumerType consumerType, Long version) {
        if (isEventAlreadyHandled(eventId, consumerType)) {
            log.warn("Event {} already handled by consumer {}", eventId, consumerType);
            return false;
        }

        try {
            EventHandled eventHandled = new EventHandled(
                eventId,
                consumerType,
                ZonedDateTime.now(),
                version
            );
            eventHandledRepository.save(eventHandled);
            log.debug("Event {} marked as handled by consumer {}", eventId, consumerType);
            return true;
        } catch (Exception e) {
            // 다른 스레드가 같은 레코드를 삽입했을 수 있는 경합 상황 처리
            log.warn("Failed to mark event {} as handled by consumer {}: {}", eventId, consumerType, e.getMessage());
            return false;
        }
    }

    /**
     * 멱등성을 보장하며 이벤트 처리
     * 이벤트가 처리되면 true, 이미 처리된 경우 false 반환
     */
    @Transactional
    public boolean processEventIdempotent(String eventId, EventHandled.ConsumerType consumerType, 
                                        Long version, Runnable processor) {
        if (!markEventAsHandled(eventId, consumerType, version)) {
            return false;  // 이미 처리됨
        }

        try {
            processor.run();
            return true;
        } catch (Exception e) {
            log.error("Error processing event {} for consumer {}", eventId, consumerType, e);
            throw e;
        }
    }
}