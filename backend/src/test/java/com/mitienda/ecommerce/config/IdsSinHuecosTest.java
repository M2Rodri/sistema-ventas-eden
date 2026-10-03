package com.mitienda.ecommerce.config;

import com.mitienda.ecommerce.models.Cliente;
import com.mitienda.ecommerce.repositories.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Los IDs salen de un contador que se revierte junto con la operación: si algo falla, el número
 * no se pierde y no queda un hueco. Todo corre en transacciones que se revierten, así que no deja
 * datos en la base.
 */
@SpringBootTest
class IdsSinHuecosTest {

    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ClienteRepository clienteRepository;
    @PersistenceContext private EntityManager em;

    private long contador(String tabla) {
        return jdbc.queryForObject("SELECT ultimo FROM contadores_id WHERE tabla = ?", Long.class, tabla);
    }

    private TransactionTemplate conRollback() {
        return new TransactionTemplate(transacciones);
    }

    @Test
    void losNumerosSonConsecutivos_yUnaOperacionRevertidaNoDejaHueco() {
        long antes = contador("ventas");
        GeneradorIdSinHuecos generador = GeneradorIdSinHuecos.para("ventas");

        List<Long> primera = new ArrayList<>();
        conRollback().executeWithoutResult(estado -> {
            SharedSessionContractImplementor sesion = em.unwrap(SharedSessionContractImplementor.class);
            primera.add((Long) generador.generate(sesion, null));
            primera.add((Long) generador.generate(sesion, null));
            estado.setRollbackOnly();
        });

        assertEquals(List.of(antes + 1, antes + 2), primera, "dentro de una operación son consecutivos");
        assertEquals(antes, contador("ventas"), "la operación se revirtió: el contador no avanzó");

        List<Long> segunda = new ArrayList<>();
        conRollback().executeWithoutResult(estado -> {
            SharedSessionContractImplementor sesion = em.unwrap(SharedSessionContractImplementor.class);
            segunda.add((Long) generador.generate(sesion, null));
            estado.setRollbackOnly();
        });
        assertEquals(List.of(antes + 1), segunda, "el siguiente reutiliza el número que no se usó");
    }

    @Test
    void guardarUnaEntidad_tomaElIdDelContador_yAlRevertirNoSePierde() {
        long antes = contador("clientes");
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            conRollback().executeWithoutResult(estado -> {
                Cliente cliente = new Cliente();
                cliente.setNombre("Prueba de numeración");
                ids.add(clienteRepository.saveAndFlush(cliente).getId());
                estado.setRollbackOnly();
            });
        }

        assertEquals(List.of(antes + 1, antes + 1), ids, "cada intento revertido recibe el mismo número");
        assertEquals(antes, contador("clientes"));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM clientes WHERE nombre = 'Prueba de numeración'", Long.class));
    }

    @Test
    void dosOperacionesAlMismoTiempo_nuncaRecibenElMismoNumero() throws Exception {
        long antes = contador("ventas");
        GeneradorIdSinHuecos generador = GeneradorIdSinHuecos.para("ventas");
        java.util.concurrent.CountDownLatch primeraTieneNumero = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch soltarPrimera = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch segundaTermino = new java.util.concurrent.CountDownLatch(1);
        List<Long> numeros = new java.util.concurrent.CopyOnWriteArrayList<>();

        java.util.concurrent.ExecutorService hilos = java.util.concurrent.Executors.newFixedThreadPool(2);
        hilos.submit(() -> conRollback().executeWithoutResult(estado -> {
            SharedSessionContractImplementor sesion = em.unwrap(SharedSessionContractImplementor.class);
            numeros.add((Long) generador.generate(sesion, null));
            primeraTieneNumero.countDown();
            try {
                soltarPrimera.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            estado.setRollbackOnly();
        }));
        primeraTieneNumero.await();
        hilos.submit(() -> {
            conRollback().executeWithoutResult(estado -> {
                SharedSessionContractImplementor sesion = em.unwrap(SharedSessionContractImplementor.class);
                numeros.add((Long) generador.generate(sesion, null));
                estado.setRollbackOnly();
            });
            segundaTermino.countDown();
        });

        // Mientras la primera no termina, la segunda espera: no puede tener otro número.
        assertEquals(false, segundaTermino.await(700, java.util.concurrent.TimeUnit.MILLISECONDS));
        soltarPrimera.countDown();
        assertEquals(true, segundaTermino.await(10, java.util.concurrent.TimeUnit.SECONDS));
        hilos.shutdown();

        // La primera revirtió, así que la segunda reutiliza su número: nunca se repiten dos a la vez.
        assertEquals(List.of(antes + 1, antes + 1), numeros);
        assertEquals(antes, contador("ventas"));
    }
}
