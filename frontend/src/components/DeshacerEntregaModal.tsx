'use client';

import { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import { EstadoEntrega, ModalidadEntrega, Venta } from '@/types/venta';
import { deshacerEntregaVenta } from '@/lib/api';

interface DeshacerEntregaModalProps {
  venta: Venta;
  onClose: () => void;
  /** Recibe la venta ya actualizada, tal como la devuelve el backend. */
  onHecho: (venta: Venta) => void;
}

/**
 * Retroceder la entrega un paso (solo ADMIN), con confirmación. En una venta
 * por transportadora que ya está entregada se elige a qué estado vuelve,
 * porque pudo haberse entregado sin pasar por el despacho.
 */
export default function DeshacerEntregaModal({ venta, onClose, onHecho }: DeshacerEntregaModalProps) {
  const eligeDestino =
    venta.modalidadEntrega === ModalidadEntrega.TRANSPORTADORA &&
    venta.estadoEntrega === EstadoEntrega.ENTREGADO;

  const [destino, setDestino] = useState<EstadoEntrega>(EstadoEntrega.DESPACHADO);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConfirmar = async () => {
    setProcesando(true);
    setError(null);
    try {
      const actualizada = await deshacerEntregaVenta(venta.id, eligeDestino ? destino : undefined);
      onHecho(actualizada);
      onClose();
    } catch (err: any) {
      setError(err.message || 'No se pudo deshacer la entrega');
    } finally {
      setProcesando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6">
        <div className="flex items-center gap-4 mb-4">
          <div className="flex-shrink-0 w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center">
            <AlertTriangle className="text-amber-600" size={24} />
          </div>
          <div>
            <h3 className="text-lg font-bold text-gray-900">Deshacer entrega</h3>
            <p className="text-sm text-gray-600 mt-1">
              {eligeDestino
                ? `La venta #${venta.id} está entregada. ¿A qué estado vuelve?`
                : `La venta #${venta.id} volverá a Pendiente. Queda registrado en la auditoría.`}
            </p>
          </div>
        </div>

        {eligeDestino && (
          <div className="space-y-2 mb-2">
            {[
              { valor: EstadoEntrega.DESPACHADO, etiqueta: 'Despachado', ayuda: 'Ya salió con la transportadora' },
              { valor: EstadoEntrega.PENDIENTE, etiqueta: 'Pendiente', ayuda: 'Se entregó sin pasar por el despacho' },
            ].map((opcion) => (
              <label
                key={opcion.valor}
                className={`flex items-start gap-3 p-3 border rounded-lg cursor-pointer ${
                  destino === opcion.valor ? 'border-primary-500 bg-primary-50' : 'border-gray-200'
                }`}
              >
                <input
                  type="radio"
                  name="destino-deshacer"
                  checked={destino === opcion.valor}
                  onChange={() => setDestino(opcion.valor)}
                  className="mt-1"
                />
                <span>
                  <span className="block text-sm font-medium text-gray-900">{opcion.etiqueta}</span>
                  <span className="block text-xs text-gray-500">{opcion.ayuda}</span>
                </span>
              </label>
            ))}
            <p className="text-xs text-gray-500 pt-1">Queda registrado en la auditoría.</p>
          </div>
        )}

        {error && (
          <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">{error}</div>
        )}

        <div className="flex justify-end gap-3 mt-6">
          <button
            onClick={onClose}
            disabled={procesando}
            className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={handleConfirmar}
            disabled={procesando}
            className="px-4 py-2 bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition-colors disabled:opacity-60"
          >
            {procesando ? 'Procesando...' : 'Deshacer entrega'}
          </button>
        </div>
      </div>
    </div>
  );
}
