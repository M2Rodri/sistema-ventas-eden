'use client';

import { useState, useEffect } from 'react';
import { createCategoria, updateCategoria } from '@/lib/api';
import { Categoria, TipoProducto } from '@/types/producto';
import { X } from 'lucide-react';

interface CategoriaModalProps {
  categoria: Categoria | null;
  onClose: () => void;
  onSuccess: () => void;
}

interface CategoriaFormData {
  nombre: string;
  descripcion: string;
  tipoProducto: TipoProducto | '';
  activo: boolean;
}

const TIPOS_PRODUCTO: { value: TipoProducto; label: string }[] = [
  { value: 'CAMA', label: 'Cama' },
  { value: 'COLCHON', label: 'Colchón' },
  { value: 'ALMOHADA', label: 'Almohada' },
  { value: 'ACCESORIO', label: 'Accesorio' },
];

export default function CategoriaModal({ categoria, onClose, onSuccess }: CategoriaModalProps) {
  const [formData, setFormData] = useState<CategoriaFormData>({
    nombre: '',
    descripcion: '',
    tipoProducto: '',
    activo: true,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (categoria) {
      setFormData({
        nombre: categoria.nombre,
        descripcion: categoria.descripcion || '',
        tipoProducto: categoria.tipoProducto,
        activo: categoria.activo,
      });
    }
  }, [categoria]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Limpiar error del campo
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      activo: e.target.checked
    }));
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.nombre.trim()) {
      newErrors.nombre = 'El nombre es obligatorio';
    } else if (formData.nombre.length < 2) {
      newErrors.nombre = 'El nombre debe tener al menos 2 caracteres';
    } else if (formData.nombre.length > 100) {
      newErrors.nombre = 'El nombre no puede exceder 100 caracteres';
    }

    if (formData.descripcion && formData.descripcion.length > 500) {
      newErrors.descripcion = 'La descripción no puede exceder 500 caracteres';
    }

    if (!formData.tipoProducto) {
      newErrors.tipoProducto = 'Elegí qué tipo de producto agrupa esta categoría';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    // validate() ya garantizó que tipoProducto no está vacío.
    const datosAEnviar = { ...formData, tipoProducto: formData.tipoProducto as TipoProducto };

    setLoading(true);
    try {
      if (categoria) {
        await updateCategoria(categoria.id, datosAEnviar);
      } else {
        await createCategoria(datosAEnviar);
      }
      onSuccess();
    } catch (error: any) {
      setErrors({ submit: error.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-2xl">
        {/* Header */}
        <div className="border-b px-6 py-4 flex justify-between items-center bg-gradient-to-r from-primary-50 to-primary-100">
          <div>
            <h2 className="text-xl font-bold text-gray-900">
              {categoria ? 'Editar Categoría' : 'Nueva Categoría'}
            </h2>
            <p className="text-sm text-gray-600 mt-1">Agrupa productos para filtrarlos en catálogo y reportes</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Error general */}
          {errors.submit && (
            <div className="bg-red-50 text-red-800 p-4 rounded-lg">
              {errors.submit}
            </div>
          )}

          {/* Nombre */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nombre <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              name="nombre"
              value={formData.nombre}
              onChange={handleChange}
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.nombre ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="Ej: Camas, Colchones, Almohadas"
              disabled={loading}
            />
            {errors.nombre && <p className="text-red-500 text-xs mt-1">{errors.nombre}</p>}
          </div>

          {/* Descripción */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Descripción
            </label>
            <textarea
              name="descripcion"
              value={formData.descripcion}
              onChange={handleChange}
              rows={4}
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.descripcion ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="Descripción opcional de la categoría"
              disabled={loading}
            />
            {errors.descripcion && <p className="text-red-500 text-xs mt-1">{errors.descripcion}</p>}
          </div>

          {/* Tipo de producto */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Tipo de producto <span className="text-red-500">*</span>
            </label>
            <select
              name="tipoProducto"
              value={formData.tipoProducto}
              onChange={handleChange}
              className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.tipoProducto ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={loading}
            >
              <option value="" disabled>Seleccioná...</option>
              {TIPOS_PRODUCTO.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            <p className="text-xs text-gray-500 mt-1">
              Los productos de esta categoría van a quedar marcados con este tipo automáticamente.
            </p>
            {errors.tipoProducto && <p className="text-red-500 text-xs mt-1">{errors.tipoProducto}</p>}
          </div>

          {/* Estado activo */}
          <div className="flex items-center">
            <input
              type="checkbox"
              name="activo"
              checked={formData.activo}
              onChange={handleCheckboxChange}
              className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              disabled={loading}
            />
            <label className="ml-2 block text-sm text-gray-700">
              Categoría activa
            </label>
          </div>

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={loading}
            >
              {loading ? 'Guardando...' : categoria ? 'Actualizar' : 'Crear'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}