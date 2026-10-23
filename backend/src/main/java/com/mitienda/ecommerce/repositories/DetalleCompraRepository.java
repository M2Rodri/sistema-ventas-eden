package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.DetalleCompra;
import com.mitienda.ecommerce.models.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio para detalle de compras */
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    /** Obtener detalles de una compra */
    List<DetalleCompra> findByCompraId(Long compraId);

    /** Eliminar detalles de una compra */
    void deleteByCompraId(Long compraId);

    /** Si el producto ya tuvo una compra confirmada, su precio de compra lo maneja Compras (ver ProductoService). */
    boolean existsByProducto_IdAndCompra_Estado(Long idProducto, EstadoCompra estado);

    /** Ids de los productos con al menos una compra en ese estado, en una sola consulta para los listados. */
    @Query("select distinct d.producto.id from DetalleCompra d where d.compra.estado = :estado")
    List<Long> findIdsProductoPorEstadoCompra(@Param("estado") EstadoCompra estado);
}