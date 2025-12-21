package com.ewolff.microservice.catalog.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${kafka.topic.order-events:order-events}", 
                   groupId = "${spring.kafka.consumer.group-id:catalog-service}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: orderId={}, type={}, items={}",
                event.getOrderId(), event.getEventType(), 
                event.getOrderLines() != null ? event.getOrderLines().size() : 0);
        
        switch (event.getEventType()) {
            case ORDER_CREATED:
                handleOrderCreated(event);
                break;
            case ORDER_DELETED:
                handleOrderDeleted(event);
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }
    
    private void handleOrderCreated(OrderEvent event) {
        // Can be used for: inventory deduction, sales statistics, etc.
        log.info("Processing ORDER_CREATED: orderId={}", event.getOrderId());
        if (event.getOrderLines() != null) {
            event.getOrderLines().forEach(line -> 
                log.info("  Item ordered: itemId={}, count={}", line.getItemId(), line.getCount())
            );
        }
    }
    
    private void handleOrderDeleted(OrderEvent event) {
        // Can be used for: inventory restoration, etc.
        log.info("Processing ORDER_DELETED: orderId={}", event.getOrderId());
    }
}
