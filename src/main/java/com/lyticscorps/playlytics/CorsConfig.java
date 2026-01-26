package com.lyticscorps.playlytics;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    /**
     * Configures CORS (Cross-Origin Resource Sharing) mappings for the application.
     * Allows requests from local development servers on ports 3000 and 5173.
     * Restricts allowed methods to GET, POST, and OPTIONS.
     * Requires the X-Frontend-Api-Key header for API requests.
     *
     * @param registry the CORS registry to configure
     */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("X-Frontend-Api-Key", "Content-Type")
                .exposedHeaders("X-Session-Id")
                .allowCredentials(false);
    }
}
