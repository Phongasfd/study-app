package com.namphong.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setRequests(2);
        properties.setDurationSeconds(60);
        filter = new RateLimitingFilter(properties);
    }

    private int send(String remoteAddr, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/group/joined");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void blocksAfterLimit() throws Exception {
        assertEquals(200, send("1.1.1.1", null));
        assertEquals(200, send("1.1.1.1", null));
        assertEquals(429, send("1.1.1.1", null));
    }

    @Test
    void spoofedForwardedForDoesNotGiveAFreshBucket() throws Exception {
        // the old filter keyed on the first X-Forwarded-For entry, which the client controls
        assertEquals(200, send("1.1.1.1", "10.0.0.1"));
        assertEquals(200, send("1.1.1.1", "10.0.0.2"));
        assertEquals(429, send("1.1.1.1", "10.0.0.3"));
    }

    @Test
    void differentClientsHaveSeparateBuckets() throws Exception {
        assertEquals(200, send("1.1.1.1", null));
        assertEquals(200, send("1.1.1.1", null));
        assertEquals(200, send("2.2.2.2", null));
    }
}
