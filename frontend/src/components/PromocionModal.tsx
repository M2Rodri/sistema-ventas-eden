'use client';

import { useState, useEffect } from 'react';
import { createPromocion, updatePromocion } from '@/lib/api';
import { Promocion, PromocionRequest } from '@/types/promocion';
import { Producto } from '@/types/producto';
import { X } from 'lucide-react';

interface PromocionModalProps {
  promocion: Promocion | null;
  productos: Producto[];
  onClose: () => void;
  onSuccess: () => void;
}

export default function PromocionModal({ promocion, productos, onClose, onSuccess }: PromocionModalProps) {
  const [formData, setFormData] = useState<PromocionRequest>({
    nombre: '',
    descripcion: '',
    descuento: 0,
    fechaInicio: '',
    fechaFin: '',
    idsProductos: [],
    activo: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (promocion) {
      setFormData({
        nombre: promocion.nombre,
        descripcion: promocion.descripcion ?? '',
        descuento: promocion.descuento,
        fechaInicio: promocion.fechaInicio.split('T')[0],
        fechaFin: promocion.fechaFin.split('T')[0],
        idsProductos: promocion.productos.map(p => p.id),
        activo: promocion.activo,
      });
    }
  }, [promocion]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Validaciones
    if (formData.descuento < 1 || formData.descuento > 100) {
      setError('El descuento debe estar entre 1% y 100%');
      return;
    }

    if (new Date(formData.fechaInicio) >= new Date(formData.fechaFin)) {
      setError('La fecha de fin debe ser posterior a la fecha de inicio');
      return;
    }

    if (formData.idsProductos.length === 0) {
      setError('Debes seleccionar al menos un producto');
      return;
    }

    try {
      setLoading(true);
      if (promocion) {
        await updatePromocion(promocion.id, formData);
      } else {
        await createPromocion(formData);
      }
      onSuccess();
    } catch (error: any) {
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const toggleProducto = (idProducto: number) => {
    setFormData(prev => ({
      ...prev,
      idsProductos: prev.idsProductos.includes(idProducto)
        ? prev.idsProductos.filter(id => id !== idProducto)
        : [...prev.idsProductos, idProducto]
    }));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">
            {promocion ? 'Editar Promoción' : 'Nueva Promoción'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-800 text-sm">
              {error}
            </div>
          )}

          {/* Nombre */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nombre *
            </label>
            <input
              type="text"
              value={formData.nombre}
              onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: Semana del Descanso"
              maxLength={150}
              required
            />
          </div>

          {/* Descripción */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Descripción
            </label>
            <textarea
              value={formData.descripcion ?? ''}
              onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              placeholder="Ej: Descuento en toda la línea de colchones por aniversario."
              rows={2}
            />
          </div>

          {/* Descuento */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Descuento (%) *
            </label>
            <input
              type="number"
              min="1"
              max="100"
              value={formData.descuento}
              onChange={(e) => setFormData({ ...formData, descuento: parseFloat(e.target.value) })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              required
            />
          </div>

          {/* Fechas */}
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Fecha Inicio *
              </label>
              <input
                type="date"
                value={formData.fechaInicio}
                onChange={(e) => setFormData({ ...formData, fechaInicio: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Fecha Fin *
              </label>
              <input
                type="date"
                value={formData.fechaFin}
                onChange={(e) => setFormData({ ...formData, fechaFin: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                required
              />
            </div>
          </div>

          {/* Productos */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Productos incluidos * ({formData.idsProductos.length} seleccionados)
            </label>
            <div className="border border-gray-300 rounded-lg max-h-60 overflow-y-auto p-3">
              {productos.map(producto => (
                <label
                  key={producto.id}
                  className="flex items-center gap-3 p-2 hover:bg-gray-50 rounded cursor-pointer"
                >
                  <input
                    type="checkbox"
                    checked={formData.idsProductos.includes(producto.id)}
                    onChange={() => toggleProducto(producto.id)}
                    className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
                  />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900">{producto.nombre}</p>
                    <p className="text-xs text-gray-500">{producto.sku}</p>
                  </div>
                  <span className="text-sm font-semibold text-gray-700">
                    Bs. {producto.precioVenta.toFixed(2)}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Activo */}
          <div className="mb-6">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={formData.activo}
                onChange={(e) => setFormData({ ...formData, activo: e.target.checked })}
                className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
              />
              <span className="text-sm font-medium text-gray-700">Promocion activa</span>
            </label>
          </div>

          {/* Botones */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
            >
              {loading ? 'Guardando...' : promocion ? 'Actualizar' : 'Crear Promocion'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
