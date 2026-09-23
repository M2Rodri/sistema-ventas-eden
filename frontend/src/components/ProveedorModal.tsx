'use client';

import { useState, useEffect } from 'react';
import { X, AlertCircle } from 'lucide-react';
import { createProveedor, updateProveedor } from '@/lib/api';
import { Proveedor, ProveedorRequest } from '@/types/proveedor';

interface ProveedorModalProps {
  proveedor: Proveedor | null;
  onClose: () => void;
  onSuccess: () => void;
}

export default function ProveedorModal({ proveedor, onClose, onSuccess }: ProveedorModalProps) {
  const [formData, setFormData] = useState<ProveedorRequest>({
    nombreEmpresa: '',
    nit: '',
    contacto: '',
    telefono: '',
    direccion: '',
    email: '',
    notas: '',
    activo: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (proveedor) {
      setFormData({
        nombreEmpresa: proveedor.nombreEmpresa,
        nit: proveedor.nit,
        contacto: proveedor.contacto,
        telefono: proveedor.telefono,
        direccion: proveedor.direccion || '',
        email: proveedor.email || '',
        notas: proveedor.notas || '',
        activo: proveedor.activo,
      });
    }
  }, [proveedor]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (proveedor) {
        await updateProveedor(proveedor.id, formData);
      } else {
        await createProveedor(formData);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
          <div>
            <h2 className="text-xl font-bold text-gray-900">
              {proveedor ? 'Editar Proveedor' : 'Nuevo Proveedor'}
            </h2>
            <p className="text-sm text-gray-600 mt-1">Datos de contacto para registrar compras a este proveedor</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 text-red-800 rounded-lg text-sm">
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          {/* Nombre de Empresa */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nombre de Empresa <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.nombreEmpresa}
              onChange={(e) => setFormData({ ...formData, nombreEmpresa: e.target.value })}
              maxLength={200}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: Distribuidora XYZ S.R.L."
              required
            />
          </div>

          {/* NIT */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              NIT <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.nit}
              onChange={(e) => setFormData({ ...formData, nit: e.target.value })}
              maxLength={20}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: 123456789"
              required
            />
          </div>

          {/* Contacto y Teléfono en una fila */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Contacto */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Persona de Contacto <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.contacto}
                onChange={(e) => setFormData({ ...formData, contacto: e.target.value })}
                maxLength={100}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: Juan Pérez"
                required
              />
            </div>

            {/* Teléfono */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Teléfono <span className="text-red-500">*</span>
              </label>
              <input
                type="tel"
                value={formData.telefono}
                onChange={(e) => setFormData({ ...formData, telefono: e.target.value })}
                maxLength={15}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: 70123456"
                required
              />
            </div>
          </div>

          {/* Correo */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Correo Electrónico
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              maxLength={100}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="email@empresa.com"
            />
          </div>

          {/* Dirección */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Dirección
            </label>
            <input
              type="text"
              value={formData.direccion}
              onChange={(e) => setFormData({ ...formData, direccion: e.target.value })}
              maxLength={300}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: Av. Principal #123, Zona Centro"
            />
          </div>

          {/* Notas */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Notas / Observaciones
            </label>
            <textarea
              value={formData.notas}
              onChange={(e) => setFormData({ ...formData, notas: e.target.value })}
              rows={3}
              maxLength={500}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              placeholder="Información adicional sobre el proveedor..."
            />
            <p className="text-xs text-gray-500 mt-1">{(formData.notas || '').length}/500 caracteres</p>
          </div>

          {/* Estado */}
          <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
            <input
              type="checkbox"
              id="activo"
              checked={formData.activo}
              onChange={(e) => setFormData({ ...formData, activo: e.target.checked })}
              className="w-5 h-5 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
            />
            <label htmlFor="activo" className="text-sm font-medium text-gray-700 cursor-pointer">
              Proveedor activo
            </label>
          </div>

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
              {loading ? 'Guardando...' : proveedor ? 'Actualizar' : 'Crear Proveedor'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}