package com.mitienda.ecommerce.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mitienda.ecommerce.storage.AlmacenArchivos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El comprobante se guarda como nombre de objeto y sale en el JSON como URL
 * firmada. Se prueba con el mismo mecanismo con el que Spring crea el
 * serializador, para asegurar que la anotación de PagoDTO funciona de verdad.
 */
class ComprobanteUrlSerializerTest {

    private AnnotationConfigApplicationContext contexto;
    private AlmacenArchivos almacen;
    private ObjectMapper mapper;

    @BeforeEach
    void preparar() {
        almacen = mock(AlmacenArchivos.class);
        contexto = new AnnotationConfigApplicationContext();
        contexto.registerBean(AlmacenArchivos.class, () -> almacen);
        contexto.refresh();

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.setHandlerInstantiator(new SpringHandlerInstantiator(contexto.getAutowireCapableBeanFactory()));
    }

    @AfterEach
    void cerrar() {
        contexto.close();
    }

    private String json(String guardado) throws Exception {
        PagoDTO pago = new PagoDTO();
        pago.setId(7L);
        pago.setUrlComprobante(guardado);
        return mapper.writeValueAsString(pago);
    }

    @Test
    void elJsonLlevaLaUrlFirmadaYNoElNombreDelObjeto() throws Exception {
        when(almacen.urlParaMostrar(eq("comprobantes"), eq("pago.jpg"), eq(Duration.ofHours(1))))
                .thenReturn("https://proyecto.supabase.co/storage/v1/object/sign/comprobantes/pago.jpg?token=T");

        String resultado = json("pago.jpg");

        assertTrue(resultado.contains("\"urlComprobante\":\"https://proyecto.supabase.co/storage/v1/object/sign/comprobantes/pago.jpg?token=T\""),
                resultado);
    }

    @Test
    void sinComprobanteSaleNulo() throws Exception {
        assertTrue(json(null).contains("\"urlComprobante\":null"));
        assertTrue(json("  ").contains("\"urlComprobante\":null"));
    }

    @Test
    void siNoSePuedeFirmarSaleNuloYNoRompeLaRespuesta() throws Exception {
        when(almacen.urlParaMostrar(any(), any(), any())).thenThrow(new RuntimeException("sin conexión"));

        assertTrue(json("pago.jpg").contains("\"urlComprobante\":null"));
    }
}
