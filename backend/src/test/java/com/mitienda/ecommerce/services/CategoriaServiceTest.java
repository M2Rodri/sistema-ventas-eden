package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CategoriaRequest;
import com.mitienda.ecommerce.dto.CategoriaResponse;
import com.mitienda.ecommerce.models.Categoria;
import com.mitienda.ecommerce.models.TipoProducto;
import com.mitienda.ecommerce.repositories.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** El nombre de la categoría es libre; el tipo de producto lo resuelve el servicio. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;

    private CategoriaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CategoriaService(categoriaRepository);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CategoriaRequest pedido(String nombre) {
        CategoriaRequest request = new CategoriaRequest();
        request.setNombre(nombre);
        request.setActivo(true);
        return request;
    }

    @Test
    void unNombreLibre_seCreaSinTipoYQuedaComoAccesorio() {
        CategoriaResponse creada = servicio.createCategoria(pedido("  Sofás  "));

        assertEquals("Sofás", creada.getNombre());
        assertEquals(TipoProducto.ACCESORIO, creada.getTipoProducto());
    }

    @Test
    void unNombreConocido_tomaSuFormaYSuTipo() {
        CategoriaResponse creada = servicio.createCategoria(pedido("camas"));

        assertEquals("Camas", creada.getNombre());
        assertEquals(TipoProducto.CAMA, creada.getTipoProducto());
    }

    @Test
    void veladoresTocadoresRoperosYZapateros_sonMuebles() {
        for (String nombre : new String[]{"veladores", "Tocadores", "ROPEROS", "zapateros"}) {
            CategoriaResponse creada = servicio.createCategoria(pedido(nombre));
            assertEquals(TipoProducto.MUEBLE, creada.getTipoProducto(), nombre);
        }
    }

    @Test
    void alEditarUnNombreLibre_conservaElTipoQueTenia() {
        Categoria existente = new Categoria();
        existente.setNombre("Camas");
        existente.setTipoProducto(TipoProducto.CAMA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));

        CategoriaResponse editada = servicio.updateCategoria(1L, pedido("Camas y literas"));

        assertEquals("Camas y literas", editada.getNombre());
        assertEquals(TipoProducto.CAMA, editada.getTipoProducto());
    }
}
