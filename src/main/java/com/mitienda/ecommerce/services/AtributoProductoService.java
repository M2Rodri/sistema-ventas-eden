package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.AtributoProductoDTO;
import com.mitienda.ecommerce.models.AtributoProducto;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.AtributoProductoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de atributos de productos
 */
@Service
public class AtributoProductoService {

    @Autowired
    private AtributoProductoRepository atributoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Obtener atributos de un producto
     */
    public List<AtributoProductoDTO> getAtributosByProducto(Long idProducto) {
        return atributoRepository.findByProductoId(idProducto)
                .stream()
                .map(AtributoProductoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Agregar atributo a un producto
     */
    @Transactional
    public AtributoProductoDTO addAtributo(Long idProducto, AtributoProductoDTO dto) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        AtributoProducto atributo = new AtributoProducto();
        atributo.setProducto(producto);
        atributo.setNombreAtributo(dto.getNombreAtributo());
        atributo.setValorAtributo(dto.getValorAtributo());

        AtributoProducto savedAtributo = atributoRepository.save(atributo);
        return new AtributoProductoDTO(savedAtributo);
    }

    /**
     * Actualizar atributo
     */
    @Transactional
    public AtributoProductoDTO updateAtributo(Long id, AtributoProductoDTO dto) {
        AtributoProducto atributo = atributoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado con ID: " + id));

        atributo.setNombreAtributo(dto.getNombreAtributo());
        atributo.setValorAtributo(dto.getValorAtributo());

        AtributoProducto updatedAtributo = atributoRepository.save(atributo);
        return new AtributoProductoDTO(updatedAtributo);
    }

    /**
     * Eliminar atributo
     */
    @Transactional
    public void deleteAtributo(Long id) {
        AtributoProducto atributo = atributoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado con ID: " + id));
        atributoRepository.delete(atributo);
    }

    /**
     * Eliminar todos los atributos de un producto
     */
    @Transactional
    public void deleteAllAtributosByProducto(Long idProducto) {
        atributoRepository.deleteByProductoId(idProducto);
    }

    /**
     * Actualizar todos los atributos de un producto (reemplazar)
     */
    @Transactional
    public List<AtributoProductoDTO> updateAllAtributos(Long idProducto, List<AtributoProductoDTO> atributos) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        // Eliminar atributos existentes
        atributoRepository.deleteByProductoId(idProducto);

        // Crear nuevos atributos
        List<AtributoProducto> nuevosAtributos = atributos.stream()
                .map(dto -> new AtributoProducto(producto, dto.getNombreAtributo(), dto.getValorAtributo()))
                .collect(Collectors.toList());

        List<AtributoProducto> savedAtributos = atributoRepository.saveAll(nuevosAtributos);

        return savedAtributos.stream()
                .map(AtributoProductoDTO::new)
                .collect(Collectors.toList());
    }
}