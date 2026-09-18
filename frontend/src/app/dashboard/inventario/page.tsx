'use client';

import { useState, useEffect } from 'react';
import { 
  getAllInventario, 
  getProductosConStockBajo, 
  getAlertasPendientes,
  marcarAlertaAtendida,
  getUltimosAjustes
} from '@/lib/api';
import { Inventario, AlertaInventario, MovimientoInventario } from '@/types/inventario';
import { Search, Package, AlertTriangle, Edit, History, TrendingUp, TrendingDown, Settings, Eye, DollarSign, ShoppingCart } from 'lucide-react';
import Link from 'next/link';
import Breadcrumbs from '@/components/Breadcrumbs';
import MovimientoInventarioModal from '@/components/MovimientoInventarioModal';
import DetalleProductoModal from '@/components/DetalleProductoModal';
import ConfigurarStockMinimoModal from '@/components/ConfigurarStockMinimoModal';
import HistorialProductoModal from '@/components/HistorialProductoModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';
import StatCard from '@/components/StatCard';

export default function InventarioPage() {
  const [inventario, setInventario] = useState<Inventario[]>([]);
  const [filteredInventario, setFilteredInventario] = useState<Inventario[]>([]);
  const [alertas, setAlertas] = useState<AlertaInventario[]>([]);
  const [historial, setHistorial] = useState<MovimientoInventario[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [stockFilter, setStockFilter] = useState<string>('TODOS');
  const [categoriaFilter, setCategoriaFilter] = useState<string>('TODOS');
  const [showHistorial, setShowHistorial] = useState(false);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
  
  // Modales
  const [isAjusteModalOpen, setIsAjusteModalOpen] = useState(false);
  const [isDetalleModalOpen, setIsDetalleModalOpen] = useState(false);
  const [isStockMinimoModalOpen, setIsStockMinimoModalOpen] = useState(false);
  const [isHistorialModalOpen, setIsHistorialModalOpen] = useState(false);
  const [selectedInventario, setSelectedInventario] = useState<Inventario | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar datos
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled y no all: si se cae el endpoint de alertas o el de últimos
      // ajustes, igual queremos mostrar el inventario, que es lo principal.
      const [rInventario, rAlertas, rHistorial] = await Promise.allSettled([
        getAllInventario(),
        getAlertasPendientes(),
        getUltimosAjustes()
      ]);

      const { tomar, fallos } = crearRecolector();
      const inventarioData = tomar(rInventario, 'el inventario', [] as Inventario[]);
      const alertasData = tomar(rAlertas, 'las alertas de stock', [] as AlertaInventario[]);
      const historialData = tomar(rHistorial, 'los últimos ajustes', [] as MovimientoInventario[]);

      setInventario(inventarioData);
      setFilteredInventario(inventarioData);
      setAlertas(alertasData);
      setHistorial(historialData);
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar inventario
  useEffect(() => {
    let filtered = inventario;

    // Filtro por búsqueda
    if (searchTerm) {
      filtered = filtered.filter(item =>
        item.nombreProducto.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.skuProducto.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Filtro por categoría real del producto
    if (categoriaFilter !== 'TODOS') {
      filtered = filtered.filter(item => 
        (item.nombreCategoria ?? '').toLowerCase() === categoriaFilter.toLowerCase()
      );
    }

    // Filtro por stock
    if (stockFilter === 'STOCK_BAJO') {
      filtered = filtered.filter(item => item.bajoStockMinimo);
    } else if (stockFilter === 'SIN_STOCK') {
      filtered = filtered.filter(item => item.cantidadDisponible === 0);
    } else if (stockFilter === 'STOCK_OK') {
      filtered = filtered.filter(item => !item.bajoStockMinimo && item.cantidadDisponible > 0);
    }

    setFilteredInventario(filtered);
  }, [searchTerm, stockFilter, categoriaFilter, inventario]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  const handleAjustarStock = (item: Inventario) => {
    setSelectedInventario(item);
    setIsAjusteModalOpen(true);
  };

  const handleVerDetalle = (item: Inventario) => {
    setSelectedInventario(item);
    setIsDetalleModalOpen(true);
  };

  const handleConfigurarStockMinimo = (item: Inventario) => {
    setSelectedInventario(item);
    setIsStockMinimoModalOpen(true);
  };

  const handleVerHistorialProducto = (item: Inventario) => {
    setSelectedInventario(item);
    setIsHistorialModalOpen(true);
  };

  const handleAtenderAlerta = async (id: number) => {
    try {
      await marcarAlertaAtendida(id);
      showMessage('success', 'Alerta marcada como atendida');
      loadData();
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  const getStockBadge = (item: Inventario) => {
    if (item.cantidadDisponible === 0) {
      return <span className="px-3 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Sin Stock</span>;
    } else if (item.bajoStockMinimo) {
      return <span className="px-3 py-1 bg-yellow-100 text-yellow-800 text-xs font-semibold rounded-full">Con Alerta</span>;
    } else {
      return <span className="px-3 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">Sin Alerta</span>;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Indicadores calculados sobre el inventario ya cargado
  const totalProductos = inventario.length;
  const productosConAlerta = inventario.filter(i => i.bajoStockMinimo).length;
  // Antes esto multiplicaba cada unidad por 1500 Bs "simulando un precio
  // promedio", así que esta pantalla y el panel de inicio mostraban dos
  // valores distintos del mismo inventario. Ahora usa el costo real.
  const valorTotalInventario = inventario.reduce(
    (sum, item) => sum + item.cantidadDisponible * Number(item.costoReferencial ?? 0),
    0
  );

  if (loading) {
    return (
      <div>
        <Breadcrumbs items={[{ label: 'Inventario' }]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Inventario' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* INDICADORES SUPERIORES - P4.1 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <StatCard
          titulo="Total de Productos"
          valor={totalProductos}
          icon={<Package size={22} />}
        />

        <StatCard
          titulo="Productos con Stock Bajo"
          valor={productosConAlerta}
          icon={<AlertTriangle size={22} />}
          onClick={() => setStockFilter(stockFilter === 'STOCK_BAJO' ? 'TODOS' : 'STOCK_BAJO')}
          className="hover:border-slate-500"
        />

        <StatCard
          titulo="Valor Total del Inventario"
          valor={`${valorTotalInventario.toLocaleString('es-BO')} Bs`}
          icon={<DollarSign size={22} />}
        />
      </div>

      {/* Header con filtros responsive */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        {/* Título */}
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Control de Inventario</h1>
          <p className="text-gray-600 mt-1">Gestiona el stock y alertas de productos</p>
        </div>
        
        {/* Filtros - wrap automático en pantallas pequeñas */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar por nombre o código..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            {searchTerm && (
              <button
                onClick={() => setSearchTerm('')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            )}
          </div>

          {/* Filtro por Categoría - NUEVO */}
          <select
            value={categoriaFilter}
            onChange={(e) => setCategoriaFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todas las Categorías</option>
            {/*
              Las opciones salen de las categorías que realmente tienen
              productos en inventario. Antes eran una lista fija en minúscula
              que no coincidía con los nombres reales.
            */}
            {Array.from(
              new Set(
                inventario
                  .map((i) => i.nombreCategoria)
                  .filter((c): c is string => !!c)
              )
            )
              .sort()
              .map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
          </select>

          {/* Filtro por Estado de Alerta */}
          <select
            value={stockFilter}
            onChange={(e) => setStockFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los Estados</option>
            <option value="STOCK_BAJO">⚠️ Con Alerta</option>
            <option value="SIN_STOCK">🚫 Sin Alerta</option>
            <option value="STOCK_OK">✅ Stock OK</option>
          </select>

          {/* Botón Configurar Notificaciones - NUEVO */}
          {/*
            Compras vive dentro de Inventario porque ese es el recorrido real:
            se ve que falta stock, se registra la compra, llega, y el stock sube.
          */}
          <Link
            href="/dashboard/compras"
            className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
            title="Compras a proveedores"
          >
            <ShoppingCart size={20} />
            <span className="hidden sm:inline">Compras</span>
          </Link>

          {/*
            Se quitó "Configurar Notificaciones": prometía avisar por correo
            cuando un producto cayera bajo el mínimo, pero el sistema no tiene
            servicio de correo. Las alertas sí existen y se ven en esta misma
            pantalla y en el panel de inicio.
          */}

          {/* Botón Historial */}
          <button
            onClick={() => setShowHistorial(!showHistorial)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
              showHistorial
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <History size={20} />
            <span className="hidden sm:inline">Historial</span>
          </button>
        </div>
      </div>

      {/* Contador de resultados */}
      {searchTerm && (
        <p className="text-sm text-gray-600 mb-4">
          Mostrando <strong>{filteredInventario.length}</strong> productos
        </p>
      )}

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Alertas de Stock Bajo */}
      {alertas.length > 0 && !showHistorial && (
        <div className="mb-6">
          <h2 className="text-lg font-bold text-gray-900 mb-3 flex items-center gap-2">
            <AlertTriangle className="text-red-600" size={22} />
            Alertas de Stock Bajo ({alertas.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {alertas.map((alerta) => (
              <div key={alerta.id} className="bg-red-50 border-2 border-red-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between mb-2">
                  <div className="flex-1">
                    <h3 className="font-semibold text-gray-900">{alerta.nombreProducto}</h3>
                    <p className="text-xs text-gray-500">{alerta.skuProducto}</p>
                  </div>
                  <AlertTriangle className="text-red-600 flex-shrink-0" size={20} />
                </div>
                <div className="flex items-center justify-between text-sm mb-3">
                  <span className="text-gray-600">Stock actual:</span>
                  <span className="font-bold text-red-700">{alerta.cantidadActual} unidades</span>
                </div>
                <div className="flex items-center justify-between text-xs text-gray-600 mb-3">
                  <span>Stock mínimo:</span>
                  <span>{alerta.cantidadMinima}</span>
                </div>
                <button
                  onClick={() => handleAtenderAlerta(alerta.id)}
                  className="w-full px-3 py-1.5 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-sm font-medium"
                >
                  Marcar como Atendida
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Historial de Ajustes */}
      {showHistorial && (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden mb-6">
          <div className="p-4 bg-gray-50 border-b border-gray-200">
            <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
              <History size={20} />
              Historial de Ajustes (Últimos 50)
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Fecha</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Producto</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Tipo</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Cantidad</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock Anterior</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock Nuevo</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Usuario</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Motivo</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {historial.map((ajuste) => (
                  <tr key={ajuste.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {formatDate(ajuste.fecha)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{ajuste.nombreProducto}</div>
                      <div className="text-xs text-gray-500">{ajuste.skuProducto}</div>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <span className={`flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full w-fit ${
                        ajuste.tipoMovimiento === 'ENTRADA' 
                          ? 'bg-green-100 text-green-800' 
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {ajuste.tipoMovimiento === 'ENTRADA' ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
                        {ajuste.tipoMovimiento}
                      </span>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-semibold">
                      <span className={ajuste.tipoMovimiento === 'ENTRADA' ? 'text-green-600' : 'text-red-600'}>
                        {ajuste.tipoMovimiento === 'ENTRADA' ? '+' : '-'}{Math.abs(ajuste.cantidad)}
                      </span>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600 text-center">
                      {ajuste.cantidadAnterior}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-semibold text-gray-900 text-center">
                      {ajuste.cantidadNueva}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {ajuste.nombreUsuario}
                    </td>
                    <td className="px-4 py-4 text-sm text-gray-600 max-w-xs">
                      <span className="line-clamp-2">{ajuste.motivo}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {historial.length === 0 && (
              <div className="text-center py-12">
                <History size={48} className="mx-auto text-gray-400 mb-4" />
                <p className="text-gray-500">No hay ajustes registrados</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tabla de Inventario */}
      {!showHistorial && (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Código</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Producto</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Categoría</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock Actual</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock Mínimo</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Ubicación</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado de Alerta</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Última Actualización</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredInventario.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                      {item.skuProducto}
                    </td>
                    <td 
                      className="px-4 py-4 whitespace-nowrap cursor-pointer hover:text-primary-600"
                      onClick={() => handleVerDetalle(item)}
                      title="Click para ver detalles"
                    >
                      <div className="text-sm font-medium text-gray-900 max-w-xs truncate">
                        {item.nombreProducto}
                      </div>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {item.nombreCategoria || 'Sin categoría'}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-center">
                      <span className={`text-lg font-bold ${
                        item.cantidadDisponible === 0 ? 'text-red-600' :
                        item.bajoStockMinimo ? 'text-yellow-600' :
                        'text-green-600'
                      }`}>
                        {item.cantidadDisponible}
                      </span>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600 text-center">
                      {item.stockMinimo}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {item.ubicacion || '-'}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      {getStockBadge(item)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {formatDate(item.fechaActualizacion)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={() => handleAjustarStock(item)}
                          className="flex items-center gap-1 px-2.5 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-xs font-medium shadow-sm"
                          title="Ajustar Inventario"
                        >
                          <Edit size={14} />
                          Ajustar
                        </button>
                        <button
                          onClick={() => handleVerHistorialProducto(item)}
                          className="p-1.5 border border-gray-200 bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-lg transition-colors"
                          title="Ver Historial"
                        >
                          <History size={16} />
                        </button>
                        <button
                          onClick={() => handleConfigurarStockMinimo(item)}
                          className="p-1.5 border border-gray-200 bg-white text-gray-600 hover:text-primary-700 hover:bg-gray-50 rounded-lg transition-colors"
                          title="Configurar Stock Mínimo"
                        >
                          <Settings size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {filteredInventario.length === 0 && (
              <div className="text-center py-12">
                <Package size={48} className="mx-auto text-gray-400 mb-4" />
                <p className="text-gray-500">No se encontraron productos en inventario</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODALES */}
      {isAjusteModalOpen && selectedInventario && (
        <MovimientoInventarioModal
          inventario={selectedInventario}
          onClose={() => setIsAjusteModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsAjusteModalOpen(false);
            showMessage('success', 'Inventario ajustado correctamente');
          }}
        />
      )}

      {isDetalleModalOpen && selectedInventario && (
        <DetalleProductoModal
          inventario={selectedInventario}
          onClose={() => setIsDetalleModalOpen(false)}
        />
      )}

      {isStockMinimoModalOpen && selectedInventario && (
        <ConfigurarStockMinimoModal
          inventario={selectedInventario}
          onClose={() => setIsStockMinimoModalOpen(false)}
          onSuccess={() => {
            // Sin este loadData la tabla seguía mostrando el mínimo viejo
            // hasta que el usuario recargaba con F5.
            loadData();
            setIsStockMinimoModalOpen(false);
            showMessage('success', 'Stock mínimo configurado exitosamente');
          }}
        />
      )}

      {isHistorialModalOpen && selectedInventario && (
        <HistorialProductoModal
          inventario={selectedInventario}
          onClose={() => setIsHistorialModalOpen(false)}
        />
      )}
    </div>
  );
}