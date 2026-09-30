'use client';

import { useState } from 'react';
import { X } from 'lucide-react';
import { Venta, ModalidadEntrega } from '@/types/venta';
import { actualizarDatosEntrega } from '@/lib/api';

interface EditarEntregaModalProps {
  venta: Venta;
  onClose: () => void;
  /** Recibe la venta ya actualizada, tal como la devuelve el backend. */
  onGuardado: (venta: Venta) => void;
}

/**
 * Completar o corregir la dirección, la transportadora y la guía de una venta
 * ya registrada (solo ADMIN). Un campo vacío borra el dato.
 */
export default function EditarEntregaModal({ venta, onClose, onGuardado }: EditarEntregaModalProps) {
  const esTransportadora = venta.modalidadEntrega === ModalidadEntrega.TRANSPORTADORA;

  const [direccion, setDireccion] = useState(venta.direccionDestino ?? '');
  const [transportadora, setTransportadora] = useState(venta.transportadora ?? '');
  const [guia, setGuia] = useState(venta.guiaRemision ?? '');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGuardar = async () => {
    setGuardando(true);
    setError(null);
    try {
      const actualizada = await actualizarDatosEntrega(venta.id, {
        direccionDestino: direccion,
        transportadora: esTransportadora ? transportadora : undefined,
        guiaRemision: esTransportadora ? guia : undefined,
      });
      onGuardado(actualizada);
      onClose();
    } catch (err: any) {
      setError(err.message || 'No se pudieron guardar los datos de entrega');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full">
        <div className="flex items-center justify-between p-5 border-b border-gray-200">
          <div>
            <h3 className="text-lg font-bold text-gray-900">Datos de entrega</h3>
            <p className="text-sm text-gray-600">Venta #{venta.id}</p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={22} />
          </button>
        </div>

        <div className="p-5 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{error}</div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Dirección</label>
            <input
              type="text"
              value={direccion}
              onChange={(e) => setDireccion(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              placeholder="Dirección de entrega"
              maxLength={300}
            />
            <p className="mt-1 text-xs text-gray-500">Te sirve para coordinar la entrega</p>
          </div>

          {esTransportadora && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Transportadora</label>
                <input
                  type="text"
                  value={transportadora}
                  onChange={(e) => setTransportadora(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  placeholder="Nombre de la transportadora"
                  maxLength={100}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Guía de remisión</label>
                <input
                  type="text"
                  value={guia}
                  onChange={(e) => setGuia(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  placeholder="Número de guía"
                  maxLength={100}
                />
              </div>
            </>
          )}
        </div>

        <div className="flex justify-end gap-3 p-5 border-t border-gray-200">
          <button
            onClick={onClose}
            disabled={guardando}
            className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={handleGuardar}
            disabled={guardando}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-60"
          >
            {guardando ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </div>
    </div>
  );
}
