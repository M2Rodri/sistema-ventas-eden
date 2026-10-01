package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CategoriaRequest;
import com.mitienda.ecommerce.dto.CategoriaResponse;
import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.models.TipoProducto;
import com.mitienda.ecommerce.repositories.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar esta anotación

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de categorías
 */
@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class CategoriaService {

    /** Las únicas categorías que existen: una por tipo de producto (son cinco). */
    private static final Map<TipoProducto, String> CATEGORIAS_FIJAS = new EnumMap<>(Map.of(
            TipoProducto.CAMA, "Camas",
            TipoProducto.COLCHON, "Colchones",
            TipoProducto.ALMOHADA, "Almohadas",
            TipoProducto.ACCESORIO, "Accesorios",
            TipoProducto.MUEBLE, "Muebles de dormitorio"));

    private final CategoriaRepository categoriaRepository;

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }


    /**
     * Listar todas las categorías
     */
    @Transactional(readOnly = true) // <--- AÑADIR ESTA ANOTACIÓN
    public List<CategoriaResponse> getAllCategorias() {
        List<Categoria> categorias = categoriaRepository.findAll();
        return categorias.stream()
                .map(CategoriaResponse::new) // Se ejecuta dentro de la transacción
                .collect(Collectors.toList());
    }

    /**
     * Listar solo categorías activas
     */
    @Transactional(readOnly = true) // <--- AÑADIR ESTA ANOTACIÓN
    public List<CategoriaResponse> getActiveCategorias() {
        List<Categoria> categorias = categoriaRepository.findByActivoTrue();
        return categorias.stream()
                .map(CategoriaResponse::new) // Se ejecuta dentro de la transacción
                .collect(Collectors.toList());
    }

    /**
     * Obtener categoría por ID
     */
    @Transactional(readOnly = true) // <--- AÑADIR ESTA ANOTACIÓN
    public CategoriaResponse getCategoriaById(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
        return new CategoriaResponse(categoria); // Se ejecuta dentro de la transacción
    }

    /**
     * Solo se admiten las cuatro categorías fijas. El tipo de producto sale del
     * nombre, y el nombre se guarda siempre con su forma canónica ("Camas").
     */
    private void normalizarCategoriaFija(CategoriaRequest request) {
        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        for (Map.Entry<TipoProducto, String> fija : CATEGORIAS_FIJAS.entrySet()) {
            if (fija.getValue().equalsIgnoreCase(nombre)) {
                request.setNombre(fija.getValue());
                request.setTipoProducto(fija.getKey());
                return;
            }
        }
        throw new RuntimeException("Solo existen las categorías: Camas, Colchones, Almohadas, Accesorios y Muebles de dormitorio");
    }

    /**
     * Crear nueva categoría
     */
    @Transactional
    public CategoriaResponse createCategoria(CategoriaRequest request) {
        normalizarCategoriaFija(request);
        // Validar que el nombre no exista
        if (categoriaRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getNombre());
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setTipoProducto(request.getTipoProducto());
        categoria.setActivo(request.getActivo());

        Categoria savedCategoria = categoriaRepository.save(categoria);
        return new CategoriaResponse(savedCategoria);
    }

    /**
     * Actualizar categoría existente
     */
    @Transactional
    public CategoriaResponse updateCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        normalizarCategoriaFija(request);

        // Validar nombre único (si cambió)
        if (!categoria.getNombre().equalsIgnoreCase(request.getNombre().trim()) &&
            categoriaRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getNombre());
        }

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setTipoProducto(request.getTipoProducto());
        categoria.setActivo(request.getActivo());

        Categoria updatedCategoria = categoriaRepository.save(categoria);
        return new CategoriaResponse(updatedCategoria);
    }

    /**
     * Eliminar categoría (desactivar)
     */
    @Transactional
    public void deleteCategoria(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Verificar que no tenga productos asociados
        // Este acceso también se beneficia de estar dentro de la transacción
        if (!categoria.getProductos().isEmpty()) {
            throw new RuntimeException("No se puede eliminar una categoría con productos asociados");
        }

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    /**
     * Activar/Desactivar categoría
     */
    @Transactional
    public CategoriaResponse toggleCategoriaStatus(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        categoria.setActivo(!categoria.getActivo());
        Categoria updatedCategoria = categoriaRepository.save(categoria);
        return new CategoriaResponse(updatedCategoria);
    }

    /**
     * Contar categorías activas
     */
    @Transactional(readOnly = true) // <--- AÑADIR ESTA ANOTACIÓN (buena práctica)
    public Long countActiveCategorias() {
        return categoriaRepository.countByActivo(true);
    }
}