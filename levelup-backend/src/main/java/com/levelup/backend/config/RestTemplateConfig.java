package com.levelup.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración de RestTemplate para llamadas HTTP externas.
 * Los timeouts se configuran desde application.properties según el ambiente.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${payment.transbank.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${payment.transbank.read-timeout:30000}")
    private int readTimeout;

    /**
     * Bean de RestTemplate configurado con timeouts desde propiedades.
     * 
     * Configuración en application.properties:
     * - payment.transbank.connect-timeout: Timeout de conexión (ms)
     * - payment.transbank.read-timeout: Timeout de lectura (ms)
     * 
     * @param builder RestTemplateBuilder inyectado por Spring
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}
