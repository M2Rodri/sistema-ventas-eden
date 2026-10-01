'use client';

import { useState } from 'react';
import { registrarPago } from '@/lib/api';
import { PagoRequest, MetodoPago } from '@/types/pago';
import { Venta } from '@/types/venta';
import { X, CreditCard, Search } from 'lucide-react';

interface PagoModalProps {
  ventas: Venta[];
  onClose: () => void;
  onSuccess: () => void;
}

export default function PagoModal({ ventas, onClose, onSuccess }: PagoModalProps) {
  const [formData, setFormData] = useState<PagoRequest>({
    idVenta: 0,
    monto: 0,
    metodoPago: 'EFECTIVO',
    referencia: '',
  });

  const [ventaSeleccionada, setVentaSeleccionada] = useState<Venta | null>(null);
  const [searchVenta, setSearchVenta] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSelectVenta = (venta: Venta) => {
    setVentaSeleccionada(venta);
    setFormData(prev => ({
      ...prev,
      idVenta: venta.id,
      monto: venta.montoTotal
    }));
    setSearchVenta('');
  };

  const ventasFiltradas = ventas.filter(v =>
    v.id.toString().includes(searchVenta) ||
    v.nombreCliente.toLowerCase().includes(searchVenta.toLowerCase())
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.idVenta) {
      setError('Debes seleccionar una venta');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      await registrarPago(formData);
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'monto' ? parseFloat(value) || 0 : value
    }));
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(value);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-3">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-2 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 shrink-0">
          <div className="flex items-center gap-2">
            <CreditCard className="text-primary-600" size={18} />
            <h2 className="text-base font-bold text-gray-900">Registrar Nuevo Pago</h2>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="flex-1 overflow-y-auto px-5 pt-4 pb-3 space-y-3">
          {error && (
            <div className="bg-red-50 text-red-800 p-3 rounded-lg text-sm border border-red-200">
              {error}
            </div>
          )}

          {/* Seleccionar Venta */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Seleccionar Venta <span className="text-red-500">*</span>
            </label>
            
            {!ventaSeleccionada ? (
              <div>
                <div className="relative mb-2">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={18} />
                  <input
                    type="text"
                    placeholder="Buscar por #venta o nombre de cliente..."
                    value={searchVenta}
                    onChange={(e) => setSearchVenta(e.target.value)}
                    className="w-full pl-10 pr-3 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>

                {searchVenta && (
                  <div className="border border-gray-300 rounded-lg max-h-48 overflow-y-auto">
                    {ventasFiltradas.length > 0 ? (
                      ventasFiltradas.map((venta) => (
                        <button
                          key={venta.id}
                          type="button"
                          onClick={() => handleSelectVenta(venta)}
                          className="w-full text-left p-3 hover:bg-gray-50 border-b border-gray-200 last:border-0 transition-colors"
                        >
                          <div className="flex justify-between items-start">
                            <div>
                              <p className="font-semibold text-gray-900">Venta #{venta.id}</p>
                              <p className="text-sm text-gray-600">{venta.nombreCliente}</p>
                              <p className="text-xs text-gray-500">{venta.telefonoCliente}</p>
                            </div>
                            <div className="text-right">
                              <p className="font-bold text-primary-600">{formatCurrency(venta.montoTotal)}</p>
                              <p className="text-xs text-gray-500">{venta.estado}</p>
                            </div>
                          </div>
                        </button>
                      ))
                    ) : (
                      <div className="p-3 text-center text-gray-500 text-sm">
                        No se encontraron ventas
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="bg-primary-50 border-2 border-primary-200 rounded-lg p-3">
                <div className="flex justify-between items-start">
                  <div>
                    <p className="font-semibold text-gray-900">Venta #{ventaSeleccionada.id}</p>
                    <p className="text-sm text-gray-600">{ventaSeleccionada.nombreCliente}</p>
                    <p className="text-xs text-gray-500">{ventaSeleccionada.telefonoCliente}</p>
                    <p className="text-sm text-gray-600 mt-2">
                      Método de venta: <span className="font-medium">{ventaSeleccionada.metodoPago}</span>
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-primary-600 text-base">{formatCurrency(ventaSeleccionada.montoTotal)}</p>
                    <button
                      type="button"
                      onClick={() => {
                        setVentaSeleccionada(null);
                        setFormData(prev => ({ ...prev, idVenta: 0, monto: 0 }));
                      }}
                      className="text-xs text-red-600 hover:text-red-800 mt-2 underline"
                    >
                      Cambiar venta
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {/* Monto */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Monto a Pagar (Bs) <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              name="monto"
              value={formData.monto}
              onChange={handleChange}
              required
              min="0.01"
              step="0.01"
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="0.00"
            />
          </div>

          {/* Método de Pago */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Método de Pago <span className="text-red-500">*</span>
            </label>
            <select
              name="metodoPago"
              value={formData.metodoPago}
              onChange={handleChange}
              required
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
            >
              <option value="EFECTIVO">💵 Efectivo</option>
              <option value="TRANSFERENCIA">🏦 Transferencia Bancaria</option>
              <option value="QR">📱 QR (Pago Digital)</option>
            </select>
          </div>

          {/* Referencia */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Referencia / Número de Transacción
            </label>
            <input
              type="text"
              name="referencia"
              value={formData.referencia}
              onChange={handleChange}
              maxLength={100}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: TRX-12345, Ref-ABC..."
            />
          </div>
          </div>

          {/* Información adicional */}
          {ventaSeleccionada && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <h4 className="text-sm font-semibold text-blue-900 mb-2">ℹ️ Información de la Venta</h4>
              <div className="space-y-1 text-sm text-blue-800">
                <p>• Total de la venta: <span className="font-bold">{formatCurrency(ventaSeleccionada.montoTotal)}</span></p>
                <p>• Estado: <span className="font-medium">{ventaSeleccionada.estado}</span></p>
                <p>• Fecha: {new Date(ventaSeleccionada.fechaVenta).toLocaleDateString('es-BO')}</p>
              </div>
            </div>
          )}

          {/* Botones */}
          </div>

          <div className="flex justify-end gap-3 px-5 py-2.5 border-t border-gray-200 bg-white shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors font-medium"
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-4 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:bg-gray-400"
              disabled={loading || !ventaSeleccionada}
            >
              {loading ? 'Registrando...' : 'Registrar Pago'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}