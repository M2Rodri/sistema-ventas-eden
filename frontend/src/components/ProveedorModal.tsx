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
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-3">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-2 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 shrink-0">
          <h2 className="text-base font-bold text-gray-900">
            {proveedor ? 'Editar Proveedor' : 'Nuevo Proveedor'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="flex-1 overflow-y-auto px-5 pt-4 pb-3 space-y-3">
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 text-red-800 rounded-lg text-sm">
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {/* Nombre de Empresa */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Nombre de Empresa <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.nombreEmpresa}
              onChange={(e) => setFormData({ ...formData, nombreEmpresa: e.target.value })}
              maxLength={200}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: Distribuidora XYZ S.R.L."
              required
            />
          </div>

          {/* NIT */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              NIT <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.nit}
              onChange={(e) => setFormData({ ...formData, nit: e.target.value })}
              maxLength={20}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: 123456789"
              required
            />
          </div>

          {/* Contacto y Teléfono en una fila */}
          <div className="contents">
            {/* Contacto */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Persona de Contacto <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.contacto}
                onChange={(e) => setFormData({ ...formData, contacto: e.target.value })}
                maxLength={100}
                className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: Juan Pérez"
                required
              />
            </div>

            {/* Teléfono */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Teléfono <span className="text-red-500">*</span>
              </label>
              <input
                type="tel"
                value={formData.telefono}
                onChange={(e) => setFormData({ ...formData, telefono: e.target.value })}
                maxLength={15}
                className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: 70123456"
                required
              />
            </div>
          </div>

          {/* Correo */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Correo Electrónico
            </label>
            <input
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              maxLength={100}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="email@empresa.com"
            />
          </div>

          {/* Dirección */}
          <div className="md:col-span-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Dirección
            </label>
            <input
              type="text"
              value={formData.direccion}
              onChange={(e) => setFormData({ ...formData, direccion: e.target.value })}
              maxLength={300}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: Av. Principal #123, Zona Centro"
            />
          </div>

          {/* Notas */}
          <div className="md:col-span-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Notas / Observaciones
            </label>
            <textarea
              value={formData.notas}
              onChange={(e) => setFormData({ ...formData, notas: e.target.value })}
              rows={2}
              maxLength={500}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              placeholder="Información adicional sobre el proveedor..."
            />
          </div>

          </div>

          {/* Estado */}
          <div className="flex items-center gap-3 p-2 bg-gray-50 rounded-lg">
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
          </div>
          <div className="flex justify-end gap-3 px-5 py-2.5 border-t border-gray-200 bg-white shrink-0">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:opacity-50"
            >
              {loading ? 'Guardando...' : proveedor ? 'Actualizar' : 'Crear Proveedor'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}