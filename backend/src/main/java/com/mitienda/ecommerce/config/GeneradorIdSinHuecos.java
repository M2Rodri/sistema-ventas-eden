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
 * Genera el ID sumando 1 al contador de la tabla (contadores_id, script 31) dentro de la transacción en curso.
 * A diferencia de la secuencia de PostgreSQL, un rollback también revierte el contador, así que no quedan huecos;
 * el UPDATE bloquea la fila del contador, por lo que dos operaciones simultáneas no reciben el mismo número.
 * Uso: @GeneratedValue(generator = "id_ventas") con @GenericGenerator(type = GeneradorIdSinHuecos.class, parameters = @Parameter(name = "tabla", value = "ventas")).
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
