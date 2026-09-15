'use client';

import { useState, useEffect } from 'react';
import { X, History, Filter, AlertCircle, PackageOpen } from 'lucide-react';
import { Inventario, MovimientoInventario } from '@/types/inventario';
import { getHistorialAjustes } from '@/lib/api';

interface HistorialProductoModalProps {
  inventario: Inventario;
  onClose: () => void;
}

/**
 * Historial de movimientos de un producto.
 *
 * Antes esta pantalla mostraba una lista inventada en el código: movimientos
 * de 2024, usuarios "Juan Pérez" y "María López", e incluso direcciones IP
 * falsas — todo idéntico para cualquier producto que se abriera. El endpoint
 * real existía sin usarse.
 */
export default function HistorialProductoModal({ inventario, onClose }: HistorialProductoModalProps) {
  const [movimientos, setMovimientos] = useState<MovimientoInventario[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tipoFiltro, setTipoFiltro] = useState<string>('TODOS');

  useEffect(() => {
    let activo = true;
    (async () => {
      try {
        const datos = await getHistorialAjustes(inventario.idProducto);
        if (activo) setMovimientos(datos);
      } catch (err: any) {
        if (activo) setError(err?.message ?? 'No se pudo cargar el historial');
      } finally {
        if (activo) setCargando(false);
      }
    })();
    return () => { activo = false; };
  }, [inventario.idProducto]);

  const fechaHora = (f: string) =>
    new Date(f).toLocaleString('es-BO', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });

  const colorTipo = (tipo: string) => {
    const entradas = ['ENTRADA', 'COMPRA', 'DEVOLUCION', 'AJUSTE_INICIAL'];
    if (entradas.includes(tipo)) return 'bg-green-100 text-green-800';
    if (tipo === 'MERMA') return 'bg-red-100 text-red-800';
    return 'bg-orange-100 text-orange-800';
  };

  const tiposPresentes = Array.from(new Set(movimientos.map((m) => m.tipoMovimiento))).sort();
  const visibles = tipoFiltro === 'TODOS'
    ? movimientos
    : movimientos.filter((m) => m.tipoMovimiento === tipoFiltro);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
          <div>
            <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
              <History className="text-primary-600" size={24} />
              Historial de movimientos
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              {inventario.nombreProducto}{' '}
              <span className="text-gray-400">({inventario.skuProducto})</span>
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X size={24} />
          </button>
        </div>

        <div className="p-6">
          {/* Estado actual */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <div>
              <p className="text-xs text-gray-500">Stock actual</p>
              <p className="text-2xl font-bold text-gray-900">{inventario.cantidadDisponible}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Stock mínimo</p>
              <p className="text-2xl font-bold text-orange-600">{inventario.stockMinimo}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Movimientos</p>
              <p className="text-2xl font-bold text-gray-900">{movimientos.length}</p>
            </div>
          </div>

          {tiposPresentes.length > 1 && (
            <div className="flex items-center gap-2 mb-4">
              <Filter size={16} className="text-gray-500" />
              <select
                value={tipoFiltro}
                onChange={(e) => setTipoFiltro(e.target.value)}
                className="px-3 py-1.5 border border-gray-300 rounded-lg bg-white text-sm"
              >
                <option value="TODOS">Todos los tipos</option>
                {tiposPresentes.map((t) => (
                  <option key={t} value={t}>{t.replace('_', ' ')}</option>
                ))}
              </select>
            </div>
          )}

          {error && (
            <div className="flex items-center gap-2 p-4 bg-red-50 text-red-800 rounded-lg mb-4">
              <AlertCircle size={20} />
              <span className="text-sm">{error}</span>
            </div>
          )}

          {cargando ? (
            <p className="text-center py-10 text-gray-500">Cargando historial…</p>
          ) : visibles.length === 0 ? (
            <div className="text-center py-12 bg-gray-50 border border-gray-200 rounded-lg">
              <PackageOpen size={40} className="mx-auto text-gray-400 mb-3" />
              <p className="text-gray-600 font-medium">
                {movimientos.length === 0
                  ? 'Este producto todavía no tiene movimientos'
                  : 'Ningún movimiento de ese tipo'}
              </p>
              <p className="text-sm text-gray-500 mt-1">
                Las compras recibidas y las ventas quedan registradas acá.
              </p>
            </div>
          ) : (
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Fecha</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tipo</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Motivo</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Antes</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Cambio</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Después</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Responsable</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {visibles.map((m) => (
                      <tr key={m.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-sm text-gray-700 whitespace-nowrap">
                          {fechaHora(m.fecha)}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${colorTipo(m.tipoMovimiento)}`}>
                            {m.tipoMovimiento.replace('_', ' ')}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          {m.motivo || '—'}
                          {m.observacion && (
                            <span className="block text-xs text-gray-500">{m.observacion}</span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-sm text-center text-gray-600">{m.cantidadAnterior}</td>
                        <td className={`px-4 py-3 text-sm text-center font-semibold ${
                          m.cantidad >= 0 ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {m.cantidad >= 0 ? `+${m.cantidad}` : m.cantidad}
                        </td>
                        <td className="px-4 py-3 text-sm text-center font-semibold text-gray-900">{m.cantidadNueva}</td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          {m.nombreUsuario || 'Sistema'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="px-6 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
