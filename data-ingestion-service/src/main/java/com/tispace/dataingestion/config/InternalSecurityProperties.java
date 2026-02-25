package com.tispace.dataingestion.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.internal")
@Validated
@Data
public class InternalSecurityProperties {

    @NotBlank
    private String header = "X-Internal-Token";

    @NotBlank
    private String token;
}
