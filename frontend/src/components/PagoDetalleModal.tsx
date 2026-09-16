'use client';

import { Pago, MetodoPago, EstadoPago } from '@/types/pago';
import { Venta } from '@/types/venta';
import { X, CreditCard, Calendar, CheckCircle, Clock, XCircle, User, Phone, Banknote, Smartphone, DollarSign } from 'lucide-react';

interface PagoDetalleModalProps {
  pago: Pago;
  venta: Venta | null;
  onClose: () => void;
}

export default function PagoDetalleModal({ pago, venta, onClose }: PagoDetalleModalProps) {
  const getEstadoIcon = (estado: EstadoPago) => {
    const icons = {
      COMPLETADO: <CheckCircle size={24} className="text-green-600" />,
      PENDIENTE: <Clock size={24} className="text-yellow-600" />,
      RECHAZADO: <XCircle size={24} className="text-red-600" />,
    };
    return icons[estado];
  };

  const getEstadoColor = (estado: EstadoPago) => {
    const colors = {
      COMPLETADO: 'bg-green-100 text-green-800 border-green-300',
      PENDIENTE: 'bg-yellow-100 text-yellow-800 border-yellow-300',
      RECHAZADO: 'bg-red-100 text-red-800 border-red-300',
    };
    return colors[estado];
  };

  const getMetodoPagoIcon = (metodo: MetodoPago) => {
    const icons = {
      EFECTIVO: <Banknote size={24} className="text-green-600" />,
      TRANSFERENCIA: <DollarSign size={24} className="text-purple-600" />,
      QR: <Smartphone size={24} className="text-orange-600" />,
    };
    return icons[metodo];
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(value);
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center">
              <CreditCard className="text-primary-600" size={20} />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">Detalle del Pago</h2>
              <p className="text-sm text-gray-500 font-mono">ID: #{pago.id}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* Estado del Pago */}
          <div className="bg-gray-50 rounded-lg p-4 border-2 border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Estado del Pago</h3>
            <div className={`flex items-center gap-3 px-4 py-3 rounded-lg border-2 ${getEstadoColor(pago.estado)}`}>
              {getEstadoIcon(pago.estado)}
              <span className="font-bold text-lg">{pago.estado}</span>
            </div>
          </div>

          {/* Información del Pago */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <CreditCard size={16} />
              Información del Pago
            </h3>
            <div className="space-y-3">
              <div className="flex justify-between items-center py-2 border-b border-gray-100">
                <span className="text-sm text-gray-600">Monto Pagado:</span>
                <span className="text-xl font-bold text-primary-600">{formatCurrency(pago.monto)}</span>
              </div>
              
              <div className="flex justify-between items-center py-2 border-b border-gray-100">
                <span className="text-sm text-gray-600">Método de Pago:</span>
                <div className="flex items-center gap-2">
                  {getMetodoPagoIcon(pago.metodoPago)}
                  <span className="font-medium text-gray-900">{pago.metodoPago}</span>
                </div>
              </div>

              <div className="flex justify-between items-center py-2 border-b border-gray-100">
                <span className="text-sm text-gray-600 flex items-center gap-1">
                  <Calendar size={14} />
                  Fecha de Pago:
                </span>
                <span className="font-medium text-gray-900">{formatDateTime(pago.fechaPago)}</span>
              </div>

              {pago.referencia && (
                <div className="flex justify-between items-center py-2">
                  <span className="text-sm text-gray-600">Referencia:</span>
                  <span className="font-mono text-sm font-medium text-gray-900">{pago.referencia}</span>
                </div>
              )}
            </div>
          </div>

          {/* Información de la Venta */}
          {venta && (
            <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
              <h3 className="text-sm font-semibold text-blue-900 mb-3">📦 Información de la Venta</h3>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-blue-800">Número de Venta:</span>
                  <span className="text-sm font-bold text-blue-900">#{venta.id}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-blue-800">Total de la Venta:</span>
                  <span className="text-sm font-bold text-blue-900">{formatCurrency(venta.montoTotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-blue-800">Estado:</span>
                  <span className="text-sm font-medium text-blue-900">{venta.estado}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-blue-800">Fecha de Venta:</span>
                  <span className="text-sm text-blue-900">{new Date(venta.fechaVenta).toLocaleDateString('es-BO')}</span>
                </div>
              </div>
            </div>
          )}

          {/* Información del Cliente */}
          {venta && (
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <User size={16} />
                Información del Cliente
              </h3>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <User size={16} className="text-gray-400" />
                  <span className="text-sm text-gray-600">Nombre:</span>
                  <span className="text-sm font-medium text-gray-900">{venta.nombreCliente}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Phone size={16} className="text-gray-400" />
                  <span className="text-sm text-gray-600">Celular:</span>
                  <span className="text-sm font-medium text-gray-900">{venta.telefonoCliente}</span>
                </div>
              </div>
            </div>
          )}

          {/* Productos de la Venta */}
          {venta && venta.detalles && venta.detalles.length > 0 && (
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">🛍️ Productos</h3>
              <div className="space-y-2">
                {venta.detalles.map((detalle, index) => (
                  <div key={index} className="flex justify-between items-start py-2 border-b border-gray-100 last:border-0">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-900">{detalle.nombreProducto}</p>
                      <p className="text-xs text-gray-500 font-mono">{detalle.skuProducto}</p>
                      <p className="text-xs text-gray-600 mt-1">
                        Cantidad: {detalle.cantidad} × {formatCurrency(detalle.precioUnitario)}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-bold text-gray-900">{formatCurrency(detalle.subtotal)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Resumen de Montos */}
          {venta && (
            <div className="bg-gradient-to-br from-primary-50 to-primary-100 rounded-lg p-4 border-2 border-primary-200">
              <div className="space-y-2">
                <div className="flex justify-between items-center pb-2 border-b border-primary-300">
                  <span className="text-sm font-medium text-gray-700">Total de la Venta:</span>
                  <span className="text-lg font-bold text-gray-900">{formatCurrency(venta.montoTotal)}</span>
                </div>
                <div className="flex justify-between items-center pb-2 border-b border-primary-300">
                  <span className="text-sm font-medium text-gray-700">Monto Pagado:</span>
                  <span className="text-lg font-bold text-green-600">{formatCurrency(pago.monto)}</span>
                </div>
                <div className="flex justify-between items-center pt-2">
                  <span className="text-sm font-semibold text-gray-700">Saldo Restante:</span>
                  <span className="text-xl font-bold text-primary-600">
                    {formatCurrency(venta.montoTotal - pago.monto)}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
            <button
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors font-medium"
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}