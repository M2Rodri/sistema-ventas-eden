'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import {
  getAllInventario,
  getProductosConStockBajo,
  getAlertasPendientes,
  atenderAlertaStock,
  reactivarAlertaStock,
} from '@/lib/api';
import { Inventario, AlertaInventario } from '@/types/inventario';
import { Search, Package, AlertTriangle, Edit, History, Settings, Eye, DollarSign, X } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import MovimientoInventarioModal from '@/components/MovimientoInventarioModal';
import DetalleProductoModal from '@/components/DetalleProductoModal';
import ConfigurarStockMinimoModal from '@/components/ConfigurarStockMinimoModal';
import HistorialProductoModal from '@/components/HistorialProductoModal';
import DesgloseValorInventarioModal from '@/components/DesgloseValorInventarioModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';
import StatCard from '@/components/StatCard';
import { mensajeError } from '@/lib/errores';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';
import { useAuth } from '@/hooks/useAuth';

function InventarioContent() {
  const { user } = useAuth();
  const searchParams = useSearchParams();
  const router = useRouter();
  const [inventario, setInventario] = useState<Inventario[]>([]);
  const [filteredInventario, setFilteredInventario] = useState<Inventario[]>([]);
  const [alertas, setAlertas] = useState<AlertaInventario[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [stockFilter, setStockFilter] = useState<string>('TODOS');
  const [categoriaFilter, setCategoriaFilter] = useState<string>('TODOS');
  // Las alertas de stock bajo están ocultas: se despliegan con el botón "Alertas de stock".
  const [showAlertas, setShowAlertas] = useState(false);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
  
  // Modales
  const [isAjusteModalOpen, setIsAjusteModalOpen] = useState(false);
  const [isDetalleModalOpen, setIsDetalleModalOpen] = useState(false);
  const [isStockMinimoModalOpen, setIsStockMinimoModalOpen] = useState(false);
  const [isHistorialModalOpen, setIsHistorialModalOpen] = useState(false);
  const [isDesgloseValorOpen, setIsDesgloseValorOpen] = useState(false);
  const [selectedInventario, setSelectedInventario] = useState<Inventario | null>(null);
  // Alerta que se está por marcar como atendida (pide confirmación antes).
  const [alertaAAtender, setAlertaAAtender] = useState<AlertaInventario | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar datos
  useEffect(() => {
    loadData();
  }, []);

  // Clickear "Inventario" en el menú, aunque ya estés en esta pantalla,
  // tiene que volver todo a su estado normal (mismo mecanismo que Ventas:
  // ver Sidebar.tsx).
  useEffect(() => {
    const resetearFiltros = () => {
      setSearchTerm('');
      setStockFilter('TODOS');
      setCategoriaFilter('TODOS');
      setShowAlertas(false);
    };
    window.addEventListener('inventario:reset-filtros', resetearFiltros);
    return () => window.removeEventListener('inventario:reset-filtros', resetearFiltros);
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled y no all: si se cae el endpoint de alertas, igual queremos
      // mostrar el inventario, que es lo principal.
      const [rInventario, rAlertas] = await Promise.allSettled([
        getAllInventario(),
        getAlertasPendientes(),
      ]);

      const { tomar, fallos } = crearRecolector();
      const inventarioData = tomar(rInventario, 'el inventario', [] as Inventario[]);
      const alertasData = tomar(rAlertas, 'las alertas de stock', [] as AlertaInventario[]);

      // Lo más nuevo arriba: por ID, de mayor a menor.
      const inventarioOrdenado = [...inventarioData].sort((a, b) => Number(b.id) - Number(a.id));
      setInventario(inventarioOrdenado);
      setFilteredInventario(inventarioOrdenado);
      setAlertas(alertasData);
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudo cargar el inventario.'));
    } finally {
      setLoading(false);
    }
  };

  // Filtrar inventario
  useEffect(() => {
    let filtered = inventario;

    // Filtro por búsqueda. Sin tildes: nadie escribe tildes buscando rápido,
    // y antes "colchon" no encontraba "Colchón" (mismo criterio que Ventas).
    if (searchTerm) {
      const normalizar = (s: string) =>
        s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
      const termino = normalizar(searchTerm);
      filtered = filtered.filter(item =>
        normalizar(item.nombreProducto).includes(termino) ||
        normalizar(item.skuProducto).includes(termino)
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

  // Los de éxito se cierran solos; los de error se quedan hasta que el
  // usuario los cierra a mano (el botón X del banner).
  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    if (type === 'success') {
      setTimeout(() => setMessage(null), 4000);
    }
  };

  const handleAjustarStock = (item: Inventario) => {
    setSelectedInventario(item);
    setIsAjusteModalOpen(true);
  };

  const handleVerDetalle = (item: Inventario) => {
    setSelectedInventario(item);
    setIsDetalleModalOpen(true);
  };

  // La campanita de notificaciones linkea acá con ?producto=<id> para que la
  // alerta de un producto lleve directo a su detalle, no solo a la pantalla
  // en general. Se limpia el parámetro de la URL para no reabrir el modal si
  // el usuario recarga la página.
  useEffect(() => {
    if (loading) return;
    const idProducto = searchParams.get('producto');
    if (!idProducto) return;
    const item = inventario.find((i) => i.idProducto === Number(idProducto));
    if (item) {
      handleVerDetalle(item);
    }
    router.replace('/dashboard/inventario');
  }, [loading, inventario, searchParams]);

  const handleConfigurarStockMinimo = (item: Inventario) => {
    setSelectedInventario(item);
    setIsStockMinimoModalOpen(true);
  };

  const handleVerHistorialProducto = (item: Inventario) => {
    setSelectedInventario(item);
    setIsHistorialModalOpen(true);
  };

  const handleAtenderAlerta = async (alerta: AlertaInventario) => {
    try {
      await atenderAlertaStock(alerta.id);
      showMessage('success', `Alerta de ${alerta.nombreProducto} marcada como atendida.`);
      loadData();
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudo marcar la alerta como atendida.'));
    } finally {
      setAlertaAAtender(null);
    }
  };

  const handleReactivarAlerta = async (item: Inventario) => {
    try {
      await reactivarAlertaStock(item.idProducto);
      showMessage('success', `Se vuelve a avisar del stock de ${item.nombreProducto}.`);
      loadData();
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudo reactivar la alerta.'));
    }
  };

  const getStockBadge = (item: Inventario) => {
    if (item.cantidadDisponible === 0) {
      return <span className="px-3 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded-full">Sin Stock</span>;
    } else if (item.bajoStockMinimo && item.alertaAtendida) {
      return (
        <span className="inline-flex items-center gap-2">
          <span className="px-3 py-1 bg-gray-100 text-gray-700 text-xs font-semibold rounded-full">Alerta atendida</span>
          {user?.role === 'ADMIN' && (
            <button
              onClick={() => handleReactivarAlerta(item)}
              className="text-xs text-primary-600 hover:text-primary-800 hover:underline"
              title="Volver a avisar de este producto"
            >
              Reactivar
            </button>
          )}
        </span>
      );
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
    (sum, item) => sum + item.cantidadDisponible * Number(item.precioCompra ?? 0),
    0
  );

  // Arrastrar la tabla desde el encabezado, como si fuera una barra de
  // scroll horizontal. Ver hooks/useDragScrollTable.ts.
  const inventarioDrag = useDragScrollTable([loading, filteredInventario]);

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Inventario' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* INDICADORES SUPERIORES - P4.1 */}
      {/* Cada tarjeta fija TODOS los filtros (búsqueda, categoría, stock,
          alertas), nunca solo el que le importa — mismo criterio que
          Ventas: si dejás algún filtro con lo que quedó de un click
          anterior, la tabla no coincide con lo que la tarjeta dice. */}
      <div className={`grid grid-cols-1 gap-4 mb-6 ${user?.role === 'ADMIN' ? 'md:grid-cols-3' : 'md:grid-cols-2'}`}>
        <StatCard
          titulo="Total de Productos"
          valor={totalProductos}
          icon={<Package size={22} />}
          loading={loading}
          onClick={() => {
            setStockFilter('TODOS');
            setCategoriaFilter('TODOS');
            setSearchTerm('');
          }}
        />

        <StatCard
          titulo="Productos con Stock Bajo"
          valor={productosConAlerta}
          icon={<AlertTriangle size={22} />}
          onClick={() => {
            setStockFilter('STOCK_BAJO');
            setCategoriaFilter('TODOS');
            setSearchTerm('');
          }}
          loading={loading}
        />

        {/* Se calcula con precioCompra: el rol EMPLEADO no ve costos. Al
            clickear muestra el desglose (cantidad × precio de compra de
            cada producto), que es la fuente real de este número — no la
            tabla general, que no muestra precios. */}
        {user?.role === 'ADMIN' && (
          <StatCard
            titulo="Valor Total del Inventario"
            valor={`${valorTotalInventario.toLocaleString('es-BO')} Bs`}
            icon={<DollarSign size={22} />}
            loading={loading}
            onClick={() => setIsDesgloseValorOpen(true)}
          />
        )}
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
            <option value="SIN_STOCK">🚫 Sin stock</option>
            <option value="STOCK_OK">✅ Stock OK</option>
          </select>

          {/*
            Se sacó el botón "Compras" de acá: ya es su propio ítem del menú
            lateral, no hace falta un segundo acceso.
          */}
          {/*
            Se quitó "Configurar Notificaciones": prometía avisar por correo
            cuando un producto cayera bajo el mínimo, pero el sistema no tiene
            servicio de correo. Las alertas sí existen y se ven en esta misma
            pantalla y en el panel de inicio.
          */}

          {/* Alertas de stock: ocultas hasta que se pidan */}
          {!loading && alertas.length > 0 && (
            <button
              onClick={() => setShowAlertas(!showAlertas)}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                showAlertas
                  ? 'bg-red-600 text-white'
                  : 'bg-red-50 text-red-700 border border-red-200 hover:bg-red-100'
              }`}
            >
              <AlertTriangle size={18} />
              Alertas de stock ({alertas.length})
            </button>
          )}
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
        <div className={`p-4 rounded-lg mb-6 flex items-start justify-between gap-3 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="flex-shrink-0 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      )}

      {/* Alertas de Stock Bajo */}
      {!loading && alertas.length > 0 && showAlertas && (
        <div className="mb-6">
          <h2 className="text-lg font-bold text-gray-900 mb-3 flex items-center gap-2">
            <AlertTriangle className="text-red-600" size={22} />
            Alertas de Stock Bajo ({alertas.length})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {alertas.map((alerta) => {
              // El backend ya devuelve cantidadActual/cantidadMinima en vivo
              // (ver InventarioService#aResponseConDatosEnVivo), no lo que
              // quedó guardado cuando se creó la alerta. Acá solo se busca
              // el item completo para poder abrir su modal de detalle.
              const actual = inventario.find((i) => i.idProducto === alerta.idProducto);
              const sinStock = alerta.cantidadActual === 0;
              return (
                <div
                  key={alerta.id}
                  onClick={() => actual && handleVerDetalle(actual)}
                  title="Click para ver detalles del producto"
                  className="bg-red-50 border-2 border-red-200 rounded-lg p-4 hover:shadow-md hover:border-red-300 transition-shadow cursor-pointer"
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-gray-900">{alerta.nombreProducto}</h3>
                        {sinStock && (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-red-600 text-white text-xs font-extrabold uppercase tracking-wide rounded-full shadow-sm ring-2 ring-red-200">
                            <span className="w-1.5 h-1.5 bg-white rounded-full animate-pulse" />
                            Sin stock
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-500">{alerta.skuProducto}</p>
                    </div>
                    <AlertTriangle className="text-red-600 flex-shrink-0" size={20} />
                  </div>
                  <div className="flex items-center justify-between text-sm mb-3">
                    <span className="text-gray-600">Stock actual:</span>
                    <span className="font-bold text-red-700">
                      {alerta.cantidadActual} unidades
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-xs text-gray-600">
                    <span>Stock mínimo:</span>
                    <span>{alerta.cantidadMinima}</span>
                  </div>
                  {user?.role === 'ADMIN' && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setAlertaAAtender(alerta);
                      }}
                      className="mt-3 w-full px-3 py-1.5 bg-white border border-red-200 text-red-700 rounded-lg hover:bg-red-100 transition-colors text-sm font-medium"
                    >
                      Marcar como atendida
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tabla de Inventario */}
      {!loading && (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto" ref={inventarioDrag.scrollContainerRef}>
            <table className="min-w-full divide-y divide-gray-200" ref={inventarioDrag.tableRef}>
              <thead
                ref={inventarioDrag.theadRef}
                className={`bg-gray-50 ${inventarioDrag.hasOverflow ? 'cursor-grab select-none' : ''}`}
                {...inventarioDrag.theadProps}
              >
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">SKU</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Producto</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Categoría</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock actual</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock mínimo</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado de Alerta</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Última Actualización</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredInventario.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">{item.id}</td>
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
                      <span className="text-sm font-semibold text-gray-900">
                        {item.cantidadDisponible}
                      </span>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600 text-center">
                      {item.stockMinimo}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      {getStockBadge(item)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {formatDate(item.fechaActualizacion)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        {/* Solo-ADMIN: el backend ya lo bloquea a EMPLEADO
                            (POST /inventario/ajustar), así que mostrárselo
                            solo lo hacía llenar un formulario para terminar
                            en un error. */}
                        {user?.role === 'ADMIN' && (
                          <button
                            onClick={() => handleAjustarStock(item)}
                            className="flex items-center gap-1 px-2.5 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors text-xs font-medium shadow-sm"
                            title="Ajustar Inventario"
                          >
                            <Edit size={14} />
                            Ajustar
                          </button>
                        )}
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
                          title="Configurar stock mínimo"
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

      {alertaAAtender && (
        <DeleteConfirmModal
          title="Marcar alerta como atendida"
          message={
            `¿Marcar como atendida la alerta de ${alertaAAtender.nombreProducto}?\n\n` +
            `Tiene ${alertaAAtender.cantidadActual} unidades (mínimo ${alertaAAtender.cantidadMinima}). ` +
            `No se te va a volver a avisar de este producto hasta que se reponga y vuelva a bajar. ` +
            `No tenés que comprar nada.`
          }
          confirmLabel="Sí, marcar como atendida"
          onConfirm={() => handleAtenderAlerta(alertaAAtender)}
          onCancel={() => setAlertaAAtender(null)}
        />
      )}

      {isDesgloseValorOpen && (
        <DesgloseValorInventarioModal
          inventario={inventario}
          onClose={() => setIsDesgloseValorOpen(false)}
        />
      )}
    </div>
  );
}

export default function InventarioPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center py-16">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    }>
      <InventarioContent />
    </Suspense>
  );
}