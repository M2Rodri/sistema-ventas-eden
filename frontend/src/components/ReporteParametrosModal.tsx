'use client';

import { useState } from 'react';
import { X, Calendar, Hash, Eye, AlertCircle } from 'lucide-react';
import { TipoReporte, ConfiguracionReporte } from '@/types/reporte';
import ReporteVistaPrevia from './ReporteVistaPrevia';

interface ReporteParametrosModalProps {
  tipoReporte: TipoReporte;
  configuracion: ConfiguracionReporte;
  onClose: () => void;
}

export default function ReporteParametrosModal({ 
  tipoReporte, 
  configuracion, 
  onClose 
}: ReporteParametrosModalProps) {
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [limite, setLimite] = useState(10);
  const [mostrarVistaPrevia, setMostrarVistaPrevia] = useState(false);
  const [errorParametros, setErrorParametros] = useState<string | null>(null);

  const handleVistaPrevia = () => {
    if (configuracion.requiereFechas && (!fechaInicio || !fechaFin)) {
      setErrorParametros('Seleccioná el rango de fechas para generar el reporte.');
      return;
    }
    setErrorParametros(null);
    setMostrarVistaPrevia(true);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">{configuracion.titulo}</h2>
            <p className="text-sm text-gray-600 mt-1">{configuracion.descripcion}</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        {/* Contenido */}
        <div className="p-6">
          {!mostrarVistaPrevia ? (
            <>
              {errorParametros && (
                <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
                  <AlertCircle className="text-red-600 flex-shrink-0" size={20} />
                  <p className="text-sm text-red-700">{errorParametros}</p>
                </div>
              )}

              {/* Parámetros */}
              <div className="space-y-6">
                {/* Rango de Fechas */}
                {configuracion.requiereFechas && (
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-3">
                      <Calendar size={18} className="text-primary-600" />
                      Rango de Fechas
                    </label>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs text-gray-600 mb-1">Fecha Inicio</label>
                        <input
                          type="date"
                          value={fechaInicio}
                          onChange={(e) => { setFechaInicio(e.target.value); setErrorParametros(null); }}
                          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-600 mb-1">Fecha Fin</label>
                        <input
                          type="date"
                          value={fechaFin}
                          onChange={(e) => { setFechaFin(e.target.value); setErrorParametros(null); }}
                          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* Límite */}
                {configuracion.requiereLimite && (
                  <div>
                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-3">
                      <Hash size={18} className="text-primary-600" />
                      Límite de Resultados
                    </label>
                    <select
                      value={limite}
                      onChange={(e) => setLimite(parseInt(e.target.value))}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                    >
                      <option value={5}>Top 5</option>
                      <option value={10}>Top 10</option>
                      <option value={20}>Top 20</option>
                      <option value={50}>Top 50</option>
                    </select>
                  </div>
                )}

              </div>

              {/* Botones de Acción */}
              <div className="flex flex-col sm:flex-row gap-3 mt-8 pt-6 border-t border-gray-200">
                <button
                  onClick={onClose}
                  className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleVistaPrevia}
                  className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
                >
                  <Eye size={20} />
                  Ver reporte
                </button>
              </div>
            </>
          ) : (
            <ReporteVistaPrevia
              tipoReporte={tipoReporte}
              fechaInicio={fechaInicio}
              fechaFin={fechaFin}
              limite={limite}
              onVolver={() => setMostrarVistaPrevia(false)}
            />
          )}
        </div>
      </div>
    </div>
  );
}