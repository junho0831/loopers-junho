package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoKafkaConsumer {

    @KafkaListener(
            topics = "${demo-kafka.test.topic-name}",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000L, multiplier = 2.0)
    )
    public void demoListener(
            List<ConsumerRecord<?, ?>> messages,
            Acknowledgment acknowledgment
    ) {
        System.out.println(messages);
        acknowledgment.acknowledge(); // manual ack
    }

    @DltHandler
    public void demoDlt(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.ORIGINAL_PARTITION) Integer originalPartition,
            @Header(KafkaHeaders.ORIGINAL_OFFSET) Long originalOffset
    ) {
        System.out.printf(
                "DLT message received - topic=%s, originalTopic=%s, partition=%s, offset=%s, payload=%s%n",
                topic, originalTopic, String.valueOf(originalPartition), String.valueOf(originalOffset), payload
        );
    }

    @DltHandler
    public void demoDltBatch(
            List<ConsumerRecord<?, ?>> records,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        System.out.printf("DLT batch received - topic=%s, size=%d%n", topic, records != null ? records.size() : 0);
        if (records != null && !records.isEmpty()) {
            ConsumerRecord<?, ?> first = records.get(0);
            System.out.printf(
                    "First record metadata - topic=%s, partition=%d, offset=%d, key=%s, value=%s%n",
                    first.topic(), first.partition(), first.offset(), String.valueOf(first.key()), String.valueOf(first.value())
            );
        }
    }
}

