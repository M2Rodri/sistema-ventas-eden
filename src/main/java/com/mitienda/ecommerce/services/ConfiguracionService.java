package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ConfiguracionRequest;
import com.mitienda.ecommerce.dto.ConfiguracionResponse;
import com.mitienda.ecommerce.models.ConfiguracionSistema;
import com.mitienda.ecommerce.repositories.ConfiguracionSistemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de configuraciones del sistema
 */
@Service
public class ConfiguracionService {

    @Autowired
    private ConfiguracionSistemaRepository configuracionRepository;

    /**
     * Listar todas las configuraciones
     */
    public List<ConfiguracionResponse> getAllConfiguraciones() {
        return configuracionRepository.findAll()
                .stream()
                .map(ConfiguracionResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener configuración por ID
     */
    public ConfiguracionResponse getConfiguracionById(Long id) {
        ConfiguracionSistema config = configuracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con ID: " + id));
        return new ConfiguracionResponse(config);
    }

    /**
     * Obtener configuración por clave
     */
    public ConfiguracionResponse getConfiguracionByClave(String clave) {
        ConfiguracionSistema config = configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con clave: " + clave));
        return new ConfiguracionResponse(config);
    }

    /**
     * Obtener valor de configuración por clave (solo el valor)
     */
    public String getValorConfiguracion(String clave) {
        ConfiguracionSistema config = configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con clave: " + clave));
        return config.getValor();
    }

    /**
     * Crear nueva configuración
     */
    @Transactional
    public ConfiguracionResponse createConfiguracion(ConfiguracionRequest request) {
        // Validar que la clave no exista
        if (configuracionRepository.existsByClave(request.getClave())) {
            throw new RuntimeException("Ya existe una configuración con la clave: " + request.getClave());
        }

        ConfiguracionSistema config = new ConfiguracionSistema();
        config.setClave(request.getClave());
        config.setValor(request.getValor());
        config.setDescripcion(request.getDescripcion());

        ConfiguracionSistema savedConfig = configuracionRepository.save(config);
        return new ConfiguracionResponse(savedConfig);
    }

    /**
     * Actualizar configuración existente
     */
    @Transactional
    public ConfiguracionResponse updateConfiguracion(Long id, ConfiguracionRequest request) {
        ConfiguracionSistema config = configuracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con ID: " + id));

        // Validar clave única (si cambió)
        if (!config.getClave().equals(request.getClave()) && 
            configuracionRepository.existsByClave(request.getClave())) {
            throw new RuntimeException("Ya existe una configuración con la clave: " + request.getClave());
        }

        config.setClave(request.getClave());
        config.setValor(request.getValor());
        config.setDescripcion(request.getDescripcion());

        ConfiguracionSistema updatedConfig = configuracionRepository.save(config);
        return new ConfiguracionResponse(updatedConfig);
    }

    /**
     * Actualizar solo el valor de una configuración por clave
     */
    @Transactional
    public ConfiguracionResponse updateValorByClave(String clave, String nuevoValor) {
        ConfiguracionSistema config = configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con clave: " + clave));

        config.setValor(nuevoValor);
        ConfiguracionSistema updatedConfig = configuracionRepository.save(config);
        return new ConfiguracionResponse(updatedConfig);
    }

    /**
     * Eliminar configuración
     */
    @Transactional
    public void deleteConfiguracion(Long id) {
        ConfiguracionSistema config = configuracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con ID: " + id));

        configuracionRepository.delete(config);
    }

    /**
     * Inicializar configuraciones por defecto (llamar al inicio de la aplicación)
     */
    @Transactional
    public void inicializarConfiguracionesDefecto() {
        // Solo crear si no existen
        if (!configuracionRepository.existsByClave("empresa_nombre")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "empresa_nombre", 
                "Mi Tienda de Camas y Colchones", 
                "Nombre de la empresa"
            ));
        }

        if (!configuracionRepository.existsByClave("empresa_telefono")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "empresa_telefono", 
                "+591 00000000", 
                "Teléfono de contacto"
            ));
        }

        if (!configuracionRepository.existsByClave("empresa_email")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "empresa_email", 
                "contacto@mitienda.com", 
                "Email de contacto"
            ));
        }

        if (!configuracionRepository.existsByClave("empresa_direccion")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "empresa_direccion", 
                "Dirección principal", 
                "Dirección física de la empresa"
            ));
        }

        if (!configuracionRepository.existsByClave("iva_porcentaje")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "iva_porcentaje", 
                "13", 
                "Porcentaje de IVA aplicable"
            ));
        }

        if (!configuracionRepository.existsByClave("moneda")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "moneda", 
                "BOB", 
                "Moneda del sistema (BOB, USD, etc.)"
            ));
        }
    }
}