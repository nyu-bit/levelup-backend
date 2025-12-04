package com.levelup.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 🔓 Para la eval: permitir cualquier origen (front local, Vercel, dominio, etc.)
        config.setAllowedOriginPatterns(List.of("*"));

        // Métodos permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos
        config.setAllowedHeaders(List.of("*"));

        // Permitir credenciales si usas Authorization, cookies, etc.
        config.setAllowCredentials(true);

        // Tiempo de cache del preflight
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplicar CORS a TODAS las rutas
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
