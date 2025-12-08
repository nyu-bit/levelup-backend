package com.levelup.backend.config;

import com.levelup.backend.security.CustomUserDetailsService;
import com.levelup.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad Spring Security.
 * 
 * Rutas públicas (sin autenticación):
 * - POST /api/auth/register
 * - POST /api/auth/login
 * - GET /api/products/**
 * - POST /api/payments/webpay/create
 * - POST /api/payments/webpay/commit
 * - GET|POST /api/payments/webpay/return
 * 
 * Rutas protegidas (requieren JWT):
 * - GET /api/auth/me
 * - Todo lo demás
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.frontend.base-url:https://levelupgamer.lol}")
    private String frontendBaseUrl;
    
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
    
    /**
     * Configuración de CORS.
     * Permite requests desde el frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                frontendBaseUrl,
                "https://levelupgamer.lol",
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF para APIs REST
            .csrf(AbstractHttpConfigurer::disable)
            
            // Habilitar CORS con la configuración definida
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless (usamos JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configurar autorización de requests
            .authorizeHttpRequests(auth -> auth
                // Permitir OPTIONS para preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // === RUTAS PÚBLICAS ===
                
                // Auth - registro y login
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                
                // Products - lectura pública
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                
                // Webpay - endpoints de pago públicos
                .requestMatchers("/api/payments/webpay/create").permitAll()
                .requestMatchers("/api/payments/webpay/commit").permitAll()
                .requestMatchers("/api/payments/webpay/return").permitAll()
                .requestMatchers("/api/payments/webpay/health").permitAll()
                
                // Swagger/OpenAPI
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // H2 Console (solo dev)
                .requestMatchers("/h2-console/**").permitAll()
                
                // === RUTAS PROTEGIDAS ===
                
                // Auth - obtener usuario actual requiere JWT
                .requestMatchers("/api/auth/me").authenticated()
                
                // Products - crear/editar/eliminar requiere ADMIN
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                
                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            
            // Registrar proveedor de autenticación
            .authenticationProvider(authenticationProvider())
            
            // Filtro JWT antes del UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Para H2 Console (frames)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        
        return http.build();
    }
}
