'use client';

import { Auditoria } from '@/types/configuracion';
import { X } from 'lucide-react';

interface AuditoriaDetalleModalProps {
  auditoria: Auditoria;
  onClose: () => void;
}

export default function AuditoriaDetalleModal({ auditoria, onClose }: AuditoriaDetalleModalProps) {
  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const getAccionColor = (accion: string) => {
    const colors: { [key: string]: string } = {
      CREATE: 'text-green-700 bg-green-100',
      UPDATE: 'text-blue-700 bg-blue-100',
      DELETE: 'text-red-700 bg-red-100',
      LOGIN: 'text-purple-700 bg-purple-100',
      LOGOUT: 'text-gray-700 bg-gray-100',
    };
    return colors[accion] || 'text-gray-700 bg-gray-100';
  };

  // Intentar parsear detalles como JSON
  let detallesFormateados = auditoria.detalles;
  try {
    const parsed = JSON.parse(auditoria.detalles);
    detallesFormateados = JSON.stringify(parsed, null, 2);
  } catch {
    // Si no es JSON válido, mostrar tal cual
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Detalle de Auditoría</h2>
            <p className="text-sm text-gray-500 mt-1">ID: {auditoria.id}</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Contenido */}
        <div className="p-6 space-y-6">
          {/* Información General */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 uppercase mb-1">
                Usuario
              </label>
              <p className="text-sm font-semibold text-gray-900">{auditoria.nombreUsuario}</p>
              {auditoria.idUsuario && (
                <p className="text-xs text-gray-500">ID: {auditoria.idUsuario}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 uppercase mb-1">
                Acción
              </label>
              <span className={`inline-flex px-3 py-1 text-sm font-semibold rounded-full ${getAccionColor(auditoria.accion)}`}>
                {auditoria.accion}
              </span>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 uppercase mb-1">
                Tabla Afectada
              </label>
              <p className="text-sm font-semibold text-gray-900">{auditoria.tablaAfectada}</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 uppercase mb-1">
                ID del Registro
              </label>
              <p className="text-sm font-mono text-gray-900">{auditoria.idRegistro}</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 uppercase mb-1">
                Fecha y Hora
              </label>
              <p className="text-sm text-gray-900">{formatDateTime(auditoria.fechaHora)}</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 uppercase mb-1">
                Dirección IP
              </label>
              <p className="text-sm font-mono text-gray-900">{auditoria.ipDispositivo || 'No disponible'}</p>
            </div>
          </div>

          {/* Detalles */}
          <div>
            <label className="block text-xs font-medium text-gray-500 uppercase mb-2">
              Detalles de la Operación
            </label>
            <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
              <pre className="text-xs text-gray-800 whitespace-pre-wrap font-mono overflow-x-auto">
                {detallesFormateados}
              </pre>
            </div>
          </div>

          {/* Nota de Seguridad */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
            <p className="text-xs text-yellow-800">
              <strong>🔒 Registro Inmutable:</strong> Este registro de auditoría no puede ser modificado ni eliminado por razones de seguridad y cumplimiento.
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 p-6 border-t border-gray-200">
          <button
            onClick={onClose}
            className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
