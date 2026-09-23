'use client';

import { useState } from 'react';
import { X, AlertCircle, AlertTriangle } from 'lucide-react';
import { Inventario } from '@/types/inventario';
import { actualizarStockMinimo } from '@/lib/api';

interface ConfigurarStockMinimoModalProps {
  inventario: Inventario;
  onClose: () => void;
  onSuccess: () => void;
}

export default function ConfigurarStockMinimoModal({ inventario, onClose, onSuccess }: ConfigurarStockMinimoModalProps) {
  const [stockMinimo, setStockMinimo] = useState<number>(inventario.stockMinimo);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  // Antes esto era un console.log: la pantalla decía que había guardado y el
  // valor nunca llegaba a la base. Como el stock mínimo es el umbral que
  // dispara las alertas, cambiarlo no tenía ningún efecto real.
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (stockMinimo <= 0) {
      setError('Ingrese un valor numérico positivo válido');
      return;
    }

    setGuardando(true);
    setError(null);
    try {
      await actualizarStockMinimo(inventario.idProducto, stockMinimo);
      onSuccess();
    } catch (err: any) {
      setError(err?.message ?? 'No se pudo guardar el stock mínimo');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Configurar Stock Mínimo</h2>
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
              <p className="text-xs text-gray-500">Stock Mínimo Actual</p>
              <p className="text-2xl font-bold text-orange-600">{inventario.stockMinimo}</p>
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

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nuevo Stock Mínimo <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              min="1"
              value={stockMinimo}
              onChange={(e) => {
                setStockMinimo(parseInt(e.target.value) || 0);
                setError(null);
              }}
              className="w-full px-4 py-3 text-lg border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ingrese el stock mínimo"
              required
            />
          </div>

          {/* Referencia Visual */}
          {stockMinimo > 0 && (
            <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg flex items-start gap-3">
              <AlertTriangle className="text-yellow-600 flex-shrink-0 mt-0.5" size={20} />
              <div>
                <p className="text-sm font-medium text-gray-900 mb-1">
                  Referencia de Alerta
                </p>
                <p className="text-sm text-gray-700">
                  Se generará alerta cuando el stock sea <strong>≤ {stockMinimo}</strong> unidades
                </p>
              </div>
            </div>
          )}

          {/* Botones */}
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={guardando}
              className="flex-1 px-4 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={guardando}
              className="flex-1 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:opacity-50"
            >
              {guardando ? 'Guardando…' : 'Guardar Configuración'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}