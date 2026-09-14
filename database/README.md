# Scripts de base de datos

Cada archivo numerado es un cambio al esquema, en el orden en que se aplicó.
No se ejecutan solos: `spring.jpa.hibernate.ddl-auto` está en `validate`, así
que Hibernate nunca modifica la estructura. La base la manejan estos scripts.

Cada uno explica en su cabecera **por qué** se hizo el cambio, no solo qué
hace. Esa es la parte que sirve cuando hay que justificar una decisión meses
después.

## Orden

| # | Qué resuelve |
|---|---|
| 01 | Quita columnas y restricciones que no se usaban |
| 02 | Índices en claves foráneas y restricciones de integridad |
| 03 | Ambigüedades: normaliza roles, saca `productos.stock` duplicado |
| 04 | Corrige valores por omisión que violaban las restricciones nuevas |
| 05 | Tablas de compras a proveedores |
| 06 | Nomenclatura: `correo`→`email`, `celular`→`telefono` |
| 07 | Tipos y restricciones: enteros a BIGINT |
| 08 | Limpieza de `tipo_imagen` |
| 09 | Renombra `users` a `usuarios` |
| 10 | Tabla de mensajes del formulario de contacto |
| 11 | **Cierra el acceso público a Supabase** |
| 12 | Alinea el esquema de Supabase con las entidades |
| 13 | Quita `registros_demo` (ejercicio de otra materia) |
| 14 | Restaura los datos del negocio |
| 15 | Renombra las claves `empresa_*` a `negocio_*` |

Del 01 al 10 se aplicaron sobre la base local. Del 11 al 15 nacieron al migrar
a Supabase, y el 15 además hay que correrlo sobre la base local para dejar las
dos iguales.

## Respaldos

`respaldo_supabase_20260913/` tiene el contenido en CSV de las tablas que
tocaron los scripts 12 y 13, tomado justo antes de modificarlas.

## Dos bases, dos perfiles

- `dev` → PostgreSQL local. Es el perfil por omisión.
- `prod` → Supabase.

```
mvnw spring-boot:run                                  # local
mvnw spring-boot:run -Dspring-boot.run.profiles=prod  # Supabase
```
