package com.ecommerce.infrastructure.kafka.producer;

import com.ecommerce.domain.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformKafkaProducer {

    private static final String ORDER_EVENT_CREATED_TOPIC = "ecommerce.order.event.created.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderEvent(OrderCreatedEvent event) {
        send(ORDER_EVENT_CREATED_TOPIC, event.orderId().toString(), event);
    }

    private <T> void send(String topic, String key, T event) {
        log.info("Kafka 메시지 발행: topic={}, key={}", topic, key);
        kafkaTemplate.send(topic, key, event);
    }
}
