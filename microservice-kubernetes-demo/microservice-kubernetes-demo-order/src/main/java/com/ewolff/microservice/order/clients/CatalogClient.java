package com.ewolff.microservice.order.clients;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class CatalogClient {

	private final Logger log = LoggerFactory.getLogger(CatalogClient.class);

	public static class ItemPagedResources extends PagedModel<Item> {

	}

	private RestTemplate restTemplate;
	private String catalogServiceHost;
	private long catalogServicePort;

	@Autowired
	public CatalogClient(@Value("${catalog.service.host:catalog}") String catalogServiceHost,
			@Value("${catalog.service.port:8080}") long catalogServicePort) {
		super();
		this.restTemplate = getRestTemplate();
		this.catalogServiceHost = catalogServiceHost;
		this.catalogServicePort = catalogServicePort;
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

	@CircuitBreaker(name = "catalogService", fallbackMethod = "priceFallback")
	@Retry(name = "catalogService")
	public double price(long itemId) {
		return getOne(itemId).getPrice();
	}

	public double priceFallback(long itemId, Throwable t) {
		log.warn("Catalog service unavailable, returning default price for item: {}. Error: {}", itemId, t.getMessage());
		return 0.0;
	}

	@Cacheable(value = "itemsList", unless = "#result == null || #result.isEmpty()")
	@CircuitBreaker(name = "catalogService", fallbackMethod = "findAllFallback")
	@Retry(name = "catalogService")
	public Collection<Item> findAll() {
		log.info("Fetching all items from Catalog service (cache miss)");
		PagedModel<Item> pagedResources = restTemplate.getForObject(catalogURL(), ItemPagedResources.class);
		return pagedResources.getContent();
	}

	public Collection<Item> findAllFallback(Throwable t) {
		log.warn("Catalog service unavailable, returning empty list. Error: {}", t.getMessage());
		return Collections.emptyList();
	}

	private String catalogURL() {
		String url = String.format("http://%s:%s/catalog/", catalogServiceHost, catalogServicePort);
		log.trace("Catalog: URL {} ", url);
		return url;
	}

	@Cacheable(value = "items", key = "#itemId", unless = "#result == null")
	@CircuitBreaker(name = "catalogService", fallbackMethod = "getOneFallback")
	@Retry(name = "catalogService")
	public Item getOne(long itemId) {
		log.info("Fetching item {} from Catalog service (cache miss)", itemId);
		return restTemplate.getForObject(catalogURL() + itemId, Item.class);
	}

	public Item getOneFallback(long itemId, Throwable t) {
		log.warn("Catalog service unavailable, returning fallback item for id: {}. Error: {}", itemId, t.getMessage());
		return new Item(itemId, "Item Unavailable", 0.0);
	}

	// Scheduled cache cleanup (every hour)
	@Scheduled(fixedRate = 3600000)
	@CacheEvict(value = {"items", "itemsList"}, allEntries = true)
	public void evictAllItemsCache() {
		log.info("Evicting all items cache");
	}
}
