package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CategoriaRequest;
import com.mitienda.ecommerce.dto.CategoriaResponse;
import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.repositories.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar esta anotación

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de categorías
 */
@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

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
     * Crear nueva categoría
     */
    @Transactional // Ya tiene @Transactional, está bien
    public CategoriaResponse createCategoria(CategoriaRequest request) {
        // Validar que el nombre no exista
        if (categoriaRepository.existsByNombre(request.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getNombre());
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setActivo(request.getActivo());

        Categoria savedCategoria = categoriaRepository.save(categoria);
        return new CategoriaResponse(savedCategoria);
    }

    /**
     * Actualizar categoría existente
     */
    @Transactional // Ya tiene @Transactional, está bien
    public CategoriaResponse updateCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Validar nombre único (si cambió)
        if (!categoria.getNombre().equals(request.getNombre()) &&
            categoriaRepository.existsByNombre(request.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + request.getNombre());
        }

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setActivo(request.getActivo());

        Categoria updatedCategoria = categoriaRepository.save(categoria);
        return new CategoriaResponse(updatedCategoria);
    }

    /**
     * Eliminar categoría (desactivar)
     */
    @Transactional // Ya tiene @Transactional, está bien
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
    @Transactional // Ya tiene @Transactional, está bien
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