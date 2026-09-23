'use client';

import React, { useState, useEffect } from 'react';
import { X, User, ShoppingBag, AlertCircle, Eye, Receipt } from 'lucide-react';
import { HistorialComprasResponse, VentaHistorial } from '@/types/cliente';
import { getHistorialCompras } from '@/lib/api';
import DetalleVentaClienteModal from './DetalleVentaClienteModal';

interface HistorialComprasModalProps {
  isOpen: boolean;
  onClose: () => void;
  clienteId: number;
}

/**
 * Historial de compras de un cliente (Interfaz P6.3)
 *
 * Antes esta pantalla mostraba solo los datos del cliente y, debajo, tres
 * cajas grises simulando contenido: nunca consultaba las compras. El endpoint
 * y el modal de detalle ya existían sin conectar.
 */
export default function HistorialComprasModal({
  isOpen,
  onClose,
  clienteId,
}: HistorialComprasModalProps) {
  const [historial, setHistorial] = useState<HistorialComprasResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ventaSeleccionada, setVentaSeleccionada] = useState<VentaHistorial | null>(null);

  useEffect(() => {
    if (isOpen && clienteId) {
      loadHistorial();
    }
  }, [isOpen, clienteId]);

  const loadHistorial = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getHistorialCompras(clienteId);
      setHistorial(data);
    } catch (err: any) {
      setError(err?.message ?? 'No se pudo cargar el historial de compras');
    } finally {
      setLoading(false);
    }
  };

  const formatBs = (monto: number) =>
    `Bs ${Number(monto ?? 0).toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  const formatFecha = (fecha?: string) =>
    fecha ? new Date(fecha).toLocaleDateString('es-BO') : '—';

  if (!isOpen) return null;

  if (loading) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-xl p-8">
          <div className="flex items-center gap-3">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <p className="text-gray-600">Cargando historial de compras...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !historial) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-xl p-8 max-w-md">
          <div className="flex items-start gap-3 mb-6">
            <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={22} />
            <div>
              <p className="font-medium text-gray-900">No se pudo cargar el historial</p>
              <p className="text-sm text-gray-600 mt-1">{error}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-full px-6 py-2.5 bg-gray-100 text-gray-800 rounded-lg hover:bg-gray-200 font-medium transition-colors"
          >
            Cerrar
          </button>
        </div>
      </div>
    );
  }

  const { cliente, ventas, estadisticas } = historial;

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="flex justify-between items-center p-6 border-b border-gray-200 sticky top-0 bg-gradient-to-r from-primary-50 to-primary-100 z-10">
            <div>
              <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                <ShoppingBag className="text-blue-600" size={28} />
                Historial de Compras
              </h2>
              <p className="text-sm text-gray-500 mt-1">
                Cliente: <span className="font-semibold">{cliente.nombreCompleto}</span> (ID: #{cliente.id})
              </p>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X size={24} />
            </button>
          </div>

          <div className="p-6">
            {/* Información del Cliente */}
            <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-start gap-3">
                <User className="text-blue-600 flex-shrink-0 mt-1" size={20} />
                <div className="flex-1">
                  <p className="text-sm font-medium text-blue-900 mb-2">
                    Información del Cliente
                  </p>
                  <div className="grid grid-cols-2 gap-2 text-xs text-gray-700">
                    <div>
                      <span className="font-medium">Teléfono:</span> {cliente.telefono || '—'}
                    </div>
                    {cliente.email && (
                      <div>
                        <span className="font-medium">Email:</span> {cliente.email}
                      </div>
                    )}
                    <div>
                      <span className="font-medium">Tipo:</span>{' '}
                      <span
                        className={`px-2 py-0.5 rounded-full ${
                          cliente.tipoCliente === 'REGISTRADO'
                            ? 'bg-green-100 text-green-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {cliente.tipoCliente}
                      </span>
                    </div>
                    <div>
                      <span className="font-medium">Registro:</span>{' '}
                      {formatFecha(cliente.fechaRegistro)}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Estadísticas */}
            {estadisticas && (
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                  <p className="text-xs text-gray-500 uppercase tracking-wide">Compras</p>
                  <p className="text-xl font-bold text-gray-900 mt-1">
                    {estadisticas.totalCompras ?? 0}
                  </p>
                </div>
                <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                  <p className="text-xs text-gray-500 uppercase tracking-wide">Total gastado</p>
                  <p className="text-xl font-bold text-gray-900 mt-1">
                    {formatBs(estadisticas.montoTotal)}
                  </p>
                </div>
                <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                  <p className="text-xs text-gray-500 uppercase tracking-wide">Ticket promedio</p>
                  <p className="text-xl font-bold text-gray-900 mt-1">
                    {formatBs(estadisticas.ticketPromedio)}
                  </p>
                </div>
                <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                  <p className="text-xs text-gray-500 uppercase tracking-wide">Última compra</p>
                  <p className="text-xl font-bold text-gray-900 mt-1">
                    {formatFecha(estadisticas.ultimaCompra)}
                  </p>
                </div>
              </div>
            )}

            {/* Listado de compras */}
            <h3 className="text-sm font-semibold text-gray-900 mb-3">
              Compras registradas ({ventas?.length ?? 0})
            </h3>

            {!ventas || ventas.length === 0 ? (
              <div className="text-center py-12 bg-gray-50 border border-gray-200 rounded-lg">
                <Receipt size={40} className="mx-auto text-gray-400 mb-3" />
                <p className="text-gray-600 font-medium">Este cliente todavía no tiene compras</p>
                <p className="text-sm text-gray-500 mt-1">
                  Las ventas que se le registren van a aparecer acá.
                </p>
              </div>
            ) : (
              <div className="border border-gray-200 rounded-lg overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Venta</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Fecha</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Productos</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Método</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Monto</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Detalle</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {ventas.map((venta) => (
                        <tr key={venta.id} className="hover:bg-gray-50">
                          <td className="px-4 py-3 text-sm font-medium text-gray-900">#{venta.id}</td>
                          <td className="px-4 py-3 text-sm text-gray-700">{formatFecha(venta.fechaVenta)}</td>
                          <td className="px-4 py-3 text-sm text-gray-700">
                            {venta.detalles?.length ?? 0}{' '}
                            {(venta.detalles?.length ?? 0) === 1 ? 'producto' : 'productos'}
                          </td>
                          <td className="px-4 py-3 text-sm text-gray-700">{venta.metodoPago || '—'}</td>
                          <td className="px-4 py-3 text-sm text-right font-semibold text-gray-900">
                            {formatBs(venta.montoTotal)}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <button
                              onClick={() => setVentaSeleccionada(venta)}
                              className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 text-sm font-medium transition-colors"
                            >
                              <Eye size={16} />
                              Ver
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex justify-end gap-4 p-6 border-t border-gray-200 bg-gray-50">
            <button
              onClick={onClose}
              className="px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 font-medium transition-all shadow-lg"
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>

      {/* Detalle de una compra */}
      {ventaSeleccionada && (
        <DetalleVentaClienteModal
          isOpen={true}
          venta={ventaSeleccionada}
          onClose={() => setVentaSeleccionada(null)}
        />
      )}
    </>
  );
}
