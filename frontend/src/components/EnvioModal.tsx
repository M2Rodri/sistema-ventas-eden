'use client';

import { useState, useEffect } from 'react';
import { createEnvio, updateEnvio } from '@/lib/api';
import { Envio, EnvioRequest } from '@/types/envio';
import { Transportadora } from '@/types/transportadora';
import { X, Truck } from 'lucide-react';

interface EnvioModalProps {
  envio: Envio | null;
  transportadoras: Transportadora[];
  onClose: () => void;
  onSuccess: () => void;
}

export default function EnvioModal({ envio, transportadoras, onClose, onSuccess }: EnvioModalProps) {
  const [formData, setFormData] = useState<EnvioRequest>({
    
    idVenta: null,
    direccionDestino: '',
    ciudad: '',
    departamento: '',
    fechaEntregaEstimada: '',
    guiaRemision: '',
    costoEnvio: 0,
    idTransportadora: null,
    notas: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (envio) {
      setFormData({
        
        idVenta: envio.idVenta,
        direccionDestino: envio.direccionDestino,
        ciudad: envio.ciudad || '',
        departamento: envio.departamento || '',
        fechaEntregaEstimada: envio.fechaEntregaEstimada,
        guiaRemision: envio.guiaRemision || '',
        costoEnvio: envio.costoEnvio,
        idTransportadora: envio.idTransportadora,
        notas: envio.notas || '',
      });
    }
  }, [envio]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (envio) {
        await updateEnvio(envio.id, formData);
      } else {
        await createEnvio(formData);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'costoEnvio' ? parseFloat(value) || 0 : 
              name === 'idTransportadora' || name === 'idVenta' ? 
              (value ? parseInt(value) : null) : value
    }));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center">
              <Truck className="text-primary-600" size={20} />
            </div>
            <h2 className="text-xl font-bold text-gray-900">
              {envio ? 'Editar Envío' : 'Nuevo Envío'}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="bg-red-50 text-red-800 p-4 rounded-lg text-sm">
              {error}
            </div>
          )}

          {/* Datos de Origen */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                ID Venta (opcional)
              </label>
              <input
                type="number"
                name="idVenta"
                value={formData.idVenta || ''}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>

          {/* Dirección de Destino */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Dirección de Destino <span className="text-red-500">*</span>
            </label>
            <textarea
              name="direccionDestino"
              value={formData.direccionDestino}
              onChange={handleChange}
              required
              rows={2}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Calle, número, zona..."
            />
          </div>

          {/* Ciudad y Departamento */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Ciudad
              </label>
              <input
                type="text"
                name="ciudad"
                value={formData.ciudad}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: Yacuiba"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Departamento
              </label>
              <input
                type="text"
                name="departamento"
                value={formData.departamento}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: Tarija"
              />
            </div>
          </div>

          {/* Fecha Estimada y Costo */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Fecha Entrega Estimada <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                name="fechaEntregaEstimada"
                value={formData.fechaEntregaEstimada}
                onChange={handleChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Costo de Envío (Bs)
              </label>
              <input
                type="number"
                name="costoEnvio"
                value={formData.costoEnvio}
                onChange={handleChange}
                step="0.01"
                min="0"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>

          {/* Transportadora y Guía */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Transportadora
              </label>
              <select
                name="idTransportadora"
                value={formData.idTransportadora || ''}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
              >
                <option value="">Sin asignar</option>
                {transportadoras.filter(t => t.activo).map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nombre} - Bs {t.tarifaBase} ({t.tiempoEstimadoDias} días)
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Guía de Remisión
              </label>
              <input
                type="text"
                name="guiaRemision"
                value={formData.guiaRemision}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Número de guía"
              />
            </div>
          </div>

          {/* Notas */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Notas / Observaciones
            </label>
            <textarea
              name="notas"
              value={formData.notas}
              onChange={handleChange}
              rows={3}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Información adicional sobre el envío..."
            />
          </div>

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors font-medium"
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:bg-gray-400"
              disabled={loading}
            >
              {loading ? 'Guardando...' : envio ? 'Actualizar' : 'Crear Envío'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}