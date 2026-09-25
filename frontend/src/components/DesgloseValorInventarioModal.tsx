'use client';

import { X, DollarSign } from 'lucide-react';
import { Inventario } from '@/types/inventario';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';

interface DesgloseValorInventarioModalProps {
  inventario: Inventario[];
  onClose: () => void;
}

/**
 * Detalle de cómo se arma "Valor Total del Inventario": cantidad × precio de
 * compra de cada producto, para que se pueda verificar a mano en vez de
 * confiar en el número de la tarjeta sin más.
 */
export default function DesgloseValorInventarioModal({ inventario, onClose }: DesgloseValorInventarioModalProps) {
  const filas = inventario
    .map((item) => ({
      ...item,
      subtotal: item.cantidadDisponible * Number(item.precioCompra ?? 0),
    }))
    .sort((a, b) => b.subtotal - a.subtotal);

  const total = filas.reduce((sum, f) => sum + f.subtotal, 0);

  const { scrollContainerRef, tableRef, theadRef, hasOverflow, theadProps } =
    useDragScrollTable([filas]);

  const formatBs = (n: number) => `${n.toLocaleString('es-BO', { minimumFractionDigits: 2 })} Bs`;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-gradient-to-r from-green-50 to-green-100 z-10">
          <div>
            <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
              <DollarSign className="text-green-600" size={24} />
              Valor Total del Inventario
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              Cantidad actual × precio de compra, producto por producto
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X size={24} />
          </button>
        </div>

        <div className="p-6">
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <div className="overflow-x-auto" ref={scrollContainerRef}>
              <table className="min-w-full divide-y divide-gray-200" ref={tableRef}>
                <thead
                  ref={theadRef}
                  className={`bg-gray-50 ${hasOverflow ? 'cursor-grab select-none' : ''}`}
                  {...theadProps}
                >
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase whitespace-nowrap">Producto</th>
                    <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase whitespace-nowrap">Cantidad</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase whitespace-nowrap">Precio compra</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase whitespace-nowrap">Subtotal</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filas.map((f) => (
                    <tr key={f.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="text-sm font-medium text-gray-900">{f.nombreProducto}</div>
                        <div className="text-xs text-gray-500 font-mono">{f.skuProducto}</div>
                      </td>
                      <td className="px-4 py-3 text-sm text-center text-gray-700">{f.cantidadDisponible}</td>
                      <td className="px-4 py-3 text-sm text-right text-gray-700">
                        {f.precioCompra != null ? formatBs(Number(f.precioCompra)) : '—'}
                      </td>
                      <td className="px-4 py-3 text-sm text-right font-semibold text-gray-900">
                        {formatBs(f.subtotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-green-50">
                  <tr>
                    <td colSpan={3} className="px-4 py-3 text-sm font-bold text-gray-900 text-right">
                      Total
                    </td>
                    <td className="px-4 py-3 text-sm font-bold text-green-700 text-right">
                      {formatBs(total)}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>

        <div className="flex justify-end p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="px-6 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
