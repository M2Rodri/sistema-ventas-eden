package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.UserRequest;
import com.mitienda.ecommerce.dto.UserResponse;
import com.mitienda.ecommerce.models.Role;
import com.mitienda.ecommerce.models.User;
import com.mitienda.ecommerce.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios (CRUD completo)
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Listar todos los usuarios
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener usuario por ID
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return new UserResponse(user);
    }

    /**
     * Crear nuevo usuario
     */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        // Validar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado: " + request.getEmail());
        }

        // Validar que la contraseña no esté vacía al crear
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La contraseña es obligatoria al crear un usuario");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        user.setRole(request.getRole());
        user.setActivo(request.getActivo());

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }

    /**
     * Actualizar usuario existente
     */
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // Validar email único (si cambió)
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado: " + request.getEmail());
        }

        // Actualizar campos
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setEmail(request.getEmail());
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        user.setRole(request.getRole());
        user.setActivo(request.getActivo());

        // Solo actualizar password si se proporcionó uno nuevo
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser);
    }

    /**
     * Eliminar usuario (desactivar)
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // Desactivar en lugar de eliminar
        user.setActivo(false);
        userRepository.save(user);
    }

    /**
     * Activar/Desactivar usuario
     */
    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        user.setActivo(!user.getActivo());
        User updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser);
    }

    /**
     * Listar usuarios por rol
     */
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == role)
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar solo usuarios activos
     */
    public List<UserResponse> getActiveUsers() {
        return userRepository.findAll()
                .stream()
                .filter(User::getActivo)
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Contar usuarios por rol
     */
    public Long countUsersByRole(Role role) {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == role)
                .count();
    }

    /**
     * Contar usuarios activos
     */
    public Long countActiveUsers() {
        return userRepository.countByActivo(true);
    }
}