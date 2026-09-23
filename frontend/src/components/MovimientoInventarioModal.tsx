'use client';

import { useState } from 'react';
import { X, AlertCircle } from 'lucide-react';
import { ajustarInventario } from '@/lib/api';
import { Inventario, MovimientoInventarioRequest } from '@/types/inventario';

interface AjusteInventarioModalProps {
  inventario: Inventario;
  onClose: () => void;
  onSuccess: () => void;
}

export default function MovimientoInventarioModal({ inventario, onClose, onSuccess }: AjusteInventarioModalProps) {
  const [formData, setFormData] = useState<MovimientoInventarioRequest>({
    idProducto: inventario.idProducto,
    cantidad: 0,
    tipoMovimiento: 'ENTRADA',
    motivo: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (formData.cantidad <= 0) {
      setError('La cantidad debe ser mayor a 0');
      return;
    }

    if (!formData.motivo.trim()) {
      setError('El motivo es obligatorio');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await ajustarInventario(formData);
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const calcularNuevaCantidad = () => {
    if (formData.tipoMovimiento === 'ENTRADA') {
      return inventario.cantidadDisponible + formData.cantidad;
    } else {
      return Math.max(0, inventario.cantidadDisponible - formData.cantidad);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Ajustar Inventario</h2>
            <p className="text-sm text-gray-600 mt-1">
              {inventario.nombreProducto} <span className="text-gray-400">({inventario.skuProducto})</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Información actual */}
        <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-gray-500">Stock Actual</p>
              <p className="text-2xl font-bold text-gray-900">{inventario.cantidadDisponible}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Stock Mínimo</p>
              <p className="text-2xl font-bold text-gray-900">{inventario.stockMinimo}</p>
            </div>
          </div>
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 text-red-800 rounded-lg text-sm">
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          {/* Tipo de ajuste */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Tipo de Ajuste <span className="text-red-500">*</span>
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setFormData({ ...formData, tipoMovimiento: 'ENTRADA' })}
                className={`px-4 py-3 rounded-lg border-2 font-medium transition-all ${
                  formData.tipoMovimiento === 'ENTRADA'
                    ? 'border-green-500 bg-green-50 text-green-700'
                    : 'border-gray-200 bg-white text-gray-700 hover:border-gray-300'
                }`}
              >
                ➕ Entrada
              </button>
              <button
                type="button"
                onClick={() => setFormData({ ...formData, tipoMovimiento: 'SALIDA' })}
                className={`px-4 py-3 rounded-lg border-2 font-medium transition-all ${
                  formData.tipoMovimiento === 'SALIDA'
                    ? 'border-red-500 bg-red-50 text-red-700'
                    : 'border-gray-200 bg-white text-gray-700 hover:border-gray-300'
                }`}
              >
                ➖ Salida
              </button>
            </div>
          </div>

          {/* Cantidad */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Cantidad <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              min="1"
              value={formData.cantidad || ''}
              onChange={(e) => setFormData({ ...formData, cantidad: parseInt(e.target.value) || 0 })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ingrese la cantidad"
              required
            />
          </div>

          {/* Motivo */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Motivo <span className="text-red-500">*</span>
            </label>
            <textarea
              value={formData.motivo}
              onChange={(e) => setFormData({ ...formData, motivo: e.target.value })}
              rows={3}
              maxLength={200}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              placeholder="Ej: Compra a proveedor, Devolución de cliente, Ajuste por inventario físico..."
              required
            />
            <p className="text-xs text-gray-500 mt-1">{formData.motivo.length}/200 caracteres</p>
          </div>

          {/* Preview del resultado */}
          {formData.cantidad > 0 && (
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-gray-700 mb-2">
                <strong>Resultado del ajuste:</strong>
              </p>
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Stock actual:</span>
                <span className="font-semibold">{inventario.cantidadDisponible}</span>
              </div>
              <div className="flex items-center justify-between text-sm mt-1">
                <span className="text-gray-600">
                  {formData.tipoMovimiento === 'ENTRADA' ? 'Se agregará:' : 'Se restará:'}
                </span>
                <span className={formData.tipoMovimiento === 'ENTRADA' ? 'text-green-600' : 'text-red-600'}>
                  {formData.tipoMovimiento === 'ENTRADA' ? '+' : '-'}{formData.cantidad}
                </span>
              </div>
              <div className="border-t border-blue-300 mt-2 pt-2 flex items-center justify-between">
                <span className="font-semibold text-gray-700">Nuevo stock:</span>
                <span className="text-xl font-bold text-blue-700">{calcularNuevaCantidad()}</span>
              </div>
            </div>
          )}

          {/* Botones */}
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="flex-1 px-4 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:opacity-50"
            >
              {loading ? 'Guardando...' : 'Confirmar Ajuste'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}