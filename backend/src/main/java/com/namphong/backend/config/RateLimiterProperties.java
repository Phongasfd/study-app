package com.namphong.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter; 

@Getter
@Setter 
@ConfigurationProperties(prefix = "ratelimiter") // Bind properties with prefix "ratelimiter" from application.yml
public class RateLimiterProperties {

    /** Number of requests allowed per window */
    private long requests = 100;

    /** Window length in seconds */
    private long durationSeconds = 60;

}
