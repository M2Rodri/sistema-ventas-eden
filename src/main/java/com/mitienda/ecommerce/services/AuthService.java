package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AuthResponse;
import com.mitienda.ecommerce.dto.LoginRequest;
import com.mitienda.ecommerce.dto.RegisterRequest;
import com.mitienda.ecommerce.models.User;
import com.mitienda.ecommerce.repositories.UserRepository;
import com.mitienda.ecommerce.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación
 * Maneja el registro y login de usuarios
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Registrar un nuevo usuario
     */
    public AuthResponse register(RegisterRequest request) {
        // Verificar si el email ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());

        user.setRole(com.mitienda.ecommerce.models.Role.CLIENTE); // Asignar rol por defecto
        user.setActivo(true); // Activar usuario

        // Guardar en la base de datos
        User savedUser = userRepository.save(user);

        // Generar token JWT
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        // Retornar respuesta con token y datos del usuario
        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getNombre(),
                savedUser.getApellido(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    /**
     * Login de usuario existente
     */
    public AuthResponse login(LoginRequest request) {
        // Autenticar usuario
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Buscar usuario en la base de datos
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si el usuario está activo
        if (!user.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }

        // Generar token JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // Retornar respuesta con token y datos del usuario
        return new AuthResponse(
                token,
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getEmail(),
                user.getRole()
        );
    }
}