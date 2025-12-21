package com.ewolff.microservice.order.clients;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class CustomerClient {

	private final Logger log = LoggerFactory.getLogger(CustomerClient.class);

	private RestTemplate restTemplate;
	private String customerServiceHost;
	private long customerServicePort;

	static class CustomerPagedResources extends PagedModel<Customer> {

	}

	@Autowired
	public CustomerClient(@Value("${customer.service.host:customer}") String customerServiceHost,
			@Value("${customer.service.port:8080}") long customerServicePort) {
		super();
		this.restTemplate = getRestTemplate();
		this.customerServiceHost = customerServiceHost;
		this.customerServicePort = customerServicePort;
	}

	@CircuitBreaker(name = "customerService", fallbackMethod = "isValidCustomerIdFallback")
	@Retry(name = "customerService")
	public boolean isValidCustomerId(long customerId) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> entity = restTemplate.getForEntity(customerURL() + customerId, String.class);
			return entity.getStatusCode().is2xxSuccessful();
		} catch (final HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404)
				return false;
			else
				throw e;
		}
	}

	public boolean isValidCustomerIdFallback(long customerId, Throwable t) {
		log.warn("Customer service unavailable for validation, assuming customer {} is valid. Error: {}", customerId, t.getMessage());
		// 降级时假设客户有效，避免阻塞订单流程
		return true;
	}

	protected RestTemplate getRestTemplate() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
		converter.setObjectMapper(mapper);

		return new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
	}

	@CircuitBreaker(name = "customerService", fallbackMethod = "findAllFallback")
	@Retry(name = "customerService")
	public Collection<Customer> findAll() {
		PagedModel<Customer> pagedResources = getRestTemplate().getForObject(customerURL(),
				CustomerPagedResources.class);
		return pagedResources.getContent();
	}

	public Collection<Customer> findAllFallback(Throwable t) {
		log.warn("Customer service unavailable, returning empty list. Error: {}", t.getMessage());
		return Collections.emptyList();
	}

	private String customerURL() {
		String url = String.format("http://%s:%s/customer/", customerServiceHost, customerServicePort);
		log.trace("Customer: URL {} ", url);
		return url;

	}

	@CircuitBreaker(name = "customerService", fallbackMethod = "getOneFallback")
	@Retry(name = "customerService")
	public Customer getOne(long customerId) {
		return restTemplate.getForObject(customerURL() + customerId, Customer.class);
	}

	public Customer getOneFallback(long customerId, Throwable t) {
		log.warn("Customer service unavailable, returning fallback customer for id: {}. Error: {}", customerId, t.getMessage());
		return new Customer(customerId, "未知", "客户", "unknown@example.com", "未知地址", "未知城市");
	}
}
