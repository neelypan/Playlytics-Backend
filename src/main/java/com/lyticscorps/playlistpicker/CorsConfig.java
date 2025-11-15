package com.lyticscorps.playlistpicker;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
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
