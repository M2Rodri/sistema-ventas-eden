package com.mitienda.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que Spring pueda construir la aplicacion entera.
 *
 * Parece poco, pero cubre una clase de fallo que el compilador no ve: los
 * errores de cableado aparecen recien al arrancar. Un bean que falta, una
 * dependencia circular o una anotacion mal puesta compilan sin una sola queja
 * y explotan cuando se levanta el servidor.
 *
 * Se agrego al pasar toda la inyeccion de campo a inyeccion por constructor:
 * ese cambio no altera lo que compila, pero si altera como Spring arma el
 * grafo de dependencias, y una dependencia circular que antes era tolerada
 * pasa a impedir el arranque. Este test lo detecta sin tener que levantar el
 * servidor a mano cada vez.
 *
 * webEnvironment por defecto es MOCK, asi que no ocupa el puerto 8080 y puede
 * correr con el backend ya andando. Si valida contra la base de datos real,
 * porque ddl-auto=validate compara las entidades contra el esquema al arrancar.
 */
@SpringBootTest
class ContextoAplicacionTest {

    @Test
    void elContextoDeSpringArranca() {
        // Sin cuerpo a proposito: si el contexto no se puede construir, el test
        // falla antes de llegar aca.
    }
}
