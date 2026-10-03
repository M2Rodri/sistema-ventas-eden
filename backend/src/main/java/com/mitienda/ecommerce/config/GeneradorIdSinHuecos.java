package com.mitienda.ecommerce.config;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * Genera el ID sumando 1 al contador de la tabla (contadores_id), con la conexión y la
 * transacción de la operación en curso. Se usa en las entidades así:
 * <pre>
 * &#64;Id
 * &#64;GeneratedValue(generator = "id_ventas")
 * &#64;GenericGenerator(name = "id_ventas", type = GeneradorIdSinHuecos.class,
 *         parameters = &#64;Parameter(name = "tabla", value = "ventas"))
 * </pre>
 *
 * La secuencia de PostgreSQL no devuelve el número si la operación se revierte, y por eso dejaba
 * huecos (1, 2, 4...). El contador sí:
 * - Si la operación falla y se revierte, el contador también: el número no se pierde.
 * - El UPDATE deja la fila del contador bloqueada hasta que la transacción termina, así dos
 *   operaciones simultáneas sobre la misma tabla nunca reciben el mismo número: la segunda espera
 *   a que la primera confirme o se revierta.
 * - Cada tabla tiene su propio contador y las operaciones guardan siempre en el mismo orden, así
 *   que no se bloquean entre sí.
 *
 * Necesita la tabla contadores_id (script 31_ids_sin_huecos.sql).
 */
public class GeneradorIdSinHuecos implements IdentifierGenerator {

    private String tabla;

    /** Hibernate (con Spring) lo crea sin argumentos y le pasa la tabla en {@link #configure}. */
    public GeneradorIdSinHuecos() {
    }

    /** Para pruebas: un generador ya apuntado a una tabla. */
    public static GeneradorIdSinHuecos para(String tabla) {
        GeneradorIdSinHuecos generador = new GeneradorIdSinHuecos();
        generador.tabla = tabla;
        return generador;
    }

    @Override
    public void configure(Type tipo, Properties parametros, ServiceRegistry servicios) throws MappingException {
        this.tabla = parametros.getProperty("tabla");
        if (tabla == null || tabla.isBlank()) {
            throw new MappingException("GeneradorIdSinHuecos necesita el parámetro 'tabla'");
        }
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner) {
        return session.doReturningWork(conexion -> {
            try (PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE contadores_id SET ultimo = ultimo + 1 WHERE tabla = ? RETURNING ultimo")) {
                consulta.setString(1, tabla);
                try (ResultSet resultado = consulta.executeQuery()) {
                    if (!resultado.next()) {
                        throw new IllegalStateException("Falta el contador de IDs de la tabla '" + tabla
                                + "'. Hay que correr el script 31_ids_sin_huecos.sql.");
                    }
                    return resultado.getLong(1);
                }
            }
        });
    }
}
