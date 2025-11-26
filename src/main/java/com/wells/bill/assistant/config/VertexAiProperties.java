package com.wells.bill.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.ai.vertex.ai.embedding")
public class VertexAiProperties {
    private String projectId;
    private String location;
}
