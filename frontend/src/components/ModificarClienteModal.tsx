'use client';

import React, { useState, useEffect } from 'react';
import { X, User, Phone, Mail, CreditCard, AlertCircle, CheckCircle } from 'lucide-react';
import { updateCliente } from '@/lib/api';
import { ClienteRequest, ClienteResponse } from '@/types/cliente';

interface ModificarClienteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  cliente: ClienteResponse;
}

/**
 * Modal para Modificar Cliente (Interfaz P6.2)
 * Cumple con CU: Modificar Cliente
 */
export default function ModificarClienteModal({
  isOpen,
  onClose,
  cliente,
  onSuccess,
}: ModificarClienteModalProps) {
  const [formData, setFormData] = useState<ClienteRequest>({
    nombre: '',
    apellido: '',
    telefono: '',
    nitCi: '',
    email: '',
    tipoCliente: cliente.tipoCliente,
    activo: cliente.activo,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (isOpen && cliente) {
      // Separar nombre completo en nombre y apellido
      const [nombre, ...apellidoParts] = cliente.nombreCompleto.split(' ');
      const apellido = apellidoParts.join(' ');

      setFormData({
        nombre: nombre || '',
        apellido: apellido || '',
        telefono: cliente.telefono,
        nitCi: cliente.nitCi || '',
        email: cliente.email || '',
        tipoCliente: cliente.tipoCliente,
        activo: cliente.activo,
      });
      setError(null);
      setSuccess(false);
    }
  }, [isOpen, cliente]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError(null);
  };

  const validarFormulario = (): boolean => {
    // Validar nombre completo (obligatorio)
    if (!formData.nombre.trim()) {
      setError('El nombre es obligatorio');
      return false;
    }

    if (formData.nombre.length < 2) {
      setError('El nombre debe tener al menos 2 caracteres');
      return false;
    }
    if (formData.apellido && formData.apellido.trim().length === 1) {
      setError('El apellido debe tener al menos 2 caracteres');
      return false;
    }

    // Validar teléfono (obligatorio)
    if (!formData.telefono || !formData.telefono.trim()) {
      setError('El número de teléfono es obligatorio');
      return false;
    }

    // Validar formato de teléfono (números y espacios, 7-15 caracteres)
    const telefonoRegex = /^[0-9\s]{7,15}$/;
    if (!telefonoRegex.test(formData.telefono.trim())) {
      setError('Ingrese un número de teléfono válido (7-15 dígitos)');
      return false;
    }

    // Validar formato de correo electrónico (si existe)
    if (formData.email && formData.email.trim()) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.email.trim())) {
        setError('Ingrese un correo electrónico válido');
        return false;
      }
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!validarFormulario()) {
      return;
    }

    setLoading(true);

    try {
      await updateCliente(cliente.id, formData);
      setSuccess(true);

      // Mostrar mensaje de éxito y cerrar después de 1.5 segundos
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1500);
    } catch (err: any) {
      setError(err.message || 'Error al actualizar el cliente');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b border-gray-200 sticky top-0 bg-gradient-to-r from-primary-50 to-primary-100 z-10">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
              <User className="text-blue-600" size={28} />
              Modificar Cliente
            </h2>
            <p className="text-sm text-gray-500 mt-1">
              Cliente ID: <span className="font-semibold">#{cliente.id}</span>
            </p>
          </div>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={loading}
          >
            <X size={24} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6">
          {/* Mensajes de error/éxito */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
              <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
              <div className="flex-1">
                <p className="text-sm font-medium text-red-800">Error</p>
                <p className="text-sm text-red-600 mt-1">{error}</p>
              </div>
              <button
                type="button"
                onClick={() => setError(null)}
                className="text-red-400 hover:text-red-600"
              >
                <X size={18} />
              </button>
            </div>
          )}

          {success && (
            <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-3">
              <CheckCircle className="text-green-600" size={20} />
              <p className="text-sm font-medium text-green-800">
                Datos del cliente actualizados exitosamente
              </p>
            </div>
          )}

          {/* Campos del formulario */}
          <div className="space-y-6">
            {/* Nombre y Apellido */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Nombre <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="nombre"
                  value={formData.nombre}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Ej: Juan"
                  maxLength={100}
                  required
                  disabled={loading || success}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Apellido <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="apellido"
                  value={formData.apellido}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Ej: Pérez"
                  maxLength={100}
                  required
                  disabled={loading || success}
                />
              </div>
            </div>

            {/* Teléfono */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                <Phone size={16} />
                Número de Teléfono <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="telefono"
                value={formData.telefono}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Ej: 71234567"
                maxLength={15}
                required
                disabled={loading || success}
              />
              <p className="text-xs text-gray-500 mt-1">* Campo obligatorio</p>
            </div>

            {/* NIT o CI */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                <CreditCard size={16} />
                NIT o CI (opcional)
              </label>
              <input
                type="text"
                name="nitCi"
                value={formData.nitCi}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Ej: 1234567"
                maxLength={20}
                disabled={loading || success}
              />
            </div>

            {/* Correo Electrónico */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                <Mail size={16} />
                Correo Electrónico (opcional)
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Ej: cliente@email.com"
                maxLength={100}
                disabled={loading || success}
              />
            </div>

          </div>

          {/* Información adicional */}
          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-xs font-medium text-blue-700 mb-2">Información del Cliente</p>
            <div className="grid grid-cols-2 gap-2 text-xs text-gray-600">
              <div>
                <span className="font-medium">Fecha de Registro:</span>{' '}
                {new Date(cliente.fechaRegistro).toLocaleDateString('es-BO')}
              </div>
              <div>
                <span className="font-medium">Tipo:</span>{' '}
                <span
                  className={`px-2 py-0.5 rounded-full ${
                    cliente.tipoCliente === 'REGISTRADO'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-gray-100 text-gray-700'
                  }`}
                >
                  {cliente.tipoCliente}
                </span>
              </div>
            </div>
          </div>

          {/* Botones de acción */}
          <div className="flex gap-4 mt-6">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium transition-colors"
              disabled={loading || success}
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading || success}
              className="flex-1 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  Guardando...
                </>
              ) : success ? (
                <>
                  <CheckCircle size={20} />
                  Guardado
                </>
              ) : (
                <>
                  <CheckCircle size={20} />
                  Guardar Cambios
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}