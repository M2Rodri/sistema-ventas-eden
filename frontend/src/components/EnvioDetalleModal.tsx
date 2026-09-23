'use client';

import { useState } from 'react';
import { cambiarEstadoEnvio, marcarEnvioEntregado } from '@/lib/api';
import { Envio, EstadoEnvio } from '@/types/envio';
import { X, Package, Clock, Truck, CheckCircle, MapPin, Phone, Home } from 'lucide-react';

interface EnvioDetalleModalProps {
  envio: Envio;
  onClose: () => void;
  onSuccess: () => void;
}

export default function EnvioDetalleModal({ envio, onClose, onSuccess }: EnvioDetalleModalProps) {
  const [nuevoEstado, setNuevoEstado] = useState<EstadoEnvio>(envio.estadoSeguimiento);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const estados: EstadoEnvio[] = [
    'PENDIENTE',
    'EN_PREPARACION',
    'EN_CAMINO',
    'ENTREGADO',
    'DEVUELTO',
    'CANCELADO'
  ];

  const handleCambiarEstado = async () => {
    if (nuevoEstado === envio.estadoSeguimiento) {
      setError('Debes seleccionar un estado diferente');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      if (nuevoEstado === 'ENTREGADO') {
        await marcarEnvioEntregado(envio.id);
      } else {
        await cambiarEstadoEnvio(envio.id, nuevoEstado);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getEstadoIcon = (estado: EstadoEnvio) => {
    const icons = {
      PENDIENTE: <Clock size={20} className="text-gray-600" />,
      EN_PREPARACION: <Package size={20} className="text-blue-600" />,
      EN_CAMINO: <Truck size={20} className="text-yellow-600" />,
      ENTREGADO: <CheckCircle size={20} className="text-green-600" />,
      DEVUELTO: <MapPin size={20} className="text-orange-600" />,
      CANCELADO: <MapPin size={20} className="text-red-600" />,
    };
    return icons[estado];
  };

  const getEstadoColor = (estado: EstadoEnvio) => {
    const colors = {
      PENDIENTE: 'bg-gray-100 text-gray-800 border-gray-300',
      EN_PREPARACION: 'bg-blue-100 text-blue-800 border-blue-300',
      EN_CAMINO: 'bg-yellow-100 text-yellow-800 border-yellow-300',
      ENTREGADO: 'bg-green-100 text-green-800 border-green-300',
      DEVUELTO: 'bg-orange-100 text-orange-800border-orange-300',
      CANCELADO: 'bg-red-100 text-red-800 border-red-300',
    };
    return colors[estado];
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-gradient-to-r from-primary-50 to-primary-100 z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center">
              <Truck className="text-primary-600" size={20} />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">Detalle del Envío</h2>
              {envio.guiaRemision && (
                <p className="text-sm text-gray-500 font-mono">Guía: {envio.guiaRemision}</p>
              )}
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {error && (
            <div className="bg-red-50 text-red-800 p-4 rounded-lg text-sm">
              {error}
            </div>
          )}

          {/* Estado Actual */}
          <div className="bg-gray-50 rounded-lg p-4 border-2 border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Estado Actual</h3>
            <div className={`flex items-center gap-3 px-4 py-3 rounded-lg border-2 ${getEstadoColor(envio.estadoSeguimiento)}`}>
              {getEstadoIcon(envio.estadoSeguimiento)}
              <span className="font-bold text-lg">{envio.estadoSeguimiento.replace('_', ' ')}</span>
            </div>
          </div>

          {/* Línea de Tiempo Visual */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-4">Seguimiento</h3>
            <div className="space-y-4">
              {estados.map((estado, index) => {
                const isActual = estado === envio.estadoSeguimiento;
                const isPasado = estados.indexOf(estado) < estados.indexOf(envio.estadoSeguimiento);
                const isActive = isActual || isPasado;

                return (
                  <div key={estado} className="flex items-start gap-4">
                    <div className="flex flex-col items-center">
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all ${
                        isActive 
                          ? 'bg-primary-100 border-primary-600' 
                          : 'bg-gray-100 border-gray-300'
                      }`}>
                        {getEstadoIcon(estado)}
                      </div>
                      {index < estados.length - 1 && (
                        <div className={`w-0.5 h-8 transition-all ${
                          isPasado ? 'bg-primary-600' : 'bg-gray-300'
                        }`} />
                      )}
                    </div>
                    <div className="flex-1 pb-4">
                      <p className={`font-semibold ${isActive ? 'text-gray-900' : 'text-gray-400'}`}>
                        {estado.replace('_', ' ')}
                      </p>
                      {isActual && (
                        <p className="text-sm text-gray-500 mt-1">
                          Actualizado: {formatDateTime(envio.fechaActualizacion)}
                        </p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Información del Cliente */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <Phone size={16} />
              Información del Cliente
            </h3>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Nombre:</span>
                <span className="text-sm font-medium text-gray-900">{envio.nombreCliente}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Celular:</span>
                <span className="text-sm font-medium text-gray-900">{envio.telefonoCliente}</span>
              </div>
            </div>
          </div>

          {/* Información de Destino */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <Home size={16} />
              Dirección de Destino
            </h3>
            <div className="space-y-2">
              <div>
                <span className="text-sm text-gray-600">Dirección:</span>
                <p className="text-sm font-medium text-gray-900 mt-1">{envio.direccionDestino}</p>
              </div>
              {envio.ciudad && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Ciudad:</span>
                  <span className="text-sm font-medium text-gray-900">{envio.ciudad}</span>
                </div>
              )}
              {envio.departamento && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Departamento:</span>
                  <span className="text-sm font-medium text-gray-900">{envio.departamento}</span>
                </div>
              )}
            </div>
          </div>

          {/* Información de Transportadora */}
          {envio.nombreTransportadora && (
            <div className="bg-white rounded-lg p-4 border border-gray-200">
              <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <Truck size={16} />
                Transportadora
              </h3>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Empresa:</span>
                  <span className="text-sm font-medium text-gray-900">{envio.nombreTransportadora}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Costo de Envío:</span>
                  <span className="text-sm font-semibold text-gray-900">
                    Bs {envio.costoEnvio.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Fechas */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <Clock size={16} />
              Fechas Importantes
            </h3>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Fecha Estimada:</span>
                <span className="text-sm font-medium text-gray-900">{formatDate(envio.fechaEntregaEstimada)}</span>
              </div>
              {envio.fechaEntregaReal && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Fecha Real de Entrega:</span>
                  <span className="text-sm font-medium text-green-600">{formatDate(envio.fechaEntregaReal)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Creado:</span>
                <span className="text-sm text-gray-600">{formatDateTime(envio.fechaCreacion)}</span>
              </div>
            </div>
          </div>

          {/* Referencias */}
          <div className="bg-white rounded-lg p-4 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-3">Referencias</h3>
            <div className="space-y-2">
              {envio.idVenta && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">ID Venta:</span>
                  <span className="text-sm font-mono font-medium text-gray-900">#{envio.idVenta}</span>
                </div>
              )}
            </div>
          </div>

          {/* Notas */}
          {envio.notas && (
            <div className="bg-yellow-50 rounded-lg p-4 border border-yellow-200">
              <h3 className="text-sm font-semibold text-gray-700 mb-2">Notas / Observaciones</h3>
              <p className="text-sm text-gray-700">{envio.notas}</p>
            </div>
          )}

          {/* Cambiar Estado */}
          {envio.estadoSeguimiento !== 'ENTREGADO' && envio.estadoSeguimiento !== 'CANCELADO' && (
            <div className="bg-blue-50 rounded-lg p-4 border-2 border-blue-200">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">Actualizar Estado del Envío</h3>
              <div className="space-y-3">
                <select
                  value={nuevoEstado}
                  onChange={(e) => setNuevoEstado(e.target.value as EstadoEnvio)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
                >
                  {estados.map((estado) => (
                    <option key={estado} value={estado}>
                      {estado.replace('_', ' ')}
                    </option>
                  ))}
                </select>
                <button
                  onClick={handleCambiarEstado}
                  disabled={loading || nuevoEstado === envio.estadoSeguimiento}
                  className="w-full px-6 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:bg-gray-400 disabled:cursor-not-allowed"
                >
                  {loading ? 'Actualizando...' : 'Confirmar Cambio de Estado'}
                </button>
              </div>
            </div>
          )}

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
            <button
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors font-medium"
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}