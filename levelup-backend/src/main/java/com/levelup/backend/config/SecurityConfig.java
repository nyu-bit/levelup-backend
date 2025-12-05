package com.levelup.backend.config;

import com.levelup.backend.security.CustomUserDetailsService;
import com.levelup.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    // ❌ CORS DESACTIVADO EN SPRING - Nginx lo maneja completamente
    // No hay CorsConfigurationSource, no hay .cors(), nada de CORS aquí.
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF para APIs REST
            .csrf(AbstractHttpConfigurer::disable)
            
            // 🔥 CORS DESACTIVADO - Nginx maneja CORS completamente
            .cors(AbstractHttpConfigurer::disable)
            
            // Stateless (usamos JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configurar autorización de requests
            .authorizeHttpRequests(auth -> auth
                // Permitir OPTIONS para preflight (Nginx maneja CORS, pero Spring debe dejar pasar OPTIONS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Rutas públicas de autenticación
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Permitir ver productos sin login
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                
                // Swagger/OpenAPI público
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // H2 Console (solo dev)
                .requestMatchers("/h2-console/**").permitAll()
                
                // Payment endpoints - test público, init requiere auth
                .requestMatchers("/api/v1/payments/test").permitAll()
                .requestMatchers("/api/v1/payments/**").authenticated()
                
                // Productos: modificaciones requieren rol ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/*/stock").hasAnyRole("ADMIN", "VENDEDOR")
                
                // Ventas: /all solo para ADMIN y VENDEDOR
                .requestMatchers("/api/v1/sales/all").hasAnyRole("ADMIN", "VENDEDOR")
                
                // Transbank endpoints públicos
                .requestMatchers(HttpMethod.POST, "/api/v1/sales/transbank/callback").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/sales/payment-status/**").permitAll()
                
                // Resto de ventas requieren autenticación
                .requestMatchers("/api/v1/sales/**").authenticated()
                
                // El resto protegido
                .anyRequest().authenticated()
            )
            
            // Registrar proveedor de autenticación
            .authenticationProvider(authenticationProvider())
            
            // Filtro JWT antes del UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Para H2 Console (frames) - disable para Codespaces
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        
        return http.build();
    }
}
