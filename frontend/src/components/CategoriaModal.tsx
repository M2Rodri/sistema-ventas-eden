'use client';

import { useState, useEffect } from 'react';
import { createCategoria, updateCategoria } from '@/lib/api';
import { Categoria, TipoProducto } from '@/types/producto';
import { X } from 'lucide-react';

interface CategoriaModalProps {
  categoria: Categoria | null;
  /** Todas las categorías existentes: para no ofrecer una que ya está creada. */
  categorias: Categoria[];
  onClose: () => void;
  onSuccess: () => void;
}

interface CategoriaFormData {
  nombre: string;
  descripcion: string;
  tipoProducto: TipoProducto | '';
  activo: boolean;
}

/** Las únicas categorías que existen (cinco): una por cada tipo de producto. */
export const CATEGORIAS_FIJAS: { nombre: string; tipo: TipoProducto }[] = [
  { nombre: 'Camas', tipo: 'CAMA' },
  { nombre: 'Colchones', tipo: 'COLCHON' },
  { nombre: 'Almohadas', tipo: 'ALMOHADA' },
  { nombre: 'Accesorios', tipo: 'ACCESORIO' },
  // Oculta por ahora (el código del tipo MUEBLE ya existe): quitar el // para mostrarla.
  // { nombre: 'Muebles de dormitorio', tipo: 'MUEBLE' },
];

export default function CategoriaModal({ categoria, categorias, onClose, onSuccess }: CategoriaModalProps) {
  const [formData, setFormData] = useState<CategoriaFormData>({
    nombre: '',
    descripcion: '',
    tipoProducto: '',
    activo: true,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  // Solo se ofrecen las categorías que todavía no existen (más la que se está
  // editando). El tipo de producto sale de la categoría elegida.
  const opciones = CATEGORIAS_FIJAS.filter(
    (f) =>
      f.nombre.toLowerCase() === categoria?.nombre.toLowerCase() ||
      !categorias.some((c) => c.nombre.toLowerCase() === f.nombre.toLowerCase())
  );
  const tipoElegido = CATEGORIAS_FIJAS.find((f) => f.nombre === formData.nombre)?.tipo;

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

    if (!tipoElegido) {
      newErrors.nombre = 'Elegí la categoría';
    }

    if (formData.descripcion && formData.descripcion.length > 500) {
      newErrors.descripcion = 'La descripción no puede exceder 500 caracteres';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const datosAEnviar = {
      ...formData,
      tipoProducto: tipoElegido as TipoProducto,
    };

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
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-3">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="border-b px-5 py-2 flex justify-between items-center bg-gradient-to-r from-primary-50 to-primary-100 shrink-0">
          <h2 className="text-base font-bold text-gray-900">
            {categoria ? 'Editar Categoría' : 'Nueva Categoría'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="flex-1 overflow-y-auto px-5 pt-4 pb-3 space-y-3">
          {/* Error general */}
          {errors.submit && (
            <div className="bg-red-50 text-red-800 p-3 rounded-lg">
              {errors.submit}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {/* Nombre */}
          <div className="md:col-span-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Nombre <span className="text-red-500">*</span>
            </label>
            <select
              name="nombre"
              value={formData.nombre}
              onChange={handleChange}
              className={`w-full px-2 py-1.5 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.nombre ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={loading}
            >
              <option value="" disabled hidden>Seleccioná…</option>
              {opciones.map((f) => (
                <option key={f.tipo} value={f.nombre}>{f.nombre}</option>
              ))}
            </select>
            {errors.nombre && <p className="text-red-500 text-xs mt-1">{errors.nombre}</p>}
          </div>

          {/* Descripción */}
          <div className="md:col-span-3">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Descripción
            </label>
            <textarea
              name="descripcion"
              value={formData.descripcion}
              onChange={handleChange}
              rows={2}
              className={`w-full px-2 py-1.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.descripcion ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="Descripción opcional de la categoría"
              disabled={loading}
            />
            {errors.descripcion && <p className="text-red-500 text-xs mt-1">{errors.descripcion}</p>}
          </div>

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
          </div>
          <div className="flex justify-end gap-3 px-5 py-2.5 border-t border-gray-200 bg-white shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-1.5 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-4 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
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