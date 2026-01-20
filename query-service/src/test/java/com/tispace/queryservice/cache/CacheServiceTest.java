package com.tispace.queryservice.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {
	
	@Mock
	private RedisTemplate<String, String> redisTemplate;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@Mock
	private CacheMetrics cacheMetrics;
	
	@Mock
	private ValueOperations<String, String> valueOperations;
	
	private CacheService cacheService;
	
	@BeforeEach
	void setUp() {
		// Create CacheService with mocked CacheMetrics
		cacheService = new CacheService(redisTemplate, objectMapper, cacheMetrics);
		
		// Set up mock that's used in most tests, but make it lenient for tests that don't need it
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		
		// Mock CacheMetrics methods to do nothing by default
		lenient().when(cacheMetrics.recordGet(any())).thenAnswer(invocation -> {
			CacheMetrics.TimerCallable<?> callable = invocation.getArgument(0);
			return callable.call();
		});
		lenient().doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(0);
			runnable.run();
			return null;
		}).when(cacheMetrics).recordPut(any());
		lenient().doNothing().when(cacheMetrics).hit();
		lenient().doNothing().when(cacheMetrics).miss();
		lenient().doNothing().when(cacheMetrics).error();
		lenient().doNothing().when(cacheMetrics).unavailable();
	}
	
	@Test
	void testGet_KeyExists_ReturnsValue() throws Exception {
		String key = "test:key";
		String jsonValue = "{\"id\":1,\"name\":\"test\"}";
		TestObject expectedObject = new TestObject(1L, "test");
		
		when(valueOperations.get(key)).thenReturn(jsonValue);
		when(objectMapper.readValue(jsonValue, TestObject.class)).thenReturn(expectedObject);
		
		CacheResult<TestObject> result = cacheService.get(key, TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Hit);
		TestObject value = ((CacheResult.Hit<TestObject>) result).value();
		assertEquals(1L, value.getId());
		assertEquals("test", value.getName());
		verify(valueOperations, times(1)).get(key);
		verify(cacheMetrics, times(1)).recordGet(any());
		verify(cacheMetrics, times(1)).hit();
		verify(cacheMetrics, never()).miss();
		verify(cacheMetrics, never()).error();
	}
	
	@Test
	void testGet_KeyNotExists_ReturnsMiss() {
		String key = "test:key";
		
		when(valueOperations.get(key)).thenReturn(null);
		
		CacheResult<TestObject> result = cacheService.get(key, TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Miss);
		verify(valueOperations, times(1)).get(key);
		verify(cacheMetrics, times(1)).recordGet(any());
		verify(cacheMetrics, times(1)).miss();
		verify(cacheMetrics, never()).hit();
		verify(cacheMetrics, never()).error();
	}
	
	@Test
	void testPut_Success_CachesValue() throws Exception {
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		String jsonValue = "{\"id\":1,\"name\":\"test\"}";
		
		when(objectMapper.writeValueAsString(value)).thenReturn(jsonValue);
		
		ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
		
		cacheService.put(key, value, 3600);
		
		verify(objectMapper, times(1)).writeValueAsString(value);
		verify(valueOperations, times(1)).set(eq(key), eq(jsonValue), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
		verify(cacheMetrics, times(1)).recordPut(any());
		verify(cacheMetrics, never()).error();
		
		// TTL has jitter applied (±10%), so we check that it's in the expected range (3240-3960)
		Long actualTtl = ttlCaptor.getValue();
		assertTrue(actualTtl >= 3240L && actualTtl <= 3960L, 
			"TTL should be in range 3240-3960 (3600 ±10%), but was: " + actualTtl);
	}
	
	@Test
	void testDelete_Success_DeletesKey() {
		String key = "test:key";
		
		cacheService.delete(key);
		
		verify(redisTemplate, times(1)).delete(key);
	}
	
	@Test
	void testGet_DeserializationException_ReturnsError() throws Exception {
		// CacheService handles exceptions gracefully and returns CacheResult.error()
		String key = "test:key";
		String jsonValue = "invalid json";
		
		when(valueOperations.get(key)).thenReturn(jsonValue);
		when(objectMapper.readValue(jsonValue, TestObject.class))
			.thenThrow(new com.fasterxml.jackson.databind.JsonMappingException(null, "Parse error"));
		
		CacheResult<TestObject> result = cacheService.get(key, TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Error); // Exceptions are handled and return error
		verify(valueOperations, times(1)).get(key);
		verify(cacheMetrics, times(1)).recordGet(any());
		verify(cacheMetrics, times(1)).error(); // Exception is caught in getFromCache, so error() is called once
		verify(cacheMetrics, never()).hit();
		verify(cacheMetrics, never()).miss();
	}
	
	@Test
	void testPut_SerializationException_FailsSilently() throws Exception {
		// CacheService handles exceptions gracefully and fails silently
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		
		when(objectMapper.writeValueAsString(value))
			.thenThrow(new com.fasterxml.jackson.databind.JsonMappingException(null, "Serialize error"));
		
		// Should not throw exception - fails silently
		cacheService.put(key, value, 3600);
		
		verify(objectMapper, times(1)).writeValueAsString(value);
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		verify(cacheMetrics, times(1)).recordPut(any());
		verify(cacheMetrics, times(1)).error(); // Exception is caught in putToCache, so error() is called once
	}
	
	@Test
	void testGet_RedisThrowsException_ReturnsError() {
		// CacheService handles exceptions gracefully and returns CacheResult.error()
		String key = "test:key";
		
		when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis connection error"));
		
		CacheResult<TestObject> result = cacheService.get(key, TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Error); // Exceptions are handled and return error
		verify(valueOperations, times(1)).get(key);
		verify(cacheMetrics, times(1)).recordGet(any());
		verify(cacheMetrics, times(1)).error(); // Exception is caught in getFromCache, so error() is called once
	}
	
	@Test
	void testPut_RedisThrowsException_FailsSilently() throws Exception {
		// CacheService handles exceptions gracefully and fails silently
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		String jsonValue = "{\"id\":1,\"name\":\"test\"}";
		
		when(objectMapper.writeValueAsString(value)).thenReturn(jsonValue);
		
		ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
		// TTL has jitter applied (±10%), so we capture the actual value
		doThrow(new RuntimeException("Redis connection error"))
			.when(valueOperations).set(eq(key), eq(jsonValue), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
		
		// Should not throw exception - fails silently
		cacheService.put(key, value, 3600);
		
		verify(objectMapper, times(1)).writeValueAsString(value);
		verify(valueOperations, times(1)).set(eq(key), eq(jsonValue), anyLong(), eq(TimeUnit.SECONDS));
		verify(cacheMetrics, times(1)).recordPut(any());
		verify(cacheMetrics, times(1)).error(); // Exception is caught in putToCache, so error() is called once
		
		// TTL has jitter applied (±10%), so we check that it's in the expected range (3240-3960)
		Long actualTtl = ttlCaptor.getValue();
		assertTrue(actualTtl >= 3240L && actualTtl <= 3960L, 
			"TTL should be in range 3240-3960 (3600 ±10%), but was: " + actualTtl);
	}
	
	@Test
	void testDelete_RedisThrowsException_FailsSilently() {
		// CacheService handles exceptions gracefully and fails silently
		String key = "test:key";
		
		doThrow(new RuntimeException("Redis connection error")).when(redisTemplate).delete(key);
		
		// Should not throw exception - fails silently
		cacheService.delete(key);
		
		verify(redisTemplate, times(1)).delete(key);
		verify(cacheMetrics, times(1)).error();
	}
	
	@Test
	void testGet_EmptyJson_HandlesGracefully() throws Exception {
		String key = "test:key";
		String jsonValue = "{}";
		TestObject expectedObject = new TestObject(null, null);
		
		when(valueOperations.get(key)).thenReturn(jsonValue);
		when(objectMapper.readValue(jsonValue, TestObject.class)).thenReturn(expectedObject);
		
		CacheResult<TestObject> result = cacheService.get(key, TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Hit);
		verify(valueOperations, times(1)).get(key);
		verify(cacheMetrics, times(1)).hit();
	}
	
	@Test
	void testPut_ZeroTtl_SkipsCaching() throws Exception {
		// CacheService skips caching when TTL <= 0
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		
		cacheService.put(key, value, 0);
		
		// Should not call objectMapper or valueOperations when TTL is invalid
		verify(objectMapper, never()).writeValueAsString(any());
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		verify(cacheMetrics, never()).recordPut(any());
	}
	
	@Test
	void testPut_NegativeTtl_SkipsCaching() throws Exception {
		// CacheService skips caching when TTL <= 0
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		
		cacheService.put(key, value, -1);
		
		// Should not call objectMapper or valueOperations when TTL is invalid
		verify(objectMapper, never()).writeValueAsString(any());
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		verify(cacheMetrics, never()).recordPut(any());
	}
	
	@Test
	void testGet_NullKey_ReturnsMiss() throws Exception {
		// CacheService handles null key gracefully and returns CacheResult.miss()
		CacheResult<TestObject> result = cacheService.get(null, TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Miss);
		verify(valueOperations, never()).get(anyString());
		verify(cacheMetrics, never()).recordGet(any());
	}
	
	@Test
	void testGet_BlankKey_ReturnsMiss() throws Exception {
		// CacheService handles blank key gracefully and returns CacheResult.miss()
		CacheResult<TestObject> result = cacheService.get("   ", TestObject.class);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Miss);
		verify(valueOperations, never()).get(anyString());
		verify(cacheMetrics, never()).recordGet(any());
	}
	
	@Test
	void testPut_NullKey_FailsSilently() throws Exception {
		// CacheService handles null key gracefully and fails silently
		TestObject value = new TestObject(1L, "test");
		
		cacheService.put(null, value, 3600);
		
		verify(objectMapper, never()).writeValueAsString(any());
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		verify(cacheMetrics, never()).recordPut(any());
	}
	
	@Test
	void testPut_BlankKey_FailsSilently() throws Exception {
		// CacheService handles blank key gracefully and fails silently
		TestObject value = new TestObject(1L, "test");
		
		cacheService.put("   ", value, 3600);
		
		verify(objectMapper, never()).writeValueAsString(any());
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		verify(cacheMetrics, never()).recordPut(any());
	}
	
	@Test
	void testPut_NullValue_SkipsCaching() throws Exception {
		// CacheService skips caching when value is null
		String key = "test:key";
		
		cacheService.put(key, null, 3600);
		
		// Should not call objectMapper or valueOperations when value is null
		verify(objectMapper, never()).writeValueAsString(any());
		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
		verify(cacheMetrics, never()).recordPut(any());
	}
	
	@Test
	void testDelete_NullKey_FailsSilently() {
		cacheService.delete(null);
		verify(redisTemplate, never()).delete(anyString());
	}
	
	@Test
	void testDelete_BlankKey_FailsSilently() {
		cacheService.delete("   ");
		verify(redisTemplate, never()).delete(anyString());
	}
	
	@Test
	void testGetFallback_ReturnsError() {
		String key = "test:key";
		Throwable cause = new RuntimeException("Circuit breaker open");
		
		CacheResult<TestObject> result = cacheService.getFallback(key, TestObject.class, cause);
		
		assertNotNull(result);
		assertTrue(result instanceof CacheResult.Error);
		assertEquals(cause, ((CacheResult.Error<TestObject>) result).cause());
		verify(cacheMetrics, times(1)).unavailable();
	}
	
	@Test
	void testPutFallback_DoesNothing() {
		String key = "test:key";
		TestObject value = new TestObject(1L, "test");
		long ttlSeconds = 3600;
		Throwable cause = new RuntimeException("Circuit breaker open");
		
		// Should not throw exception
		cacheService.putFallback(key, value, ttlSeconds, cause);
		
		verify(cacheMetrics, times(1)).unavailable();
		try {
			verify(objectMapper, never()).writeValueAsString(any());
		} catch (Exception e) {
			// This won't happen with never(), but needed for compilation
		}
		verify(redisTemplate, never()).opsForValue();
	}
	
	@Test
	void testDeleteFallback_DoesNothing() {
		String key = "test:key";
		Throwable cause = new RuntimeException("Circuit breaker open");
		
		// Should not throw exception
		cacheService.deleteFallback(key, cause);
		
		verify(cacheMetrics, times(1)).unavailable();
		verify(redisTemplate, never()).delete(anyString());
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

