package com.tispace.queryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
	
	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	
	public <T> T get(String key, Class<T> type) {
		if (key == null || key.isEmpty()) {
			log.warn("Attempted to get cache with null or empty key");
			return null;
		}
		
		try {
			String value = redisTemplate.opsForValue().get(key);
			if (value == null) {
				return null;
			}
			return objectMapper.readValue(value, type);
		} catch (org.springframework.data.redis.RedisConnectionFailureException e) {
			log.warn("Redis connection failure while getting key: {}. Returning null (cache miss)", key);
			return null; // Treat as cache miss when Redis is unavailable - cache is optional
		} catch (JsonProcessingException e) {
			log.warn("Error deserializing cached value for key: {}. Returning null (cache miss)", key);
			return null; // Treat serialization errors as cache miss - cache is optional
		} catch (Exception e) {
			log.warn("Unexpected error getting cached value for key: {}. Returning null (cache miss)", key);
			return null; // Treat all errors as cache miss - cache is optional
		}
	}
	
	public void put(String key, Object value, long ttlSeconds) {
		if (key == null || key.isEmpty()) {
			log.warn("Attempted to put cache with null or empty key");
			return;
		}
		
		if (value == null) {
			log.warn("Attempted to cache null value for key: {}", key);
			return;
		}
		
		if (ttlSeconds <= 0) {
			log.warn("Invalid TTL {} seconds for key: {}. Skipping cache put.", ttlSeconds, key);
			return;
		}
		
		try {
			String jsonValue = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
			log.debug("Cached value for key: {} with TTL: {} seconds", key, ttlSeconds);
		} catch (org.springframework.data.redis.RedisConnectionFailureException e) {
			log.warn("Redis connection failure while putting key: {}. Cache operation failed silently.", key);
			// Fail silently - cache is optional, service should continue working
		} catch (JsonProcessingException e) {
			log.warn("Error serializing value for cache key: {}. Cache operation failed silently.", key);
			// Fail silently - cache is optional, service should continue working
		} catch (Exception e) {
			log.warn("Unexpected error putting cached value for key: {}. Cache operation failed silently.", key);
			// Fail silently - cache is optional, service should continue working
		}
	}
	
	public void delete(String key) {
		if (key == null || key.isEmpty()) {
			log.warn("Attempted to delete cache with null or empty key");
			return;
		}
		
		try {
			redisTemplate.delete(key);
		} catch (org.springframework.data.redis.RedisConnectionFailureException e) {
			log.warn("Redis connection failure while deleting key: {}. Operation failed silently.", key);
			// Fail silently - cache is optional, service should continue working
		} catch (Exception e) {
			log.warn("Unexpected error deleting cached value for key: {}. Operation failed silently.", key);
			// Fail silently - cache is optional, service should continue working
		}
	}
}


