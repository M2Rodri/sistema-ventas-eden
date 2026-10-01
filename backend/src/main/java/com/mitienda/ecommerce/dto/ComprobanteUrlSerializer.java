package com.mitienda.ecommerce.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/**
 * Convierte, al armar la respuesta JSON, lo que hay guardado como comprobante
 * en la URL con la que se puede ver la foto.
 *
 * El bucket de comprobantes es privado: lo guardado es el nombre del objeto, y
 * acá se cambia por una URL firmada que vence a la hora. Así ninguna foto de
 * pago tiene una URL permanente, y no hay que tocar cada lugar del backend que
 * arma un PagoDTO.
 *
 * Los registros viejos (/uploads/...) se devuelven tal cual.
 */
public class ComprobanteUrlSerializer extends StdSerializer<String> {

    static final Duration VIGENCIA = Duration.ofHours(1);

    private static final Logger log = LoggerFactory.getLogger(ComprobanteUrlSerializer.class);

    private final AlmacenArchivos almacen;

    public ComprobanteUrlSerializer(AlmacenArchivos almacen) {
        super(String.class);
        this.almacen = almacen;
    }

    @Override
    public void serialize(String guardado, JsonGenerator generador, SerializerProvider proveedor) throws IOException {
        if (guardado == null || guardado.isBlank()) {
            generador.writeNull();
            return;
        }
        String url = null;
        try {
            url = almacen.urlParaMostrar(AlmacenArchivos.BUCKET_COMPROBANTES, guardado, VIGENCIA);
        } catch (RuntimeException e) {
            log.warn("No se pudo preparar la URL del comprobante: {}", e.getMessage());
        }
        if (url == null) {
            generador.writeNull();
        } else {
            generador.writeString(url);
        }
    }
}
