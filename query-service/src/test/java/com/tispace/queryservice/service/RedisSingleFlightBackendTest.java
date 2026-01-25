package com.tispace.queryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tispace.queryservice.dto.SingleFlightEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSingleFlightBackendTest {
    
    @Mock
    private StringRedisTemplate redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private ObjectMapper objectMapper;
    private RedisSingleFlightBackend backend;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        backend = new RedisSingleFlightBackend(redisTemplate, objectMapper);
    }
    
    @Test
    void tryAcquireLock_whenSuccess_thenReturnsTrue() {
        String lockKey = "lock:test";
        String token = "token-123";
        Duration ttl = Duration.ofSeconds(10);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), eq(token), eq(ttl))).thenReturn(true);
        
        Boolean result = backend.tryAcquireLock(lockKey, token, ttl);
        
        assertTrue(result);
        verify(valueOperations).setIfAbsent(lockKey, token, ttl);
    }
    
    @Test
    void tryAcquireLock_whenLockHeld_thenReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        
        Boolean result = backend.tryAcquireLock("lock:test", "token", Duration.ofSeconds(10));
        
        assertFalse(result);
    }
    
    @Test
    void tryAcquireLock_whenRedisError_thenReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis down"));
        
        Boolean result = backend.tryAcquireLock("lock:test", "token", Duration.ofSeconds(10));
        
        assertNull(result);
    }
    
    @Test
    void releaseLock_whenSuccess_thenExecutesScript() {
        String lockKey = "lock:test";
        String token = "token-123";
        
        backend.releaseLock(lockKey, token);
        
        @SuppressWarnings("unchecked")
        ArgumentCaptor<DefaultRedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(DefaultRedisScript.class);
        
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(Collections.singletonList(lockKey)), eq(token));
        assertNotNull(scriptCaptor.getValue());
    }
    
    @Test
    void readResult_whenFound_thenReturnsEnvelope() throws Exception {
        String resultKey = "result:test";
        SingleFlightEnvelope expected = new SingleFlightEnvelope(true, "{\"value\":\"test\"}", null, null);
        String json = objectMapper.writeValueAsString(expected);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(resultKey)).thenReturn(json);
        
        SingleFlightEnvelope result = backend.readResult(resultKey);
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(expected.getPayload(), result.getPayload());
    }
    
    @Test
    void readResult_whenNotFound_thenReturnsNull() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        
        SingleFlightEnvelope result = backend.readResult("result:test");
        
        assertNull(result);
    }
    
    @Test
    void readResult_whenRedisError_thenThrowsException() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));
        
        assertThrows(Exception.class, () -> backend.readResult("result:test"));
    }
    
    @Test
    void writeResult_whenSuccess_thenStoresEnvelope() throws Exception {
        String resultKey = "result:test";
        SingleFlightEnvelope envelope = new SingleFlightEnvelope(true, "{\"value\":\"test\"}", null, null);
        Duration ttl = Duration.ofSeconds(10);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        backend.writeResult(resultKey, envelope, ttl);
        
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        
        verify(valueOperations).set(keyCaptor.capture(), jsonCaptor.capture(), ttlCaptor.capture());
        assertEquals(resultKey, keyCaptor.getValue());
        assertEquals(ttl, ttlCaptor.getValue());
        
        SingleFlightEnvelope stored = objectMapper.readValue(jsonCaptor.getValue(), SingleFlightEnvelope.class);
        assertEquals(envelope.isSuccess(), stored.isSuccess());
        assertEquals(envelope.getPayload(), stored.getPayload());
    }
    
    @Test
    void writeResult_whenRedisError_thenLogsWarningButDoesNotThrow() {
        String resultKey = "result:test";
        SingleFlightEnvelope envelope = new SingleFlightEnvelope(true, "{\"value\":\"test\"}", null, null);
        Duration ttl = Duration.ofSeconds(10);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        
        assertDoesNotThrow(() -> backend.writeResult(resultKey, envelope, ttl));
    }
}

