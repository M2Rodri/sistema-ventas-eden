package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ConfiguracionRequest;
import com.mitienda.ecommerce.dto.ConfiguracionResponse;
import com.mitienda.ecommerce.models.ConfiguracionSistema;
import com.mitienda.ecommerce.repositories.ConfiguracionSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de configuraciones del sistema
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class ConfiguracionService {

    private final ConfiguracionSistemaRepository configuracionRepository;

    private final RegistroAuditoria registroAuditoria;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ConfiguracionService(ConfiguracionSistemaRepository configuracionRepository, RegistroAuditoria registroAuditoria) {
        this.configuracionRepository = configuracionRepository;
        this.registroAuditoria = registroAuditoria;
    }


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

        String valorAnterior = config.getValor();
        config.setValor(nuevoValor);
        ConfiguracionSistema updatedConfig = configuracionRepository.save(config);

        // Se guarda el antes y el despues: aca viven los datos del negocio que
        // salen impresos en los comprobantes, asi que conviene poder ver quien
        // cambio que y volver atras si hace falta.
        registroAuditoria.registrar("ACTUALIZAR_CONFIGURACION", "configuracion_sistema",
                updatedConfig.getId(),
                clave + ": \"" + valorAnterior + "\" -> \"" + nuevoValor + "\"");

        return new ConfiguracionResponse(updatedConfig);
    }

    /**
     * Eliminar configuración
     */
    @Transactional
    public void deleteConfiguracion(Long id) {
        ConfiguracionSistema config = configuracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada con ID: " + id));

        registroAuditoria.registrar("ELIMINAR_CONFIGURACION", "configuracion_sistema",
                config.getId(), "Se elimino la clave " + config.getClave());

        configuracionRepository.delete(config);
    }

    /**
     * Inicializar configuraciones por defecto (llamar al inicio de la aplicación)
     */
    @Transactional
    public void inicializarConfiguracionesDefecto() {
        // Solo crear si no existen
        // La clave es negocio_razon_social, no negocio_nombre: es la que leen la
        // tienda y la pantalla de Configuracion. Estaban las dos dando vueltas
        // para lo mismo, y el valor por defecto ademas traia la marca vieja.
        if (!configuracionRepository.existsByClave("negocio_razon_social")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "negocio_razon_social",
                "Muebleria Eden",
                "Nombre del negocio tal como aparece en la tienda y los comprobantes"
            ));
        }

        if (!configuracionRepository.existsByClave("negocio_telefono")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "negocio_telefono", 
                "+591 00000000", 
                "Teléfono de contacto"
            ));
        }

        if (!configuracionRepository.existsByClave("negocio_email")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "negocio_email", 
                "contacto@mitienda.com", 
                "Email de contacto"
            ));
        }

        if (!configuracionRepository.existsByClave("negocio_direccion")) {
            configuracionRepository.save(new ConfiguracionSistema(
                "negocio_direccion", 
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