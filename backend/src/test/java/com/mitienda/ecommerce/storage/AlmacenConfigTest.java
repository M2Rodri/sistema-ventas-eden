package com.mitienda.ecommerce.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenConfigTest {

    private final AlmacenConfig config = new AlmacenConfig();

    private static MockEnvironment entorno(String perfil) {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles(perfil);
        return entorno;
    }

    @Test
    void conLasVariablesSeUsaSupabase() {
        AlmacenArchivos almacen = config.almacenArchivos("https://proyecto.supabase.co", "clave", entorno("prod"));
        assertInstanceOf(AlmacenSupabase.class, almacen);
        assertTrue(almacen.esExterno());
    }

    @Test
    void enProduccionSinLasVariablesNoArrancaYExplicaQueFalta() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> config.almacenArchivos("", "", entorno("prod")));

        assertTrue(error.getMessage().contains("SUPABASE_URL"));
        assertTrue(error.getMessage().contains("SUPABASE_SERVICE_KEY"));
    }

    @Test
    void enDesarrolloSinLasVariablesUsaElDiscoLocal() {
        AlmacenArchivos almacen = config.almacenArchivos("", "", entorno("dev"));
        assertInstanceOf(AlmacenLocal.class, almacen);
        assertFalse(almacen.esExterno());
    }

    @Test
    void enDesarrolloConUnaSolaVariableAvisaQueVanJuntas() {
        assertThrows(IllegalStateException.class, () -> config.almacenArchivos("https://x.supabase.co", "", entorno("dev")));
        assertThrows(IllegalStateException.class, () -> config.almacenArchivos("", "clave", entorno("dev")));
    }
}
