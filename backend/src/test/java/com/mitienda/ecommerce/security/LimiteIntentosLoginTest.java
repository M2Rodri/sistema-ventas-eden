package com.mitienda.ecommerce.security;

import com.mitienda.ecommerce.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cinco fallos en 15 minutos bloquean al usuario 15 minutos; un acierto borra la cuenta. */
class LimiteIntentosLoginTest {

    /** Reloj que se mueve a mano, para no esperar 15 minutos de verdad. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-10-07T12:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private RelojManual reloj;
    private LimiteIntentosLogin limite;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        limite = new LimiteIntentosLogin(reloj);
    }

    private void fallar(String usuario, int veces) {
        for (int i = 0; i < veces; i++) {
            limite.registrarFallo(usuario);
        }
    }

    @Test
    void cuatroFallosNoBloquean() {
        fallar("ana", 4);
        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void elQuintoFalloBloqueaYLoAvisaUnaVez() {
        fallar("ana", 4);
        assertTrue(limite.registrarFallo("ana"), "el quinto fallo debe avisar que quedó bloqueado");

        DemasiadosIntentosException error = assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("ana"));
        assertEquals("DEMASIADOS_INTENTOS", error.getCodigo());
        assertEquals(429, error.getEstado().value());
    }

    @Test
    void elBloqueoDuraQuinceMinutos() {
        fallar("ana", 5);

        reloj.avanzar(Duration.ofMinutes(14));
        assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("ana"));

        reloj.avanzar(Duration.ofMinutes(1));
        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void despuesDelBloqueoEmpiezaDeCero() {
        fallar("ana", 5);
        reloj.avanzar(LimiteIntentosLogin.BLOQUEO);
        limite.verificar("ana");

        fallar("ana", 4);
        assertDoesNotThrow(() -> limite.verificar("ana"), "con 4 fallos nuevos todavía no se bloquea");
    }

    @Test
    void losFallosViejosNoSuman() {
        fallar("ana", 4);
        reloj.avanzar(Duration.ofMinutes(16));
        assertFalse(limite.registrarFallo("ana"), "los 4 anteriores ya salieron de la ventana de 15 minutos");
        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void unInicioDeSesionCorrectoBorraLaCuenta() {
        fallar("ana", 4);
        limite.limpiar("ana");
        fallar("ana", 4);
        assertDoesNotThrow(() -> limite.verificar("ana"));
    }

    @Test
    void cadaUsuarioTieneSuPropiaCuenta() {
        fallar("ana", 5);
        assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("ana"));
        assertDoesNotThrow(() -> limite.verificar("luis"));
    }

    @Test
    void mayusculasYEspaciosSonElMismoUsuario() {
        fallar("Ana ", 3);
        fallar("ana", 2);
        assertThrows(DemasiadosIntentosException.class, () -> limite.verificar(" ANA"));
    }

    @Test
    void elMensajeDiceCuantoFalta() {
        fallar("ana", 5);
        reloj.avanzar(Duration.ofMinutes(10));
        DemasiadosIntentosException error = assertThrows(DemasiadosIntentosException.class, () -> limite.verificar("ana"));
        assertTrue(error.getMessage().contains("5 minutos"), error.getMessage());
    }
}
