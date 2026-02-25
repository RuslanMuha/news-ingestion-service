package com.tispace.dataingestion.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.internal.token=test-token",
        "query-service.internal-token=test-token",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "scheduler.enabled=false",
        "external-api.news-api.api-key=test-key"
})
class SecurityConfigActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_isPermittedWithoutAuth() throws Exception {
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
    void actuatorPrometheus_requiresAuth_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorMetrics_requiresAuth_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorPrometheus_allowsAccessWithValidToken() throws Exception {
        int status = mockMvc.perform(get("/actuator/prometheus").header("X-Internal-Token", "test-token"))
                .andReturn().getResponse().getStatus();
        assertTrue(status != 401 && status != 403, "prometheus with token should not be blocked by auth (status was " + status + ")");
    }

    @Test
    void actuatorMetrics_allowsAccessWithValidToken() throws Exception {
        int status = mockMvc.perform(get("/actuator/metrics").header("X-Internal-Token", "test-token"))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 200 || status == 503, "metrics with token should be accessible (200 or 503), was " + status);
    }
}
