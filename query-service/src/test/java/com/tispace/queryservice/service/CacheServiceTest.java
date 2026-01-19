package com.tispace.queryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {
	
	@Mock
	private RedisTemplate<String, String> redisTemplate;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@Mock
	private ValueOperations<String, String> valueOperations;
	
	@InjectMocks
	private CacheService cacheService;
	
	@BeforeEach
	void setUp() {
		// Set up mock that's used in most tests, but make it lenient for tests that don't need it
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}
	
	@Test
	void testGet_KeyExists_ReturnsValue() throws Exception {
		String key = "test:key";
		String jsonValue = "{\"id\":1,\"name\":\"test\"}";
		TestObject expectedObject = new TestObject(1L, "test");
		
		when(valueOperations.get(key)).thenReturn(jsonValue);
		when(objectMapper.readValue(jsonValue, TestObject.class)).thenReturn(expectedObject);
		
		TestObject result = cacheService.get(key, TestObject.class);
		
		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals("test", result.getName());
		verify(valueOperations, times(1)).get(key);
	}
	
	@Test
	void testGet_KeyNotExists_ReturnsNull() {
		String key = "test:key";
		
		when(valueOperations.get(key)).thenReturn(null);
		
		TestObject result = cacheService.get(key, TestObject.class);
		
		assertNull(result);
		verify(valueOperations, times(1)).get(key);
	}
	
	@Test
	void testPut_Success_CachesValue() throws Exception {
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		String jsonValue = "{\"id\":1,\"name\":\"test\"}";
		
		when(objectMapper.writeValueAsString(value)).thenReturn(jsonValue);
		
		cacheService.put(key, value, 3600);
		
		verify(objectMapper, times(1)).writeValueAsString(value);
		verify(valueOperations, times(1)).set(eq(key), eq(jsonValue), eq(3600L), eq(TimeUnit.SECONDS));
	}
	
	@Test
	void testDelete_Success_DeletesKey() {
		String key = "test:key";
		
		cacheService.delete(key);
		
		verify(redisTemplate, times(1)).delete(key);
	}
	
	// Helper class for testing
	private static class TestObject {
		private Long id;
		private String name;
		
		public TestObject(Long id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public Long getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
	}
}

