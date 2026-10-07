package com.mitienda.ecommerce.security;

import com.mitienda.ecommerce.exception.DemasiadosIntentosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos fallidos de inicio de sesión, por usuario.
 *
 * Cinco fallos dentro de 15 minutos bloquean ese usuario durante 15 minutos: mientras
 * dure el bloqueo, el login responde 429 DEMASIADOS_INTENTOS sin ni siquiera revisar la
 * contraseña. Un inicio de sesión correcto borra el contador.
 *
 * El contador vive en memoria: se pierde al reiniciar el servidor y no se comparte entre
 * varias copias del backend. Con una sola instancia (Render, hoy) alcanza.
 *
 * Se cuenta por el nombre que se escribió, exista o no el usuario, para que la respuesta
 * no delate qué usuarios existen. El costo es que alguien puede bloquear a propósito a un
 * usuario real escribiendo mal su clave cinco veces; el bloqueo dura solo 15 minutos y
 * queda registrado en la auditoría.
 */
@Component
public class LimiteIntentosLogin {

    public static final int MAX_FALLOS = 5;
    public static final Duration VENTANA = Duration.ofMinutes(15);
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    /** A partir de cuántos usuarios distintos se limpian los registros ya vencidos. */
    private static final int LIMITE_PARA_LIMPIAR = 1000;

    private final Clock reloj;
    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    @Autowired
    public LimiteIntentosLogin() {
        this(Clock.systemDefaultZone());
    }

    /** Con reloj propio, para las pruebas. */
    public LimiteIntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    /** Lanza 429 si el usuario está bloqueado. Se llama antes de revisar la contraseña. */
    public void verificar(String usuario) {
        Estado estado = estados.get(clave(usuario));
        if (estado == null) {
            return;
        }
        Instant ahora = reloj.instant();
        synchronized (estado) {
            if (estado.bloqueadoHasta != null) {
                if (ahora.isBefore(estado.bloqueadoHasta)) {
                    long minutos = Math.max(1, (Duration.between(ahora, estado.bloqueadoHasta).toSeconds() + 59) / 60);
                    throw new DemasiadosIntentosException("DEMASIADOS_INTENTOS",
                            "Demasiados intentos fallidos. Intentá de nuevo en " + minutos
                                    + (minutos == 1 ? " minuto." : " minutos."));
                }
                // El bloqueo terminó: empieza de cero.
                estado.bloqueadoHasta = null;
                estado.fallos.clear();
            }
        }
    }

    /**
     * Anota un intento fallido. Devuelve true si con este fallo el usuario queda bloqueado
     * (para que quien llama lo deje en la auditoría una sola vez).
     */
    public boolean registrarFallo(String usuario) {
        Instant ahora = reloj.instant();
        limpiarVencidosSiHaceFalta(ahora);
        Estado estado = estados.computeIfAbsent(clave(usuario), k -> new Estado());
        synchronized (estado) {
            while (!estado.fallos.isEmpty() && !estado.fallos.peekFirst().isAfter(ahora.minus(VENTANA))) {
                estado.fallos.pollFirst();
            }
            estado.fallos.addLast(ahora);
            if (estado.fallos.size() >= MAX_FALLOS) {
                estado.bloqueadoHasta = ahora.plus(BLOQUEO);
                estado.fallos.clear();
                return true;
            }
            return false;
        }
    }

    /** Un inicio de sesión correcto borra los fallos anteriores. */
    public void limpiar(String usuario) {
        estados.remove(clave(usuario));
    }

    private void limpiarVencidosSiHaceFalta(Instant ahora) {
        if (estados.size() < LIMITE_PARA_LIMPIAR) {
            return;
        }
        estados.entrySet().removeIf(e -> {
            Estado s = e.getValue();
            synchronized (s) {
                boolean bloqueoVigente = s.bloqueadoHasta != null && ahora.isBefore(s.bloqueadoHasta);
                boolean fallosVigentes = !s.fallos.isEmpty() && s.fallos.peekLast().isAfter(ahora.minus(VENTANA));
                return !bloqueoVigente && !fallosVigentes;
            }
        });
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase();
    }

    private static final class Estado {
        private final Deque<Instant> fallos = new ArrayDeque<>();
        private Instant bloqueadoHasta;
    }
}
