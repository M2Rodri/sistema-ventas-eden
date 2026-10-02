'use client';

import { useEffect, useState } from 'react';
import { X, Package } from 'lucide-react';
import { Producto } from '@/types/producto';
import { getInventarioByProducto, urlArchivo } from '@/lib/api';

interface ProductoDetalleModalProps {
  producto: Producto;
  onClose: () => void;
}

/**
 * Ficha de un producto, solo para mirar: sin editar nada. Se abre desde Compras (al elegir
 * productos y desde el detalle de una compra) para distinguir un producto de otro parecido.
 */
export default function ProductoDetalleModal({ producto, onClose }: ProductoDetalleModalProps) {
  const [stock, setStock] = useState<number | null>(null);

  useEffect(() => {
    let activo = true;
    getInventarioByProducto(producto.id)
      .then((inv) => activo && setStock(inv.cantidadDisponible))
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, [producto.id]);

  const bs = (n?: number) =>
    `Bs ${Number(n ?? 0).toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  const filas: [string, string | undefined][] = [
    ['SKU', producto.sku],
    ['Categoría', producto.nombreCategoria],
    ['Marca', producto.marca],
    ['Modelo', producto.modelo],
    ['Calidad', producto.calidad],
    ['Color', producto.color],
    ['Firmeza', producto.firmeza],
    ['Material del núcleo', producto.materialNucleo],
    ['Material del armazón', producto.materialArmazon],
    ['Medida', producto.dimensiones],
    ['Precio de venta', bs(producto.precioVenta)],
    ['Último costo de compra', producto.precioCompra != null ? bs(producto.precioCompra) : undefined],
    ['Stock actual', stock != null ? String(stock) : undefined],
  ];

  const imagen = producto.imagenes?.[0];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[70] p-3">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden">
        <div className="flex items-center justify-between px-5 py-2.5 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 shrink-0">
          <h2 className="text-base font-bold text-gray-900">{producto.nombre}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" title="Cerrar">
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
          {imagen ? (
            <img
              src={urlArchivo(imagen.urlImagen)}
              alt={producto.nombre}
              className="w-full max-h-48 object-contain rounded-lg bg-gray-50"
            />
          ) : (
            <div className="flex items-center justify-center h-24 rounded-lg bg-gray-50 text-gray-300">
              <Package size={36} />
            </div>
          )}

          <dl className="grid grid-cols-2 gap-x-4 gap-y-2.5">
            {filas
              .filter(([, valor]) => valor)
              .map(([titulo, valor]) => (
                <div key={titulo}>
                  <dt className="text-xs text-gray-500">{titulo}</dt>
                  <dd className="text-sm font-medium text-gray-900 break-words">{valor}</dd>
                </div>
              ))}
          </dl>

          {producto.descripcion && (
            <p className="text-sm text-gray-600 border-t border-gray-100 pt-3">{producto.descripcion}</p>
          )}
        </div>

        <div className="flex justify-end px-5 py-2.5 border-t border-gray-200 shrink-0">
          <button
            onClick={onClose}
            className="px-4 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
