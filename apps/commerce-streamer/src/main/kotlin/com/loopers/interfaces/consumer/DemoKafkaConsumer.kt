package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.annotation.DltHandler
import org.springframework.retry.annotation.Backoff
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class DemoKafkaConsumer {
    @KafkaListener(
        topics = ["\${demo-kafka.test.topic-name}"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    @RetryableTopic(
        attempts = "3",
        backoff = Backoff(delay = 1000L, multiplier = 2.0)
    )
    fun demoListener(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        println(messages)
        acknowledgment.acknowledge() // manual ack
    }

    @DltHandler
    fun demoDlt(
        payload: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.ORIGINAL_TOPIC) originalTopic: String?,
        @Header(KafkaHeaders.ORIGINAL_PARTITION) originalPartition: Int?,
        @Header(KafkaHeaders.ORIGINAL_OFFSET) originalOffset: Long?,
    ) {
        println("DLT message received - topic=$topic, originalTopic=$originalTopic, partition=$originalPartition, offset=$originalOffset, payload=$payload")
    }

    @DltHandler
    fun demoDltBatch(
        records: List<ConsumerRecord<Any, Any>>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
    ) {
        println("DLT batch received - topic=$topic, size=${records.size}")
        if (records.isNotEmpty()) {
            val first = records.first()
            println("First record metadata - topic=${first.topic()}, partition=${first.partition()}, offset=${first.offset()}, key=${first.key()}, value=${first.value()}")
        }
    }
}
