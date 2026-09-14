'use client';

import React from 'react';
import { X, ShoppingCart, User, Calendar, CreditCard, Package, DollarSign } from 'lucide-react';
import { VentaHistorial, DetalleVentaHistorial } from '@/types/cliente';

interface DetalleVentaClienteModalProps {
  isOpen: boolean;
  onClose: () => void;
  /**
   * Antes estaba tipado como `any`, y por eso pasaba desapercibido que se
   * mostraba `venta.cantidadTotalProductos`, un campo que el backend nunca
   * envía: en pantalla salía "undefined unidad(es)".
   */
  venta: VentaHistorial;
}

/**
 * Modal para Ver Detalle Completo de una Venta (Interfaz P6.4)
 * Usado desde el historial de compras del cliente
 */
export default function DetalleVentaClienteModal({
  isOpen,
  onClose,
  venta,
}: DetalleVentaClienteModalProps) {
  if (!isOpen || !venta) return null;

  const formatFecha = (fecha: string) => {
    return new Date(fecha).toLocaleString('es-BO', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-[60] p-4">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
              <ShoppingCart className="text-blue-600" size={28} />
              Detalle de Venta
            </h2>
            <p className="text-sm text-gray-500 mt-1">
              Información completa de la venta #{venta.id}
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={24} />
          </button>
        </div>

        <div className="p-6">
          {/* Información general de la venta */}
          <div className="mb-6 p-6 bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-xs font-medium text-gray-600 uppercase mb-1 flex items-center gap-1">
                  <ShoppingCart size={12} /> Número de Venta
                </p>
                <p className="text-2xl font-bold text-blue-600">#{venta.id}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-600 uppercase mb-1 flex items-center gap-1">
                  <Calendar size={12} /> Fecha y Hora
                </p>
                <p className="text-base font-semibold text-gray-900">{formatFecha(venta.fechaVenta)}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-600 uppercase mb-1 flex items-center gap-1">
                  <CreditCard size={12} /> Método de Pago
                </p>
                <span className="inline-block px-3 py-1 text-sm font-semibold bg-green-100 text-green-800 rounded-full">
                  {venta.metodoPago}
                </span>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-600 uppercase mb-1 flex items-center gap-1">
                  <User size={12} /> Vendedor
                </p>
                <p className="text-base font-semibold text-gray-900">{venta.nombreUsuario}</p>
              </div>
            </div>
          </div>

          {/* Productos vendidos */}
          <div className="mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Package size={20} className="text-gray-600" />
              Productos Vendidos
            </h3>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">
                      Producto
                    </th>
                    <th className="px-4 py-3 text-center text-xs font-medium text-gray-700 uppercase">
                      Cantidad
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-700 uppercase">
                      Precio Unitario
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-700 uppercase">
                      Subtotal
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {venta.detalles.map((detalle: any, index: number) => (
                    <tr key={detalle.id || index} className="hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <p className="font-medium text-gray-900">{detalle.nombreProducto}</p>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full font-semibold">
                          {detalle.cantidad}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-gray-900">
                        Bs. {detalle.precioUnitario.toFixed(2)}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-gray-900">
                        Bs. {detalle.subtotal.toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Totales */}
          <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-lg p-6">
            <div className="flex justify-between items-center">
              <div className="flex items-center gap-3">
                <div className="p-3 bg-green-600 rounded-full">
                  <DollarSign className="text-white" size={24} />
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-600">Total de la Venta</p>
                  <p className="text-xs text-gray-500">
                    {venta.detalles.length} producto(s) ·{' '}
                    {venta.detalles.reduce(
                      (total: number, d: DetalleVentaHistorial) => total + (d.cantidad ?? 0),
                      0
                    )}{' '}
                    unidad(es)
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-4xl font-bold text-green-600">Bs. {venta.montoTotal.toFixed(2)}</p>
              </div>
            </div>
          </div>

          {/* Estado de la venta */}
          {venta.estado && (
            <div className="mt-6 p-4 bg-gray-50 border border-gray-200 rounded-lg flex items-center justify-between">
              <p className="text-xs font-medium text-gray-600 uppercase">Estado de la venta</p>
              <span
                className={`px-3 py-1 rounded-full text-sm font-medium ${
                  venta.estado === 'COMPLETADA'
                    ? 'bg-green-100 text-green-800'
                    : venta.estado === 'CANCELADA'
                      ? 'bg-red-100 text-red-800'
                      : 'bg-yellow-100 text-yellow-800'
                }`}
              >
                {venta.estado}
              </span>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="px-6 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 font-medium transition-colors"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}