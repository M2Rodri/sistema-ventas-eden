package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.models.ImagenProducto;
import com.mitienda.ecommerce.models.Producto;
import com.mitienda.ecommerce.repositories.ImagenProductoRepository;
import com.mitienda.ecommerce.repositories.ProductoRepository;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import com.mitienda.ecommerce.storage.ValidadorImagen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
// Lectura dentro de transacción por defecto: con spring.jpa.open-in-view=false
// no hay sesión de Hibernate fuera de la transacción, y los DTO de respuesta se
// arman recorriendo relaciones perezosas. Sin esto, los endpoints de lectura
// fallaban con LazyInitializationException.
// Los métodos que escriben llevan su propio @Transactional, que tiene precedencia.
@Transactional(readOnly = true)
public class ImagenProductoService {

    private final ImagenProductoRepository imagenProductoRepository;

    private final ProductoRepository productoRepository;

    private final AlmacenArchivos almacen;

    private static final Logger log = LoggerFactory.getLogger(ImagenProductoService.class);

    /**
     * Inyeccion por constructor, no por campo.
     *
     * Es lo que recomienda Spring: las dependencias quedan final, la clase no
     * puede existir a medio construir, y una dependencia circular falla al
     * arrancar en vez de aparecer en ejecucion.
     */
    public ImagenProductoService(ImagenProductoRepository imagenProductoRepository,
                                 ProductoRepository productoRepository,
                                 AlmacenArchivos almacen) {
        this.imagenProductoRepository = imagenProductoRepository;
        this.productoRepository = productoRepository;
        this.almacen = almacen;
    }


    public List<ImagenProducto> getImagenesByProductoId(Long idProducto) {
        return imagenProductoRepository.findByProductoIdOrderByOrdenAsc(idProducto);
    }

    @Transactional
    public ImagenProducto saveImagenProducto(MultipartFile file, Long idProducto, Boolean esPrincipal) throws IOException {
        // Se valida antes de tocar el almacenamiento: tipo real, contenido y tamaño.
        ValidadorImagen.ImagenValida imagen =
                ValidadorImagen.validar(file, ValidadorImagen.MAXIMO_FOTO_PRODUCTO);

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + idProducto));

        String referencia = almacen.guardar(AlmacenArchivos.BUCKET_PRODUCTOS,
                imagen.nombreObjeto(), imagen.contenido(), imagen.tipoContenido());

        try {
            ImagenProducto nueva = new ImagenProducto();
            nueva.setUrlImagen(referencia);
            nueva.setEsPrincipal(esPrincipal != null && esPrincipal);
            nueva.setProducto(producto);

            // Si se marca como principal, desmarcar la anterior del mismo producto
            if (nueva.getEsPrincipal()) {
                desmarcarImagenPrincipalAnterior(idProducto);
            }

            return imagenProductoRepository.save(nueva);
        } catch (RuntimeException e) {
            // Si la base falla, el archivo recién subido quedaría huérfano en el bucket.
            try {
                almacen.eliminar(AlmacenArchivos.BUCKET_PRODUCTOS, referencia);
            } catch (IOException limpieza) {
                log.warn("No se pudo limpiar el archivo subido tras un error: {}", limpieza.getMessage());
            }
            throw e;
        }
    }

    private void desmarcarImagenPrincipalAnterior(Long idProducto) {
        List<ImagenProducto> imagenesExistentes = imagenProductoRepository.findByProductoIdOrderByOrdenAsc(idProducto);
        for (ImagenProducto img : imagenesExistentes) {
            if (img.getEsPrincipal()) {
                img.setEsPrincipal(false);
                imagenProductoRepository.save(img);
            }
        }
    }

    @Transactional
    public void deleteImagenProducto(Long idImagen) {
        ImagenProducto imagen = imagenProductoRepository.findById(idImagen)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada con ID: " + idImagen));

        // Primero el archivo en el bucket: si no se puede borrar, se avisa y el
        // registro queda para reintentar, en vez de dejar un archivo huérfano.
        try {
            almacen.eliminar(AlmacenArchivos.BUCKET_PRODUCTOS, imagen.getUrlImagen());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el archivo de la imagen: " + e.getMessage(), e);
        }

        imagenProductoRepository.deleteById(idImagen);
    }

    @Transactional
    public void setImagenPrincipal(Long idImagen, Long idProducto) {
        ImagenProducto imagen = imagenProductoRepository.findById(idImagen)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada con ID: " + idImagen));

        if (!imagen.getProducto().getId().equals(idProducto)) {
            throw new RuntimeException("La imagen no pertenece al producto especificado");
        }

        desmarcarImagenPrincipalAnterior(idProducto);

        imagen.setEsPrincipal(true);
        imagenProductoRepository.save(imagen);
    }
}