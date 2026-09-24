// src/app/tienda/producto/[id]/page.tsx
'use client';

// Agrega useParams a la importación
import { useParams } from 'next/navigation';
import React, { useState, useEffect } from 'react';
import Link from 'next/link';
// Se usa el tipo compartido en lugar de una copia local: las interfaces
// duplicadas de esta página habían quedado desactualizadas (seguían con
// precioUnitario y sin marca, firmeza ni materialNucleo).
import { Producto, ImagenProducto } from '@/types/producto';
import { BACKEND_URL } from '@/lib/api';
import { MdStar, MdStarBorder, MdStarHalf, MdVisibility, MdViewInAr } from 'react-icons/md';
import { FiShoppingCart } from 'react-icons/fi';

// Asumiendo tus DTOs (ajusta según tu frontend)
const DetalleProductoPage = () => {
  // Ahora useParams debería estar disponible
  const { id } = useParams<{ id: string }>(); // Obtener el ID del producto desde la URL
  const [producto, setProducto] = useState<Producto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Antes el onClick de las miniaturas estaba vacío: se veían clickeables
  // y no hacían nada.
  const [imagenSeleccionada, setImagenSeleccionada] = useState<string | null>(null);

  useEffect(() => {
    const fetchProducto = async () => {
      if (!id) return; // Si no hay ID, no hacer nada

      try {
        setLoading(true);
        setError(null);
        const response = await fetch(`${BACKEND_URL}/api/productos/${id}`); // Usar la API para obtener el producto por ID

        if (!response.ok) {
          throw new Error(`Error al cargar el producto: ${response.status} ${response.statusText}`);
        }

        const data: Producto = await response.json();
        setProducto(data);
      } catch (err) {
        console.error('Error fetching product:', err);
        setError((err as Error).message || 'Error al cargar el producto.');
      } finally {
        setLoading(false);
      }
    };

    fetchProducto();
  }, [id, BACKEND_URL]); // Dependencias: id y BACKEND_URL

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-16rem)]">
        <p>Cargando producto...</p>
      </div>
    );
  }

  if (error || !producto) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-16rem)]">
        <div className="text-center">
          <p className="text-red-500">Error: {error || 'Producto no encontrado'}</p>
          <Link href="/tienda/productos" className="mt-4 inline-block text-blue-500 hover:underline">
            Volver al Catálogo
          </Link>
        </div>
      </div>
    );
  }

  // Asumiendo que la primera imagen es la principal, o filtrar por esPrincipal
  const imagenPorDefecto = producto.imagenes?.find(img => img.esPrincipal)?.urlImagen
    || producto.imagenes?.[0]?.urlImagen
    || '/images/placeholder-imagen.jpg';
  const imagenPrincipal = imagenSeleccionada ?? imagenPorDefecto;

  return (
    <div className="container mx-auto px-4 py-10">
      {/* Botón para volver al catálogo */}
      <Link href="/tienda/productos" className="text-blue-500 hover:underline mb-4 inline-block">
        &larr; Volver al Catálogo
      </Link>

      {/* Contenedor principal del producto */}
      <div className="bg-white rounded-xl shadow-lg overflow-hidden">
        {/* Sección de imágenes y detalles */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 p-6">
          {/* Galería de Imágenes */}
          <div>
            <img
              src={`${BACKEND_URL}${imagenPrincipal}`}
              alt={producto.nombre}
              className="w-full h-auto object-cover rounded-lg"
            />
            {/* Mostrar otras imágenes si existen */}
            {producto.imagenes && producto.imagenes.length > 1 && (
              <div className="mt-4 grid grid-cols-4 gap-2">
                {producto.imagenes.map((img, index) => (
                  <img
                    key={img.id}
                    src={`${BACKEND_URL}${img.urlImagen}`}
                    alt={`Imagen ${index + 1} de ${producto.nombre}`}
                    className={`w-full h-24 object-cover rounded cursor-pointer border-2 transition-colors ${
                      imagenPrincipal === img.urlImagen ? 'border-primary-600' : 'border-gray-200 hover:border-gray-400'
                    }`}
                    onClick={() => setImagenSeleccionada(img.urlImagen)}
                  />
                ))}
              </div>
            )}
          </div>

          {/* Detalles del Producto */}
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{producto.nombre}</h1>
            <p className="text-lg text-gray-600 mt-1">{producto.nombreCategoria}</p>
            <p className="text-xl font-semibold text-primary-600 mt-2">Bs. {producto.precioVenta.toFixed(2)}</p>

            <div className="mt-4">
              <h2 className="text-lg font-semibold text-gray-800">Descripción</h2>
              <p className="text-gray-600 mt-1">{producto.descripcion}</p>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-2 text-sm text-gray-600">
              <p><span className="font-medium">SKU:</span> {producto.sku}</p>
              <p><span className="font-medium">Tipo:</span> {producto.tipoProducto}</p>
              {producto.modelo && <p><span className="font-medium">Modelo:</span> {producto.modelo}</p>}
              {producto.calidad && <p><span className="font-medium">Calidad:</span> {producto.calidad}</p>}
              {producto.dimensiones && <p><span className="font-medium">Dimensiones:</span> {producto.dimensiones}</p>}
              {producto.marca && <p><span className="font-medium">Marca:</span> {producto.marca}</p>}
              {producto.firmeza && <p><span className="font-medium">Firmeza:</span> {producto.firmeza}</p>}
              {producto.materialNucleo && <p><span className="font-medium">Material del núcleo:</span> {producto.materialNucleo}</p>}
            </div>

            {/* Botones de acción */}
            <div className="mt-6 flex flex-col sm:flex-row gap-3">
              <Link href={`/tienda/visualizacion-3d-ar?id=${producto.id}`} className="flex h-12 items-center justify-center gap-2 rounded-lg bg-secondary-600 text-sm font-bold text-white transition-transform hover:scale-105">
                <MdViewInAr className="text-base" /> Ver en 3D/AR
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DetalleProductoPage;