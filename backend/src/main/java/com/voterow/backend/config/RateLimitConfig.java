package com.voterow.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Profile("prod")
public class RateLimitConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/voting/cast", "/api/auth/login");
    }

    public static class RateLimitInterceptor implements HandlerInterceptor {
        private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Long> requestTimes = new ConcurrentHashMap<>();
        private final int MAX_REQUESTS = 10;
        private final long TIME_WINDOW = 60000; // 1 minute

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String clientIp = getClientIp(request);

            // Keep local development friction-free. The frontend in this workspace
            // talks to the backend from localhost, so repeated auth attempts after
            // logout should not be blocked by the prod limiter.
            if (isLocalClient(clientIp)) {
                return true;
            }

            // Keep login and vote-cast limits independent. Dashboard polling must not
            // consume the login quota for every user sharing localhost/NAT.
            String rateLimitKey = clientIp + ":" + request.getRequestURI();
            long currentTime = System.currentTimeMillis();
            
            requestTimes.entrySet().removeIf(entry -> currentTime - entry.getValue() > TIME_WINDOW);
            requestCounts.entrySet().removeIf(entry -> !requestTimes.containsKey(entry.getKey()));
            
            AtomicInteger count = requestCounts.computeIfAbsent(rateLimitKey, k -> new AtomicInteger(0));
            requestTimes.put(rateLimitKey, currentTime);
            
            if (count.incrementAndGet() > MAX_REQUESTS) {
                response.setStatus(429);
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return false;
            }
            
            return true;
        }

        private String getClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }

        private boolean isLocalClient(String clientIp) {
            return "127.0.0.1".equals(clientIp)
                || "::1".equals(clientIp)
                || "0:0:0:0:0:0:0:1".equals(clientIp)
                || clientIp.startsWith("10.")
                || clientIp.startsWith("192.168.")
                || clientIp.startsWith("172.16.")
                || clientIp.startsWith("172.17.")
                || clientIp.startsWith("172.18.")
                || clientIp.startsWith("172.19.")
                || clientIp.startsWith("172.2")
                || clientIp.startsWith("localhost");
        }
    }
}
