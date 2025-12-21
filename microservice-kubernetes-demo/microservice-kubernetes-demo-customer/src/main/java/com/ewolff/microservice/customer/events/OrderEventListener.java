package com.ewolff.microservice.customer.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${kafka.topic.order-events:order-events}", 
                   groupId = "${spring.kafka.consumer.group-id:customer-service}",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: orderId={}, customerId={}, type={}",
                event.getOrderId(), event.getCustomerId(), event.getEventType());
        
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
        // 可以在这里实现：更新客户订单统计、发送通知等
        log.info("Processing ORDER_CREATED for customer: {}", event.getCustomerId());
    }
    
    private void handleOrderDeleted(OrderEvent event) {
        log.info("Processing ORDER_DELETED for order: {}", event.getOrderId());
    }
}
