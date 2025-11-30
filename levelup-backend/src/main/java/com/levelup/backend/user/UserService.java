package com.levelup.backend.user;

import com.levelup.backend.dto.*;
import com.levelup.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar que las contraseñas coincidan
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        
        // Validar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        
        // Buscar rol CLIENTE (crearlo si no existe)
        Role clienteRole = roleRepository.findByName(RoleName.CLIENTE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.CLIENTE).build()));
        
        // Crear usuario
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Collections.singleton(clienteRole))
                .build();
        
        user = userRepository.save(user);
        
        // Generar token JWT
        String token = jwtTokenProvider.generateToken(user.getEmail(), 
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));
        
        return AuthResponse.builder()
                .token("Bearer " + token)
                .user(mapToUserResponse(user))
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        // Autenticar usando AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Buscar usuario
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        
        // Generar token JWT
        String token = jwtTokenProvider.generateToken(user.getEmail(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));
        
        return AuthResponse.builder()
                .token("Bearer " + token)
                .user(mapToUserResponse(user))
                .build();
    }
    
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return mapToUserResponse(user);
    }
    
    @Transactional
    public UserResponse updateCurrentUser(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Actualizar campos si se proporcionan
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Verificar que el nuevo email no exista (si es diferente al actual)
            if (!request.getEmail().equals(email) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("El email ya está en uso");
            }
            user.setEmail(request.getEmail());
        }
        
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .build();
    }
}
