package com.tispace.queryservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies actuator endpoint access: health/info are public, metrics/prometheus require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.internal.token=test-token")
class SecurityConfigActuatorTest {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_isPermittedWithoutAuth() throws Exception {
        // 200 when all components up; 503 when e.g. Redis (mocked) is down - endpoint must not return 403
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 200 || status == 503, "health should be permitted (200 or 503), was " + status);
    }

    @Test
    void actuatorHealthReadiness_isPermittedWithoutAuth() throws Exception {
        int status = mockMvc.perform(get("/actuator/health/readiness"))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 200 || status == 503, "readiness should be permitted (200 or 503), was " + status);
    }

    @Test
    void actuatorPrometheus_requiresAuth_returns403WithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actuatorMetrics_requiresAuth_returns403WithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
    }
}
