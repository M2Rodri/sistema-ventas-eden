'use client';

import { X, Building2, Calendar, DollarSign, Package, FileText, User } from 'lucide-react';
import { Compra } from '@/types/proveedor';

interface DetalleCompraModalProps {
  compra: Compra;
  onClose: () => void;
}

export default function DetalleCompraModal({ compra, onClose }: DetalleCompraModalProps) {

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
    });
  };

  const getEstadoBadge = () => {
    const colors: Record<string, string> = {
      PENDIENTE: 'bg-yellow-100 text-yellow-800',
      CONFIRMADA: 'bg-blue-100 text-blue-800',
      EN_TRANSITO: 'bg-purple-100 text-purple-800',
      RECIBIDA: 'bg-green-100 text-green-800',
      CANCELADA: 'bg-red-100 text-red-800',
    };
    const labels: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      EN_TRANSITO: 'En Tránsito',
      RECIBIDA: 'Recibida',
      CANCELADA: 'Cancelada',
    };
    return { 
      color: colors[compra.estado] || 'bg-gray-100 text-gray-800', 
      label: labels[compra.estado] || compra.estado 
    };
  };

  const estadoBadge = getEstadoBadge();

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-secondary-50 to-secondary-100">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Detalle de Compra</h2>
            <p className="text-sm text-gray-600 mt-1">
              ID: #{compra.id}
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
          {/* Proveedor */}
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center gap-2 mb-3">
              <Building2 className="text-secondary-600" size={20} />
              <h3 className="font-semibold text-gray-900">Proveedor</h3>
            </div>
            <p className="text-gray-900 font-medium">{compra.nombreProveedor}</p>
            <p className="text-sm text-gray-600">NIT: {compra.nitProveedor}</p>
          </div>

          {/* Usuario */}
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center gap-2 mb-3">
              <User className="text-secondary-600" size={20} />
              <h3 className="font-semibold text-gray-900">Registrado por</h3>
            </div>
            <p className="text-gray-900 font-medium">{compra.nombreUsuario}</p>
          </div>

          {/* Fecha */}
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center gap-2 mb-3">
              <Calendar className="text-secondary-600" size={20} />
              <h3 className="font-semibold text-gray-900">Fecha de Compra</h3>
            </div>
            <p className="text-gray-900">{formatDate(compra.fechaCompra)}</p>
          </div>

          {/* Costo Total */}
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center gap-2 mb-3">
              <DollarSign className="text-secondary-600" size={20} />
              <h3 className="font-semibold text-gray-900">Costo Total</h3>
            </div>
            <p className="text-2xl font-bold text-secondary-600">{formatPrice(compra.montoTotal)}</p>
          </div>
        </div>

        {/* Estado */}
        <div className="px-6 pb-6">
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-700">Estado de la Compra:</span>
              <span className={`px-4 py-1.5 rounded-full text-sm font-semibold ${estadoBadge.color}`}>
                {estadoBadge.label}
              </span>
            </div>
          </div>
        </div>

        {/* Detalle de Productos */}
        <div className="px-6 pb-6">
          <div className="flex items-center gap-2 mb-4">
            <Package className="text-secondary-600" size={20} />
            <h3 className="font-semibold text-gray-900">Productos Comprados</h3>
          </div>
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                  <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Precio Unit.</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Subtotal</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {compra.detalles.map((detalle, index) => (
                  <tr key={index}>
                    <td className="px-4 py-3">
                      <div className="text-sm font-medium text-gray-900">{detalle.nombreProducto}</div>
                      <div className="text-xs text-gray-500">{detalle.skuProducto}</div>
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
          <div className="bg-secondary-50 border-2 border-secondary-200 p-4 rounded-lg">
            <div className="flex justify-between items-center">
              <span className="text-lg font-bold text-gray-900">COSTO TOTAL:</span>
              <span className="text-3xl font-bold text-secondary-600">{formatPrice(compra.montoTotal)}</span>
            </div>
          </div>
        </div>

        {/* Notas */}
        {compra.notas && (
          <div className="px-6 pb-6">
            <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <FileText className="text-yellow-600" size={18} />
                <h4 className="font-semibold text-gray-900">Notas / Observaciones:</h4>
              </div>
              <p className="text-sm text-gray-700">{compra.notas}</p>
            </div>
          </div>
        )}

        {/* Información de Actualización */}
        <div className="px-6 pb-6">
          <div className="bg-gray-50 p-3 rounded-lg">
            <p className="text-xs text-gray-600">
              Última actualización: {formatDate(compra.fechaActualizacion)}
            </p>
          </div>
        </div>

        {/* Botón */}
        <div className="flex gap-3 p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}