// src/app/tienda/page.tsx
'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { getActiveProductos, getActiveCategorias, BACKEND_URL } from '@/lib/api';
import { Producto, Categoria } from '@/types/producto';

/**
 * Portada de la tienda.
 *
 * Antes mostraba tres categorías y cuatro "productos destacados" escritos a
 * mano — "Cama King Size Premium Bs. 4.500", "Colchón Ortopédico Lux
 * Bs. 3.200" — que no existían en el catálogo, y sus enlaces apuntaban a
 * /tienda/producto/1, una ruta inexistente. Ahora todo sale del backend.
 */

const CategoriaCard = ({ nombre, href }: { nombre: string; href: string }) => (
  <Link href={href} className="block group">
    <div className="bg-white rounded-xl shadow-md overflow-hidden transition-transform duration-300 group-hover:shadow-lg group-hover:-translate-y-1">
      <div className="h-40 bg-gradient-to-br from-primary-50 to-secondary-50 flex items-center justify-center">
        <span className="text-4xl">🛏️</span>
      </div>
      <div className="p-4 text-center">
        <h3 className="text-lg font-semibold text-gray-800 group-hover:text-primary-600">{nombre}</h3>
      </div>
    </div>
  </Link>
);

const ProductoCard = ({ producto }: { producto: Producto }) => {
  const principal =
    producto.imagenes?.find((i) => i.esPrincipal) ?? producto.imagenes?.[0];

  return (
    <Link href={`/tienda/productos/${producto.id}`} className="block group">
      <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-200 transition-transform duration-300 group-hover:shadow-md group-hover:-translate-y-1">
        <div className="h-48 bg-gray-100 flex items-center justify-center overflow-hidden">
          {principal ? (
            <img
              src={`${BACKEND_URL}${principal.urlImagen}`}
              alt={producto.nombre}
              className="h-full w-full object-cover"
              onError={(e) => {
                (e.target as HTMLImageElement).src = '/images/placeholder-imagen.jpg';
              }}
            />
          ) : (
            <span className="text-5xl opacity-30">🛏️</span>
          )}
        </div>
        <div className="p-4">
          <span className="text-sm text-gray-500">{producto.nombreCategoria}</span>
          <h3 className="font-semibold text-gray-900 group-hover:text-primary-600 line-clamp-2">
            {producto.nombre}
          </h3>
          <p className="text-lg font-bold text-primary-600 mt-1">
            Bs. {Number(producto.precioVenta).toLocaleString('es-BO', { minimumFractionDigits: 2 })}
          </p>
        </div>
      </div>
    </Link>
  );
};

const TiendaHomePage = () => {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let activo = true;
    (async () => {
      // allSettled y no all: si una sección falla, la otra igual se muestra.
      const [prods, cats] = await Promise.allSettled([
        getActiveProductos(),
        getActiveCategorias(),
      ]);
      if (!activo) return;
      if (prods.status === 'fulfilled') setProductos(prods.value);
      if (cats.status === 'fulfilled') setCategorias(cats.value);
      setCargando(false);
    })();
    return () => {
      activo = false;
    };
  }, []);

  // Se destacan los cuatro productos más caros: son los que mejor representan
  // el catálogo en la portada.
  const destacados = [...productos]
    .sort((a, b) => Number(b.precioVenta) - Number(a.precioVenta))
    .slice(0, 4);

  return (
    <div className="space-y-12 py-8 bg-gradient-to-b from-gray-50 to-gray-100 min-h-[calc(100vh-16rem)]">
      {/* Hero Banner */}
      <section className="relative bg-gradient-to-r from-gray-900 to-gray-700 text-white rounded-2xl overflow-hidden shadow-xl pt-16">
        <div className="absolute inset-0 bg-white bg-opacity-20 backdrop-blur-sm rounded-2xl"></div>
        <div className="relative container mx-auto px-4 sm:px-6 lg:px-8 py-12 md:py-16 flex flex-col items-center text-center">
          <h1 className="text-3xl md:text-5xl font-bold mb-4">Descansa Mejor con Nuestras Camas Premium</h1>
          <p className="text-lg md:text-xl mb-8 max-w-2xl">Entrega en toda Bolivia - Tecnología 3D y AR</p>
          <div className="flex flex-col sm:flex-row gap-4">
            <Link href="/tienda/productos" className="bg-primary-600 hover:bg-primary-700 text-white font-bold py-3 px-8 rounded-full transition-colors duration-300 shadow-lg">
              Ver Catálogo
            </Link>
            <Link href="/tienda/promociones" className="bg-secondary-600 hover:bg-secondary-700 text-white font-bold py-3 px-8 rounded-full transition-colors duration-300 shadow-lg">
              Promociones Especiales
            </Link>
          </div>
        </div>
      </section>

      {/* Categorías */}
      {categorias.length > 0 && (
        <section className="container mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl md:text-3xl font-bold text-center mb-8 text-gray-800">Explora por Categorías</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {categorias.map((categoria) => (
              <CategoriaCard
                key={categoria.id}
                nombre={categoria.nombre}
                href={`/tienda/productos?categoria=${encodeURIComponent(categoria.nombre)}`}
              />
            ))}
          </div>
        </section>
      )}

      {/* Productos Destacados */}
      <section className="container mx-auto px-4 sm:px-6 lg:px-8">
        <h2 className="text-2xl md:text-3xl font-bold text-center mb-8 text-gray-800">Productos Destacados</h2>

        {cargando ? (
          <p className="text-center text-gray-500">Cargando productos…</p>
        ) : destacados.length === 0 ? (
          <p className="text-center text-gray-500">
            Todavía no hay productos publicados en el catálogo.
          </p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {destacados.map((producto) => (
              <ProductoCard key={producto.id} producto={producto} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export default TiendaHomePage;
