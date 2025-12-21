package com.ewolff.microservice.order.events;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.ewolff.microservice.order.events.OrderEvent.EventType;
import com.ewolff.microservice.order.events.OrderEvent.OrderLineEvent;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Value("${kafka.topic.order-events:order-events}")
    private String orderEventsTopic;
    
    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Autowired
    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(long orderId, long customerId, List<OrderLineEvent> orderLines) {
        if (!kafkaEnabled) {
            log.debug("Kafka is disabled, skipping event publish for order: {}", orderId);
            return;
        }
        
        OrderEvent event = new OrderEvent(orderId, customerId, orderLines, EventType.ORDER_CREATED);
        sendEvent(event);
    }

    public void publishOrderDeleted(long orderId) {
        if (!kafkaEnabled) {
            log.debug("Kafka is disabled, skipping event publish for deleted order: {}", orderId);
            return;
        }
        
        OrderEvent event = new OrderEvent(orderId, null, null, EventType.ORDER_DELETED);
        sendEvent(event);
    }

    private void sendEvent(OrderEvent event) {
        try {
            ListenableFuture<SendResult<String, OrderEvent>> future = 
                kafkaTemplate.send(orderEventsTopic, String.valueOf(event.getOrderId()), event);
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, OrderEvent>>() {
                @Override
                public void onSuccess(SendResult<String, OrderEvent> result) {
                    log.info("Order event sent successfully: orderId={}, type={}, partition={}, offset={}",
                            event.getOrderId(), event.getEventType(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Failed to send order event: orderId={}, type={}, error={}",
                            event.getOrderId(), event.getEventType(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Exception while sending order event: orderId={}, error={}", 
                    event.getOrderId(), e.getMessage());
        }
    }
}
