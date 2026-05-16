package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.ReseniaRequest;
import com.mitienda.ecommerce.dto.ReseniaResponse;
import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.models.Resenia;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.repositories.ReseniaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de reseñas
 */
@Service
public class ReseniaService {

    @Autowired
    private ReseniaRepository reseniaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Listar todas las reseñas
     */
    public List<ReseniaResponse> getAllResenias() {
        return reseniaRepository.findAll()
                .stream()
                .map(ReseniaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener reseña por ID
     */
    public ReseniaResponse getReseniaById(Long id) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada con ID: " + id));
        return new ReseniaResponse(resenia);
    }

    /**
     * Obtener reseñas de un producto (solo aprobadas)
     */
    public List<ReseniaResponse> getReseniasByProducto(Long idProducto) {
        return reseniaRepository.findByProductoIdAndAprobadoTrueOrderByFechaDesc(idProducto)
                .stream()
                .map(ReseniaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener todas las reseñas de un producto (incluso no aprobadas - ADMIN)
     */
    public List<ReseniaResponse> getAllReseniasByProducto(Long idProducto) {
        return reseniaRepository.findByProductoIdOrderByFechaDesc(idProducto)
                .stream()
                .map(ReseniaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener reseñas de un cliente
     */
    public List<ReseniaResponse> getReseniasByCliente(Long idCliente) {
        return reseniaRepository.findByClienteIdOrderByFechaDesc(idCliente)
                .stream()
                .map(ReseniaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener reseñas pendientes de aprobación
     */
    public List<ReseniaResponse> getReseniasPendientes() {
        return reseniaRepository.findByAprobadoFalseOrderByFechaDesc()
                .stream()
                .map(ReseniaResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Crear nueva reseña
     */
    @Transactional
    public ReseniaResponse createResenia(ReseniaRequest request) {
        // Validar producto
        Producto producto = productoRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + request.getIdProducto()));

        // Validar cliente
        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + request.getIdCliente()));

        // Crear reseña
        Resenia resenia = new Resenia();
        resenia.setProducto(producto);
        resenia.setCliente(cliente);
        resenia.setCalificacion(request.getCalificacion());
        resenia.setComentario(request.getComentario());
        resenia.setAprobado(false); // Requiere aprobación

        Resenia savedResenia = reseniaRepository.save(resenia);
        return new ReseniaResponse(savedResenia);
    }

    /**
     * Aprobar reseña
     */
    @Transactional
    public ReseniaResponse aprobarResenia(Long id) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada con ID: " + id));

        resenia.setAprobado(true);
        Resenia updatedResenia = reseniaRepository.save(resenia);
        return new ReseniaResponse(updatedResenia);
    }

    /**
     * Rechazar/Desaprobar reseña
     */
    @Transactional
    public ReseniaResponse rechazarResenia(Long id) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada con ID: " + id));

        resenia.setAprobado(false);
        Resenia updatedResenia = reseniaRepository.save(resenia);
        return new ReseniaResponse(updatedResenia);
    }

    /**
     * Eliminar reseña
     */
    @Transactional
    public void deleteResenia(Long id) {
        Resenia resenia = reseniaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada con ID: " + id));

        reseniaRepository.delete(resenia);
    }

    /**
     * Obtener calificación promedio de un producto
     */
    public Double getCalificacionPromedio(Long idProducto) {
        Double promedio = reseniaRepository.getCalificacionPromedio(idProducto);
        return promedio != null ? promedio : 0.0;
    }

    /**
     * Contar reseñas aprobadas de un producto
     */
    public Long countReseniasByProducto(Long idProducto) {
        return reseniaRepository.countByProductoIdAndAprobadoTrue(idProducto);
    }
}