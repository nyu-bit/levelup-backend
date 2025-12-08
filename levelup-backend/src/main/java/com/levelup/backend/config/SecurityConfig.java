package com.levelup.backend.config;

import com.levelup.backend.security.CustomUserDetailsService;
import com.levelup.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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

import java.util.List;

/**
 * Configuración de seguridad Spring Security para Level-Up Gamer.
 * 
 * CORS habilitado para:
 * - https://levelupgamer.lol (producción)
 * - https://www.levelupgamer.lol (producción con www)
 * - http://localhost:5173 (desarrollo Vite)
 * 
 * Rutas públicas (sin autenticación):
 * - /api/auth/** (registro, login)
 * - GET /api/products/** (catálogo)
 * - /api/payments/webpay/** (pagos)
 * - /swagger-ui/**, /v3/api-docs/** (documentación)
 * 
 * Rutas protegidas (requieren JWT):
 * - Todo lo demás
 */
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
    
    /**
     * Configuración de CORS.
     * Permite requests desde el frontend en producción y desarrollo.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 🔹 Dominios que pueden llamar a la API
        config.setAllowedOrigins(List.of(
                "https://levelupgamer.lol",       // frontend producción
                "https://www.levelupgamer.lol",   // frontend producción con www
                "http://localhost:5173"           // frontend local Vite
        ));
        
        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Headers permitidos (todos)
        config.setAllowedHeaders(List.of("*"));
        
        // Para que se envíe el token JWT en headers
        config.setAllowCredentials(true);
        
        // Cache preflight por 1 hora
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // aplica a todas las rutas
        
        return source;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 🔥 CORS habilitado con Customizer.withDefaults() - usa corsConfigurationSource automáticamente
            .cors(Customizer.withDefaults())
            
            // Desactivar CSRF para APIs REST stateless
            .csrf(AbstractHttpConfigurer::disable)
            
            // Sesión stateless (usamos JWT, no sesiones)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configurar autorización de requests
            .authorizeHttpRequests(auth -> auth
                // Permitir OPTIONS para preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // === RUTAS PÚBLICAS ===
                
                // Auth - todos los endpoints de autenticación públicos
                .requestMatchers("/api/auth/**").permitAll()
                
                // Products - lectura pública (catálogo)
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                
                // Webpay - todos los endpoints de pago públicos
                .requestMatchers("/api/payments/webpay/**").permitAll()
                
                // Swagger/OpenAPI - documentación pública
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // H2 Console (solo dev)
                .requestMatchers("/h2-console/**").permitAll()
                
                // === RUTAS PROTEGIDAS ===
                
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
            
            // Para H2 Console (frames) - solo en desarrollo
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        
        return http.build();
    }
}
