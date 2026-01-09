package com.ewolff.microservice.order.events;

import java.time.LocalDateTime;
import java.util.List;

public class OrderEvent {
    
    public enum EventType {
        ORDER_CREATED,
        ORDER_DELETED,
        ORDER_UPDATED
    }
    
    private Long orderId;
    private Long customerId;
    private List<OrderLineEvent> orderLines;
    private EventType eventType;
    private LocalDateTime timestamp;
    
    public OrderEvent() {
    }
    
    public OrderEvent(Long orderId, Long customerId, List<OrderLineEvent> orderLines, EventType eventType) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderLines = orderLines;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public List<OrderLineEvent> getOrderLines() { return orderLines; }
    public void setOrderLines(List<OrderLineEvent> orderLines) { this.orderLines = orderLines; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public static class OrderLineEvent {
        private Long itemId;
        private int count;
        
        public OrderLineEvent() {}
        
        public OrderLineEvent(Long itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
        
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
