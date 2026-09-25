'use client';

import React, { useState, useEffect } from 'react';
import { X, User, Calendar, CreditCard, Package, AlertCircle, FileText, Upload, Image as ImageIcon, Banknote } from 'lucide-react';
import { Venta, Pago } from '@/types/venta';
import { adjuntarComprobantePago, BACKEND_URL } from '@/lib/api';
import ComprobanteModal from '@/components/ComprobanteModal';

interface DetalleVentaModalProps {
  isOpen: boolean;
  onClose: () => void;
  venta: Venta;
  onUpdated?: () => void;
  onCobrarSaldo?: () => void;
}

export default function DetalleVentaModal({ isOpen, onClose, venta, onUpdated, onCobrarSaldo }: DetalleVentaModalProps) {
  const [showComprobanteModal, setShowComprobanteModal] = useState(false);
  const [pagosState, setPagosState] = useState<Pago[]>(venta.pagos);
  const [subiendoId, setSubiendoId] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  // Si venta.pagos cambia (por ejemplo, al cobrar el saldo pendiente desde
  // este mismo modal), este estado local se pone al día.
  useEffect(() => {
    setPagosState(venta.pagos);
  }, [venta.pagos]);

  const tienePagosSinRespaldo = pagosState.some((p) => p.sinRespaldo);

  const handleAdjuntarComprobante = async (idPago: number, file: File) => {
    setUploadError(null);
    setSubiendoId(idPago);
    try {
      const actualizado = await adjuntarComprobantePago(idPago, file);
      setPagosState((prev) =>
        prev.map((p) =>
          p.id === idPago
            ? { ...p, urlComprobante: actualizado.urlComprobante, sinRespaldo: actualizado.sinRespaldo }
            : p,
        ),
      );
      onUpdated?.();
    } catch (err: any) {
      setUploadError(err.message || 'Error al subir el comprobante');
    } finally {
      setSubiendoId(null);
    }
  };

  if (!isOpen) return null;

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(price);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  const getEstadoBadge = () => {
    const colors = {
      COMPLETADA: 'bg-green-100 text-green-800',
      PENDIENTE_PAGO: 'bg-yellow-100 text-yellow-800',
      CANCELADA: 'bg-red-100 text-red-800',
    };
    const labels = {
      COMPLETADA: 'Completada',
      PENDIENTE_PAGO: 'Pendiente Pago',
      CANCELADA: 'Cancelada',
    };
    return { color: colors[venta.estado], label: labels[venta.estado] };
  };

  const getMetodoPagoIcon = () => {
    const icons: Record<string, string> = {
      EFECTIVO: '💵',
      TRANSFERENCIA: '🏦',
      QR: '📱',
    };
    // metodoPago puede venir vacío (venta sin cobros) o como "VARIOS"
    // cuando la venta se pagó con más de un método.
    return icons[venta.metodoPago ?? ''] || '💰';
  };

  const getMetodoPagoLabel = (metodo: string) => {
    const labels: Record<string, string> = {
      EFECTIVO: 'Efectivo',
      TRANSFERENCIA: 'Transferencia',
      QR: 'QR',
    };
    return labels[metodo] || metodo;
  };

  const handleVerComprobante = () => {
    setShowComprobanteModal(true);
  };

  // Antes había acá dos botones simulados: "Imprimir Comprobante" y
  // "Descargar PDF", que solo mostraban un alert. Se quitaron porque
  // "Ver Comprobante" ya abre el comprobante real, y desde ahí se imprime
  // o se guarda como PDF con el diálogo del navegador.
  //
  // También había una observación inventada que aparecía en toda venta cuyo
  // id fuera múltiplo de 3. La tabla ventas no tiene columna de
  // observaciones, así que la sección se eliminó.

  const estadoBadge = getEstadoBadge();

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Detalle de Venta</h2>
              <p className="text-sm text-gray-600 mt-1">
                ID: <span className="font-bold text-blue-600">#{venta.id}</span>
              </p>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X size={24} />
            </button>
          </div>

          {/* Información General */}
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Cliente */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <User className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Cliente</h3>
              </div>
              <p className="text-gray-900 font-medium">{venta.nombreCliente}</p>
              <p className="text-sm text-gray-600">{venta.telefonoCliente || 'Sin teléfono'}</p>
            </div>

            {/* Vendedor */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <User className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Vendedor</h3>
              </div>
              <p className="text-gray-900 font-medium">{venta.nombreUsuario}</p>
            </div>

            {/* Fecha */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <Calendar className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Fecha de Venta</h3>
              </div>
              <p className="text-gray-900">{formatDate(venta.fechaVenta)}</p>
            </div>

            {/* Método de Pago */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <CreditCard className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Método de Pago</h3>
              </div>
              <p className="text-gray-900 flex items-center gap-2">
                <span>{getMetodoPagoIcon()}</span>
                {getMetodoPagoLabel(venta.metodoPago ?? "")}
              </p>
            </div>
          </div>

          {/* Estado */}
          <div className="px-6 pb-6">
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Estado de la Venta:</span>
                <span className={`px-4 py-1.5 rounded-full text-sm font-semibold ${estadoBadge.color}`}>
                  {estadoBadge.label}
                </span>
              </div>
              {venta.estado === 'PENDIENTE_PAGO' && (venta.saldoPendiente ?? 0) > 0 && (
                <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-200">
                  <span className="text-sm font-medium text-gray-700">Saldo pendiente:</span>
                  <span className="text-lg font-bold text-red-600">
                    {formatPrice(venta.saldoPendiente ?? 0)}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Aviso: pagos sin respaldo */}
          {tienePagosSinRespaldo && (
            <div className="px-6 pb-6">
              <div className="bg-orange-50 border border-orange-200 p-4 rounded-lg flex items-start gap-3">
                <AlertCircle className="text-orange-600 flex-shrink-0" size={20} />
                <p className="text-sm text-orange-800">
                  Esta venta tiene pagos sin respaldo: falta la foto del comprobante de un pago
                  por QR o transferencia. Se puede adjuntar más abajo, en "Pagos Registrados".
                </p>
              </div>
            </div>
          )}



          {/* Detalle de Productos */}
          <div className="px-6 pb-6">
            <div className="flex items-center gap-2 mb-4">
              <Package className="text-blue-600" size={20} />
              <h3 className="font-semibold text-gray-900">Productos</h3>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    {/* NUEVA COLUMNA: Código */}
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Código</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                    <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Precio Unit.</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Subtotal</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {venta.detalles.map((detalle, index) => (
                    <tr key={index}>
                      {/* NUEVA COLUMNA: Código del producto */}
                      <td className="px-4 py-3">
                        <span className="text-xs font-mono bg-gray-100 px-2 py-1 rounded text-gray-700">
                          {detalle.skuProducto}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="text-sm font-medium text-gray-900">{detalle.nombreProducto}</div>
                      </td>
                      <td className="px-4 py-3 text-center text-sm text-gray-900">{detalle.cantidad}</td>
                      <td className="px-4 py-3 text-right text-sm text-gray-900">{formatPrice(detalle.precioUnitario)}</td>
                      <td className="px-4 py-3 text-right text-sm font-semibold text-gray-900">
                        {formatPrice(detalle.subtotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Total */}
          <div className="px-6 pb-6">
            <div className="bg-blue-50 border-2 border-blue-200 p-4 rounded-lg">
              <div className="flex justify-between items-center">
                <span className="text-lg font-bold text-gray-900">TOTAL:</span>
                <span className="text-3xl font-bold text-blue-600">{formatPrice(venta.montoTotal)}</span>
              </div>
            </div>
          </div>

          {/* Pagos registrados */}
          {pagosState && pagosState.length > 0 && (
            <div className="px-6 pb-6">
              <div className="flex items-center gap-2 mb-4">
                <CreditCard className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Pagos Registrados</h3>
              </div>
              {venta.estado === 'CANCELADA' && (
                <div className="mb-3 p-3 bg-gray-100 border border-gray-300 rounded-lg text-sm text-gray-600">
                  Esta venta está cancelada: los pagos de abajo ya no son válidos, quedan solo como registro histórico.
                </div>
              )}
              {uploadError && (
                <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
                  {uploadError}
                </div>
              )}
              <div className="space-y-3">
                {pagosState.map((pago, index) => (
                  <div key={index} className={`border p-3 rounded-lg ${
                    venta.estado === 'CANCELADA'
                      ? 'bg-gray-50 border-gray-200 opacity-60'
                      : 'bg-green-50 border-green-200'
                  }`}>
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="text-sm font-medium text-gray-900 flex items-center gap-2">
                          {getMetodoPagoLabel(pago.metodoPago)}
                          {pago.sinRespaldo && (
                            <span className="px-2 py-0.5 text-xs bg-orange-100 text-orange-700 rounded-full">
                              Sin respaldo
                            </span>
                          )}
                        </p>
                        {pago.referencia && (
                          <p className="text-xs text-gray-600">Ref: {pago.referencia}</p>
                        )}
                        <p className="text-xs text-gray-500">
                          {formatDate(pago.fechaPago)}
                          {pago.nombreUsuario && ` · Registrado por ${pago.nombreUsuario}`}
                        </p>
                      </div>
                      <span className={`text-lg font-bold ${venta.estado === 'CANCELADA' ? 'text-gray-500 line-through' : 'text-green-700'}`}>
                        {formatPrice(pago.monto)}
                      </span>
                    </div>

                    <div className="mt-2 pt-2 border-t border-green-200 flex items-center gap-3">
                      {pago.urlComprobante ? (
                        <a
                          href={`${BACKEND_URL}${pago.urlComprobante}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800"
                        >
                          <ImageIcon size={14} /> Ver comprobante
                        </a>
                      ) : (
                        <p className="text-xs text-gray-400">Sin comprobante adjunto</p>
                      )}

                      <label className="flex items-center gap-1 text-xs text-gray-600 hover:text-gray-900 cursor-pointer ml-auto">
                        <Upload size={14} />
                        {subiendoId === pago.id
                          ? 'Subiendo...'
                          : pago.urlComprobante
                            ? 'Reemplazar'
                            : 'Adjuntar comprobante'}
                        <input
                          type="file"
                          accept=".jpg,.jpeg,.png,.webp"
                          className="hidden"
                          disabled={subiendoId === pago.id}
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) handleAdjuntarComprobante(pago.id, file);
                            e.target.value = '';
                          }}
                        />
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Botones */}
          <div className="flex gap-3 p-6 border-t border-gray-200 bg-gray-50">
            
            {/* El comprobante se genera para toda venta al registrarla, sin
                importar el estado: documenta qué se vendió, no si está
                pagada. Por eso este botón no depende del estado. */}
            <button
              onClick={handleVerComprobante}
              className="flex items-center gap-2 px-4 py-2.5 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors font-medium"
            >
              <FileText size={20} />
              Ver Comprobante
            </button>

            {venta.estado === 'PENDIENTE_PAGO' && (venta.saldoPendiente ?? 0) > 0 && onCobrarSaldo && (
              <button
                onClick={onCobrarSaldo}
                className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
              >
                <Banknote size={20} />
                Cobrar saldo pendiente
              </button>
            )}

            <button
              onClick={onClose}
              className="flex-1 px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>

      {/* Modal de Comprobante */}
      {showComprobanteModal && (
        <ComprobanteModal
          isOpen={showComprobanteModal}
          onClose={() => setShowComprobanteModal(false)}
          idVenta={venta.id}
        />
      )}
    </>
  );
}