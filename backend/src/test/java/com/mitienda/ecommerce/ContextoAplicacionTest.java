package com.mitienda.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifica que Spring pueda construir la aplicacion entera. */
@SpringBootTest
class ContextoAplicacionTest {
    @Test
    void elContextoDeSpringArranca() {
        // Sin cuerpo a proposito: si el contexto no se puede construir, el test
        // falla antes de llegar aca.
    }
}
