package com.ewolff.microservice.order.logic;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ewolff.microservice.order.clients.CatalogClient;
import com.ewolff.microservice.order.clients.CustomerClient;
import com.ewolff.microservice.order.events.OrderEventPublisher;
import com.ewolff.microservice.order.events.OrderEvent.OrderLineEvent;

@Service
class OrderService {

	private OrderRepository orderRepository;
	private CustomerClient customerClient;
	private CatalogClient itemClient;
	private OrderEventPublisher orderEventPublisher;

	@Autowired
	private OrderService(OrderRepository orderRepository,
			CustomerClient customerClient, CatalogClient itemClient,
			@Autowired(required = false) OrderEventPublisher orderEventPublisher) {
		super();
		this.orderRepository = orderRepository;
		this.customerClient = customerClient;
		this.itemClient = itemClient;
		this.orderEventPublisher = orderEventPublisher;
	}

	public Order order(Order order) {
		if (order.getNumberOfLines() == 0) {
			throw new IllegalArgumentException("No order lines!");
		}
		if (!customerClient.isValidCustomerId(order.getCustomerId())) {
			throw new IllegalArgumentException("Customer does not exist!");
		}
		Order savedOrder = orderRepository.save(order);
		
		// Publish order created event
		if (orderEventPublisher != null) {
			orderEventPublisher.publishOrderCreated(
				savedOrder.getId(),
				savedOrder.getCustomerId(),
				savedOrder.getOrderLine().stream()
					.map(line -> new OrderLineEvent(line.getItemId(), line.getCount()))
					.collect(Collectors.toList())
			);
		}
		
		return savedOrder;
	}

	public double getPrice(long orderId) {
		return orderRepository.findById(orderId).get().totalPrice(itemClient);
	}
	
	public void deleteOrder(long orderId) {
		orderRepository.deleteById(orderId);
		// Publish order deleted event
		if (orderEventPublisher != null) {
			orderEventPublisher.publishOrderDeleted(orderId);
		}
	}

}
