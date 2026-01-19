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
		try {
			String value = redisTemplate.opsForValue().get(key);
			if (value == null) {
				return null;
			}
			return objectMapper.readValue(value, type);
		} catch (JsonProcessingException e) {
			log.error("Error deserializing cached value for key: {}", key, e);
			return null;
		}
	}
	
	public void put(String key, Object value, long ttlSeconds) {
		try {
			String jsonValue = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
			log.debug("Cached value for key: {} with TTL: {} seconds", key, ttlSeconds);
		} catch (JsonProcessingException e) {
			log.error("Error serializing value for cache key: {}", key, e);
		}
	}
	
	public void delete(String key) {
		redisTemplate.delete(key);
	}
}


