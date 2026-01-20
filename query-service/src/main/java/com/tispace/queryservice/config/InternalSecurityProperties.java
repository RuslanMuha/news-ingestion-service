package com.tispace.queryservice.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.internal")
@Data
public class InternalSecurityProperties {

    private String header = "X-Internal-Token";
    private String token;
}