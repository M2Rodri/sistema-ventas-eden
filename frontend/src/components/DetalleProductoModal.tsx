'use client';

import { useState, useEffect } from 'react';
import { X, Package, MapPin, Calendar, DollarSign } from 'lucide-react';
import { Inventario, MovimientoInventario } from '@/types/inventario';
import { getHistorialAjustes } from '@/lib/api';

interface DetalleProductoModalProps {
  inventario: Inventario;
  onClose: () => void;
}

export default function DetalleProductoModal({ inventario, onClose }: DetalleProductoModalProps) {
  // Antes acá había un precio fijo de 1500 Bs y cinco movimientos escritos a
  // mano (fechas de 2024, usuarios "Admin" y "Vendedor1"), iguales para
  // cualquier producto que se abriera. Ahora sale todo del backend.
  const [movimientos, setMovimientos] = useState<MovimientoInventario[]>([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    let activo = true;
    getHistorialAjustes(inventario.idProducto)
      .then((datos) => activo && setMovimientos(datos.slice(0, 5)))
      .catch(() => {})
      .finally(() => activo && setCargando(false));
    return () => { activo = false; };
  }, [inventario.idProducto]);

  const costo = Number(inventario.costoReferencial ?? 0);
  const valorTotal = inventario.cantidadDisponible * costo;

  const esEntrada = (tipo: string) =>
    ['ENTRADA', 'COMPRA', 'DEVOLUCION', 'AJUSTE_INICIAL'].includes(tipo);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-primary-50">
          <div className="flex items-center gap-3">
            <Package className="text-primary-600" size={28} />
            <div>
              <h2 className="text-xl font-bold text-gray-900">Detalles del Producto</h2>
              <p className="text-sm text-gray-600">{inventario.skuProducto}</p>
            </div>
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
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-3">Información General</h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-gray-50 p-4 rounded-lg">
                <p className="text-xs text-gray-500 mb-1">Nombre del Producto</p>
                <p className="font-medium text-gray-900">{inventario.nombreProducto}</p>
              </div>
              <div className="bg-gray-50 p-4 rounded-lg">
                <p className="text-xs text-gray-500 mb-1">Código SKU</p>
                <p className="font-mono font-medium text-gray-900">{inventario.skuProducto}</p>
              </div>
              <div className="bg-gray-50 p-4 rounded-lg">
                <p className="text-xs text-gray-500 mb-1">Categoría</p>
                <p className="font-medium text-gray-900">
                  {inventario.nombreProducto.toLowerCase().includes('cama') ? 'Camas' : 
                   inventario.nombreProducto.toLowerCase().includes('colchon') ? 'Colchones' : 
                   inventario.nombreProducto.toLowerCase().includes('almohada') ? 'Almohadas' : 'Otro'}
                </p>
              </div>
              <div className="bg-gray-50 p-4 rounded-lg">
                <p className="text-xs text-gray-500 mb-1">Descripción</p>
                <p className="font-medium text-gray-900">Producto de alta calidad</p>
              </div>
            </div>
          </div>

          {/* Stock e Inventario */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-3">Stock e Inventario</h3>
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-green-50 border border-green-200 p-4 rounded-lg text-center">
                <p className="text-xs text-gray-600 mb-1">Stock Actual</p>
                <p className="text-3xl font-bold text-green-600">{inventario.cantidadDisponible}</p>
              </div>
              <div className="bg-orange-50 border border-orange-200 p-4 rounded-lg text-center">
                <p className="text-xs text-gray-600 mb-1">Stock Mínimo</p>
                <p className="text-3xl font-bold text-orange-600">{inventario.stockMinimo}</p>
              </div>
              <div className={`border p-4 rounded-lg text-center ${
                inventario.bajoStockMinimo ? 'bg-red-50 border-red-200' : 'bg-blue-50 border-blue-200'
              }`}>
                <p className="text-xs text-gray-600 mb-1">Estado de Alerta</p>
                <p className={`text-lg font-bold ${inventario.bajoStockMinimo ? 'text-red-600' : 'text-blue-600'}`}>
                  {inventario.bajoStockMinimo ? 'ACTIVA' : 'INACTIVA'}
                </p>
              </div>
            </div>
          </div>

          {/* Ubicación y Valor */}
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <MapPin className="text-gray-600" size={20} />
                <p className="text-sm font-medium text-gray-900">Ubicación Física</p>
              </div>
              <p className="text-lg font-semibold text-gray-700">
                {inventario.ubicacion || 'No especificada'}
              </p>
            </div>
            <div className="bg-green-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <DollarSign className="text-green-600" size={20} />
                <p className="text-sm font-medium text-gray-900">Valor Total del Stock</p>
              </div>
              <p className="text-lg font-semibold text-green-600">
                Bs {valorTotal.toLocaleString('es-BO', { minimumFractionDigits: 2 })}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                {inventario.cantidadDisponible} × Bs {costo.toLocaleString('es-BO', { minimumFractionDigits: 2 })} (costo de referencia)
              </p>
            </div>
          </div>

          {/* Última Actualización */}
          <div className="bg-blue-50 p-4 rounded-lg">
            <div className="flex items-center gap-2 mb-2">
              <Calendar className="text-blue-600" size={20} />
              <p className="text-sm font-medium text-gray-900">Última Fecha de Actualización</p>
            </div>
            <p className="text-lg font-semibold text-blue-600">
              {new Date(inventario.fechaActualizacion).toLocaleString('es-BO')}
            </p>
          </div>

          {/* Últimos 5 Movimientos */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-3">Últimos movimientos</h3>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Fecha</th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Tipo</th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Usuario</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {cargando ? (
                    <tr><td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-500">Cargando…</td></tr>
                  ) : movimientos.length === 0 ? (
                    <tr><td colSpan={4} className="px-4 py-6 text-center text-sm text-gray-500">
                      Este producto todavía no tiene movimientos
                    </td></tr>
                  ) : (
                    movimientos.map((mov) => (
                      <tr key={mov.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-sm text-gray-600">
                          {new Date(mov.fecha).toLocaleString('es-BO', {
                            day: '2-digit', month: '2-digit', year: 'numeric',
                            hour: '2-digit', minute: '2-digit',
                          })}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                            esEntrada(mov.tipoMovimiento) ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {mov.tipoMovimiento.replace('_', ' ')}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm font-semibold">
                          <span className={mov.cantidad >= 0 ? 'text-green-600' : 'text-red-600'}>
                            {mov.cantidad >= 0 ? `+${mov.cantidad}` : mov.cantidad}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600">{mov.nombreUsuario || 'Sistema'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="w-full px-4 py-2.5 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors font-medium"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}