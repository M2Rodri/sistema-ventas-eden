// src/app/tienda/productos/page.tsx
'use client';

import React, { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { BACKEND_URL } from '@/lib/api';
// Se usa el tipo compartido en lugar de una copia local: las interfaces
// duplicadas de esta página habían quedado desactualizadas (seguían con
// precioUnitario y sin marca, firmeza ni materialNucleo).
import { Producto, ImagenProducto } from '@/types/producto';
import { useSearchParams } from 'next/navigation';
import { FiFilter, FiShoppingCart } from 'react-icons/fi';
import { MdStar, MdStarBorder, MdStarHalf, MdVisibility, MdViewInAr } from 'react-icons/md';

interface CategoriaResponse {
  id: number;
  nombre: string;
  descripcion?: string;
  cantidadProductos: number;
  activo: boolean;
}

// Componente para cada tarjeta de producto
const ProductoCard = ({ producto }: { producto: Producto }) => {
  const imagenPrincipal = producto.imagenes?.[0]?.urlImagen || '/images/placeholder-imagen.jpg'; // Cambiado a ruta dentro de public/
  const precioVenta = producto.precioVenta;

  const productoHref = `/tienda/productos/${producto.id}`;
  const ar3dHref = `/tienda/visualizacion-3d-ar?id=${producto.id}`;


  // Generar etiquetas dinámicas (ej: Promocion, Nuevo)
  const etiquetas: { texto: string; clase: string }[] = [];
  // Ejemplo: Si el producto tiene un descuento, mostrar "Promocion"
  // Esta lógica dependerá de tu backend o de cómo definas promociones
  // if (producto.tieneDescuento) {
  //   etiquetas.push({ texto: "Promocion", clase: "bg-secondary-600" });
  // }
  // if (producto.esNuevo) {
  //   etiquetas.push({ texto: "Nuevo", clase: "bg-primary-600" });
  // }

  return (
    <div className="group relative flex transform flex-col overflow-hidden rounded-lg bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg border border-gray-200">
      {/* Etiquetas de producto */}
      {etiquetas.length > 0 && (
        <div className="absolute right-3 top-3 z-10 rounded-full px-3 py-1 text-xs font-bold uppercase text-white bg-secondary-600">
          {etiquetas[0].texto}
        </div>
      )}

      {/* Imagen del producto - Clickeable hacia detalles */}
      <Link href={productoHref} className="relative h-60 w-full bg-gray-100 flex items-center justify-center overflow-hidden">
        <img
          src={`${BACKEND_URL}${imagenPrincipal}`} // Cambiado para usar BACKEND_URL y urlImagen
          alt={producto.nombre}
          className="w-full h-full object-cover"
          onError={(e) => {
            const target = e.target as HTMLImageElement;
            target.onerror = null; // Evita bucle infinito si el placeholder también falla
            target.src = '/images/placeholder-imagen.jpg'; // Imagen por defecto si falla, dentro de public/
          }}
        />
      </Link>

      <div className="flex flex-1 flex-col p-5">
        {/* Nombre del producto - Clickeable hacia detalles */}
        <Link href={productoHref} className="text-lg font-semibold text-gray-900 group-hover:text-primary-600 truncate">
          {producto.nombre}
        </Link>

        {/* Descripción corta */}
        <p className="mt-1 flex-grow text-sm text-gray-600 line-clamp-2">{producto.descripcion}</p>

        {/* Precio */}
        <div className="my-4 flex items-baseline gap-2">
          <span className="text-2xl font-bold text-primary-600">Bs. {precioVenta.toFixed(2)}</span>
        </div>


        {/*
          Antes acá había tres afirmaciones fijas — "Garantía 2 años" y
          "12 cuotas sin interés" — que el negocio no ofrece, más una
          calificación de 4.5 estrellas escrita en el código. Se reemplazaron
          por los atributos reales del producto, que son los que el cliente
          necesita para decidir.
        */}
        <div className="my-4 space-y-1.5 text-xs text-gray-600">
          {producto.dimensiones && (
            <p className="flex items-center gap-2">
              <span className="font-medium text-gray-500">Medidas:</span> {producto.dimensiones}
            </p>
          )}
          {producto.firmeza && (
            <p className="flex items-center gap-2">
              <span className="font-medium text-gray-500">Firmeza:</span> {producto.firmeza}
            </p>
          )}
          {producto.materialNucleo && (
            <p className="flex items-center gap-2">
              <span className="font-medium text-gray-500">Núcleo:</span> {producto.materialNucleo}
            </p>
          )}
          {producto.marca && (
            <p className="flex items-center gap-2">
              <span className="font-medium text-gray-500">Marca:</span> {producto.marca}
            </p>
          )}
        </div>

        {/* Botones de acción */}
        <div className="mt-auto grid grid-cols-2 gap-3 pt-2">
          <Link href={ar3dHref} className="flex h-10 items-center justify-center gap-2 rounded-lg bg-secondary-600 text-sm font-bold text-white transition-transform hover:scale-105">
            <MdViewInAr className="text-base" /> 3D/AR
          </Link>
          {/* <button className="flex h-10 items-center justify-center gap-2 rounded-lg bg-primary-600 text-sm font-bold text-white transition-transform hover:scale-105">
            <FiShoppingCart className="text-base" /> Añadir
          </button> */}
        </div>
      </div>
    </div>
  );
};

// Componente para el sidebar de filtros
const SidebarFiltros = ({ categorias, onFiltrosChange }: { categorias: CategoriaResponse[], onFiltrosChange: (categorias: string[], texto: string) => void }) => {
  const [categoriasSeleccionadas, setCategoriasSeleccionadas] = useState<string[]>([]);
  const [textoBusqueda, setTextoBusqueda] = useState<string>('');

  const toggleCategoria = (nombre: string) => {
    setCategoriasSeleccionadas(prev =>
      prev.includes(nombre)
        ? prev.filter(c => c !== nombre)
        : [...prev, nombre]
    );
  };

  const aplicarFiltros = () => {
    onFiltrosChange(categoriasSeleccionadas, textoBusqueda);
  };

  const limpiarFiltros = () => {
    setCategoriasSeleccionadas([]);
    setTextoBusqueda('');
    onFiltrosChange([], ''); // Notificar al padre que se limpió
  };

  // Iconos por categoría (puedes hacer un mapping más complejo)
  const getCategoriaIcon = (nombre: string) => {
    switch (nombre.toLowerCase()) {
      case 'camas': return '🛏️';
      case 'colchones': return '🛏️';
      case 'almohadas': return '🛏️';
      default: return '📦';
    }
  };

  return (
    <aside className="col-span-1">
      <div className="sticky top-24 rounded-lg bg-white p-6 shadow-sm border border-gray-200">
        {/* Encabezado de filtros */}
        <div className="mb-6 flex items-center gap-3 border-b pb-4">
          <FiFilter className="text-gray-700 text-xl" />
          <h2 className="text-xl font-bold leading-tight tracking-tight text-gray-900">Filtrar Productos</h2>
        </div>

        {/* Filtros de Categorías */}
        <div className="space-y-4 border-b pb-6">
          <h3 className="font-semibold text-gray-900">Categorías</h3>
          {categorias.map((categoria) => (
            <label key={categoria.id} className="flex cursor-pointer items-center gap-x-3">
              <input
                type="checkbox"
                className="h-5 w-5 rounded border-gray-300 text-primary-600 focus:ring-primary-600"
                checked={categoriasSeleccionadas.includes(categoria.nombre)}
                onChange={() => toggleCategoria(categoria.nombre)}
              />
              <span className="text-gray-500">{getCategoriaIcon(categoria.nombre)}</span>
              <p className="flex-grow text-base font-normal leading-normal text-gray-900">{categoria.nombre}</p>
              <span className="text-sm text-gray-500">({categoria.cantidadProductos})</span>
            </label>
          ))}
        </div>

        {/* Filtro de texto */}
        <div className="space-y-4 border-b py-6">
          <h3 className="font-semibold text-gray-900">Buscar</h3>
          <input
            type="text"
            placeholder="Nombre o descripción..."
            className="w-full p-2 border border-gray-300 rounded"
            value={textoBusqueda}
            onChange={(e) => setTextoBusqueda(e.target.value)}
          />
        </div>

        {/* Acciones de filtros */}
        <div className="mt-6 flex flex-col gap-3">
          <button
            onClick={aplicarFiltros}
            className="flex h-11 w-full cursor-pointer items-center justify-center rounded-lg bg-primary-600 text-sm font-bold text-white transition-transform hover:scale-105"
          >
            Aplicar Filtros
          </button>
          <button
            onClick={limpiarFiltros}
            className="flex h-11 w-full cursor-pointer items-center justify-center rounded-lg bg-gray-200 text-sm font-bold text-gray-700 transition-colors hover:bg-gray-300"
          >
            Limpiar
          </button>
        </div>
      </div>
    </aside>
  );
};

const CatalogoContent = () => {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<CategoriaResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filtroTexto, setFiltroTexto] = useState('');
  const searchParams = useSearchParams();
  const categoriaParam = searchParams.get('categoria'); // Ejemplo: ?categoria=camas

  // Cargar productos y categorías
  useEffect(() => {
    const fetchCatalogo = async () => {
      try {
        setLoading(true);
        setError(null); // Limpiar error previo
        // allSettled y no all: los productos son el catálogo y las categorías
        // solo alimentan el filtro lateral. Antes, si fallaba cualquiera de las
        // dos, la tienda entera mostraba el mensaje de error y no se veía ni un
        // producto. Ahora las categorías pueden faltar sin tapar el catálogo.
        //
        // fetch solo rechaza ante un fallo de red, no ante un 500, así que
        // además hay que mirar el .ok de cada respuesta.
        const [rProductos, rCategorias] = await Promise.allSettled([
          fetch(`${BACKEND_URL}/api/productos/activos`),
          fetch(`${BACKEND_URL}/api/categorias/activas`)
        ]);

        if (rProductos.status === 'rejected' || !rProductos.value.ok) {
          throw new Error('No se pudo cargar el catálogo de productos');
        }

        const productosData: Producto[] = await rProductos.value.json();
        setProductos(productosData);

        if (rCategorias.status === 'fulfilled' && rCategorias.value.ok) {
          const categoriasData: CategoriaResponse[] = await rCategorias.value.json();
          setCategorias(categoriasData);
        } else {
          // El catálogo se ve igual; solo queda sin el filtro por categoría.
          console.error('No se pudieron cargar las categorías:',
            rCategorias.status === 'rejected' ? rCategorias.reason : rCategorias.value.status);
          setCategorias([]);
        }
      } catch (err) {
        console.error('Error fetching catalog ', err);
        setError('Error al cargar el catálogo. Por favor, inténtelo de nuevo más tarde.');
        // Opcional: mostrar mensaje de error en UI
      } finally {
        setLoading(false); // Asegurar que loading se apague
      }
    };

    fetchCatalogo();
  }, [categoriaParam, BACKEND_URL]); // Añadir BACKEND_URL como dependencia

  // Aplicar filtros
  const [categoriasFiltro, setCategoriasFiltro] = useState<string[]>([]);

  const aplicarFiltros = (cats: string[], texto: string) => {
    setCategoriasFiltro(cats);
    setFiltroTexto(texto);
  };

  // Filtrar productos basados en criterios
  const productosFiltrados = productos.filter(producto => {
    const coincideTexto = producto.nombre.toLowerCase().includes(filtroTexto.toLowerCase()) || (producto.descripcion ?? "").toLowerCase().includes(filtroTexto.toLowerCase());
    const coincideCategoria = categoriasFiltro.length === 0 || categoriasFiltro.includes(producto.nombreCategoria);

    return coincideTexto && coincideCategoria;
  });

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
          <div className="mx-auto bg-gray-200 border-2 border-dashed rounded-xl w-16 h-16 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
            </svg>
          </div>
          <h1 className="text-3xl font-extrabold text-gray-900">Cargando Catálogo</h1>
          <p className="text-base text-gray-600">Por favor, espere mientras cargamos los productos.</p>
        </div>
      </div>
    );
  }

  if (error) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4 sm:px-6 lg:px-8">
          <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
            <div className="mx-auto bg-red-100 border-2 border-dashed rounded-xl w-16 h-16 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
            </div>
            <h1 className="text-3xl font-extrabold text-gray-900">Error</h1>
            <p className="text-base text-gray-600">{error}</p>
            <button
                onClick={() => window.location.reload()}
                className="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
                Reintentar
            </button>
          </div>
        </div>
      );
  }

  return (
    <div className="py-10">
      {/* Encabezado de la página */}
      <div className="mb-8 text-center">
        <h1 className="text-3xl md:text-4xl lg:text-5xl font-black leading-tight tracking-tighter text-gray-900">Nuestro Catálogo Premium</h1>
        <p className="mt-2 text-base md:text-lg font-normal leading-normal text-gray-600">Encuentra la comodidad perfecta para tu descanso</p>
      </div>

      {/* Barra de búsqueda global */}
      <div className="mb-8 max-w-2xl mx-auto px-4">
        <div className="relative">
          <input
            type="text"
            placeholder="Buscar productos..."
            className="w-full p-4 pr-12 rounded-lg border border-gray-300 shadow-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            value={filtroTexto}
            onChange={(e) => setFiltroTexto(e.target.value)} // Actualiza el estado local
          />
          <button className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </div>
      </div>

      {/* Contenido principal con sidebar y productos */}
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-4 px-4">
        {/* Sidebar de Filtros */}
        <SidebarFiltros categorias={categorias} onFiltrosChange={aplicarFiltros} />

        {/* Grid de Productos */}
        <section className="col-span-1 lg:col-span-3">
          {productosFiltrados.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-600">No se encontraron productos que coincidan con los filtros.</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
                {productosFiltrados.map((producto) => (
                  <ProductoCard key={producto.id} producto={producto} />
                ))}
              </div>

              {/*
                Acá había una paginación simulada: siete enlaces fijos a "#"
                que mostraban "1 2 3 ... 8" sin importar cuántos productos
                hubiera. Con un catálogo de este tamaño no hace falta paginar,
                así que se reemplazó por el conteo real de resultados.
              */}
              <p className="mt-12 text-center text-sm text-gray-500">
                {productosFiltrados.length}{' '}
                {productosFiltrados.length === 1 ? 'producto' : 'productos'}
              </p>
            </>
          )}
        </section>
      </div>
    </div>
  );
};

const CatalogoPage = () => {
  return (
    <Suspense fallback={
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
          <div className="mx-auto bg-gray-200 border-2 border-dashed rounded-xl w-16 h-16 flex items-center justify-center animate-pulse">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
            </svg>
          </div>
          <h1 className="text-3xl font-extrabold text-gray-900">Cargando...</h1>
        </div>
      </div>
    }>
      <CatalogoContent />
    </Suspense>
  );
};

export default CatalogoPage;