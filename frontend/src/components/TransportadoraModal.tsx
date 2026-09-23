'use client';

import { useState, useEffect } from 'react';
import { createTransportadora, updateTransportadora } from '@/lib/api';
import { Transportadora, TransportadoraRequest } from '@/types/transportadora';
import { X, Building2 } from 'lucide-react';

interface TransportadoraModalProps {
  transportadora: Transportadora | null;
  onClose: () => void;
  onSuccess: () => void;
}

export default function TransportadoraModal({ transportadora, onClose, onSuccess }: TransportadoraModalProps) {
  const [formData, setFormData] = useState<TransportadoraRequest>({
    nombre: '',
    telefono: '',
    email: '',
    tarifaBase: 0,
    tiempoEstimadoDias: 3,
    activo: true,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (transportadora) {
      setFormData({
        nombre: transportadora.nombre,
        telefono: transportadora.telefono || '',
        email: transportadora.email || '',
        tarifaBase: transportadora.tarifaBase,
        tiempoEstimadoDias: transportadora.tiempoEstimadoDias,
        activo: transportadora.activo,
      });
    }
  }, [transportadora]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (transportadora) {
        await updateTransportadora(transportadora.id, formData);
      } else {
        await createTransportadora(formData);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'number' ? parseFloat(value) || 0 : 
              type === 'checkbox' ? (e.target as HTMLInputElement).checked : 
              value
    }));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-gradient-to-r from-primary-50 to-primary-100 z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center">
              <Building2 className="text-primary-600" size={20} />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                {transportadora ? 'Editar Transportadora' : 'Nueva Transportadora'}
              </h2>
              <p className="text-sm text-gray-600 mt-1">Empresa encargada de las entregas a domicilio</p>
            </div>
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

          {/* Nombre */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nombre de la Transportadora <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              name="nombre"
              value={formData.nombre}
              onChange={handleChange}
              required
              minLength={2}
              maxLength={100}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: DHL Express"
            />
          </div>

          {/* Teléfono y Correo */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Teléfono
              </label>
              <input
                type="tel"
                name="telefono"
                value={formData.telefono}
                onChange={handleChange}
                maxLength={15}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: 71234567"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Correo Electrónico
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                maxLength={100}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="contacto@transportadora.com"
              />
            </div>
          </div>

          {/* Tarifa Base y Tiempo Estimado */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Tarifa Base (Bs) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                name="tarifaBase"
                value={formData.tarifaBase}
                onChange={handleChange}
                required
                min="0"
                step="0.01"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="0.00"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Tiempo Estimado (días) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                name="tiempoEstimadoDias"
                value={formData.tiempoEstimadoDias}
                onChange={handleChange}
                required
                min="1"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="3"
              />
            </div>
          </div>

          {/* Estado Activo */}
          <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
            <input
              type="checkbox"
              name="activo"
              id="activo"
              checked={formData.activo}
              onChange={handleChange}
              className="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
            />
            <label htmlFor="activo" className="text-sm font-medium text-gray-700 cursor-pointer">
              Transportadora activa (disponible para envíos)
            </label>
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
              {loading ? 'Guardando...' : transportadora ? 'Actualizar' : 'Crear Transportadora'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}