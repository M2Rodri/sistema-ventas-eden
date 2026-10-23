'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import {
  getAllInventario,
  getProductosConStockBajo,
  getAlertasPendientes,
  atenderAlertaStock,
  reactivarAlertaStock,
  getUltimosAjustes,
} from '@/lib/api';
import { Inventario, AlertaInventario, MovimientoInventario } from '@/types/inventario';
import { Search, Package, AlertTriangle, Edit, History, Settings, Eye, DollarSign, X, RefreshCw, Clock } from 'lucide-react';
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
import Paginacion from '@/components/Paginacion';

const ITEMS_PER_PAGE = 30;

function InventarioContent() {
  const { user } = useAuth();
  const searchParams = useSearchParams();
  const router = useRouter();
  const [inventario, setInventario] = useState<Inventario[]>([]);
  const [filteredInventario, setFilteredInventario] = useState<Inventario[]>([]);
  const [alertas, setAlertas] = useState<AlertaInventario[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [stockFilter, setStockFilter] = useState<string>('TODOS');
  const [categoriaFilter, setCategoriaFilter] = useState<string>('TODOS');
  // Las alertas de stock bajo están ocultas: se despliegan con el botón "Alertas de stock".
  const [showAlertas, setShowAlertas] = useState(false);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);

  // Vista: false = Inventario normal, true = Movimientos recientes
  const [mostrarMovimientos, setMostrarMovimientos] = useState(false);

  // Estados para Movimientos Recientes
  const [movimientos, setMovimientos] = useState<MovimientoInventario[]>([]);
  const [filteredMovimientos, setFilteredMovimientos] = useState<MovimientoInventario[]>([]);
  const [loadingMovimientos, setLoadingMovimientos] = useState(false);
  const [movimientosCargados, setMovimientosCargados] = useState(false);
  const [searchMovimiento, setSearchMovimiento] = useState('');
  const [filtroFecha, setFiltroFecha] = useState<'HOY' | 'AYER' | 'FECHA'>('HOY');
  const [fechaElegida, setFechaElegida] = useState('');
  const [filtroOperacion, setFiltroOperacion] = useState<'TODOS' | 'COMPRA' | 'VENTA' | 'CARGA'>('TODOS');
  const [currentMovimientosPage, setCurrentMovimientosPage] = useState(1);
  const ITEMS_PER_PAGE_MOVIMIENTOS = 25;
  
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

  // Clic en "Inventario" del menú estando en esta pantalla la devuelve a su estado normal (ver Sidebar.tsx).
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
      // allSettled: si falla el endpoint de alertas, igual se muestra el inventario.
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

    // Búsqueda sin tildes, como en Ventas.
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

    setCurrentPage(1);
    setFilteredInventario(filtered);
  }, [searchTerm, stockFilter, categoriaFilter, inventario]);

  // Cargar movimientos recientes
  const loadMovimientos = async () => {
    try {
      setLoadingMovimientos(true);
      const data = await getUltimosAjustes();
      setMovimientos(data);
      setFilteredMovimientos(data);
      setMovimientosCargados(true);
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudieron cargar los movimientos recientes.'));
    } finally {
      setLoadingMovimientos(false);
    }
  };

  // Filtrar movimientos
  useEffect(() => {
    let filtrados = movimientos;

    // Filtro por texto
    if (searchMovimiento.trim()) {
      const q = searchMovimiento.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      filtrados = filtrados.filter((m) => {
        const prod = (m.nombreProducto ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
        const sku = (m.skuProducto ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
        const mot = (m.motivo ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
        const usu = (m.nombreUsuario ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
        return prod.includes(q) || sku.includes(q) || mot.includes(q) || usu.includes(q);
      });
    }

    // Filtro por fecha (Hoy / Ayer / Fecha elegida)
    const ahora = new Date();
    const aY = ahora.getFullYear();
    const aM = ahora.getMonth();
    const aD = ahora.getDate();

    filtrados = filtrados.filter((m) => {
      const f = new Date(m.fecha);
      const fY = f.getFullYear();
      const fM = f.getMonth();
      const fD = f.getDate();

      if (filtroFecha === 'HOY') {
        return fY === aY && fM === aM && fD === aD;
      }
      if (filtroFecha === 'AYER') {
        const ayer = new Date(ahora);
        ayer.setDate(aD - 1);
        return fY === ayer.getFullYear() && fM === ayer.getMonth() && fD === ayer.getDate();
      }
      if (filtroFecha === 'FECHA' && fechaElegida) {
        const [eY, eM, eD] = fechaElegida.split('-').map(Number);
        return fY === eY && fM === eM - 1 && fD === eD;
      }
      return true;
    });

    // Filtro por operación simple (Compras / Ventas / Cargas)
    if (filtroOperacion !== 'TODOS') {
      filtrados = filtrados.filter((m) => {
        if (filtroOperacion === 'COMPRA') return m.tipoMovimiento === 'COMPRA';
        if (filtroOperacion === 'VENTA') return m.tipoMovimiento === 'VENTA';
        if (filtroOperacion === 'CARGA') {
          return m.tipoMovimiento === 'AJUSTE_INICIAL' || m.tipoMovimiento === 'ENTRADA';
        }
        return true;
      });
    }

    setCurrentMovimientosPage(1);
    setFilteredMovimientos(filtrados);
  }, [searchMovimiento, filtroFecha, fechaElegida, filtroOperacion, movimientos]);

  // Los avisos de éxito se cierran solos; los de error esperan a que el usuario los cierre.
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

  // La campanita enlaza con ?producto=<id> para abrir el detalle; el parámetro se limpia para no reabrir el modal al recargar.
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
      return <span className="px-2.5 py-[3px] bg-red-100 text-red-800 text-xs font-semibold rounded-full">Sin Stock</span>;
    } else if (item.bajoStockMinimo && item.alertaAtendida) {
      return (
        <span className="inline-flex items-center gap-1.5">
          <span className="px-2.5 py-[3px] bg-gray-100 text-gray-700 text-xs font-semibold rounded-full">Alerta atendida</span>
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
      return <span className="px-2.5 py-[3px] bg-yellow-100 text-yellow-800 text-xs font-semibold rounded-full">Con Alerta</span>;
    } else {
      return <span className="px-2.5 py-[3px] bg-green-100 text-green-800 text-xs font-semibold rounded-full">Sin Alerta</span>;
    }
  };

  const formatDateShort = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  const formatTimeShort = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString('es-BO', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  // Indicadores calculados sobre el inventario ya cargado
  const totalProductos = inventario.length;
  const productosConAlerta = inventario.filter(i => i.bajoStockMinimo).length;
  // Usa el costo real del inventario, igual que el panel de inicio.
  const valorTotalInventario = inventario.reduce(
    (sum, item) => sum + item.cantidadDisponible * Number(item.precioCompra ?? 0),
    0
  );

  // Arrastrar la tabla desde el encabezado (ver hooks/useDragScrollTable.ts).
  const inventarioDrag = useDragScrollTable([loading, filteredInventario]);

  // Paginación: se calcula a partir de filteredInventario
  const totalPages = Math.ceil(filteredInventario.length / ITEMS_PER_PAGE);
  const paginatedInventario = filteredInventario.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE,
  );
  const startId = paginatedInventario.length > 0 ? paginatedInventario[0].id : undefined;
  const endId = paginatedInventario.length > 0 ? paginatedInventario[paginatedInventario.length - 1].id : undefined;

  // Arrastrar la tabla de movimientos
  const movimientosDrag = useDragScrollTable([loadingMovimientos, filteredMovimientos]);

  // Paginación de movimientos
  const totalMovimientosPages = Math.ceil(filteredMovimientos.length / ITEMS_PER_PAGE_MOVIMIENTOS);
  const paginatedMovimientos = filteredMovimientos.slice(
    (currentMovimientosPage - 1) * ITEMS_PER_PAGE_MOVIMIENTOS,
    currentMovimientosPage * ITEMS_PER_PAGE_MOVIMIENTOS,
  );
  const startMov = paginatedMovimientos.length > 0 ? (currentMovimientosPage - 1) * ITEMS_PER_PAGE_MOVIMIENTOS + 1 : undefined;
  const endMov = paginatedMovimientos.length > 0 ? Math.min(currentMovimientosPage * ITEMS_PER_PAGE_MOVIMIENTOS, filteredMovimientos.length) : undefined;

  const getInfoFecha = (fechaIso: string) => {
    const f = new Date(fechaIso);
    const ahora = new Date();

    const esHoy =
      f.getDate() === ahora.getDate() &&
      f.getMonth() === ahora.getMonth() &&
      f.getFullYear() === ahora.getFullYear();

    const ayer = new Date(ahora);
    ayer.setDate(ahora.getDate() - 1);
    const esAyer =
      f.getDate() === ayer.getDate() &&
      f.getMonth() === ayer.getMonth() &&
      f.getFullYear() === ayer.getFullYear();

    const hora = f.toLocaleTimeString('es-BO', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });

    if (esHoy) {
      return { etiqueta: 'Hoy', hora, esHoy: true, esAyer: false };
    }
    if (esAyer) {
      return { etiqueta: 'Ayer', hora, esHoy: false, esAyer: true };
    }
    return {
      etiqueta: f.toLocaleDateString('es-BO', { day: '2-digit', month: '2-digit', year: 'numeric' }),
      hora,
      esHoy: false,
      esAyer: false,
    };
  };

  const getOperacionTexto = (tipo: string, cantidad: number) => {
    const signo = cantidad > 0 ? `+${cantidad}` : `${cantidad}`;

    if (tipo === 'COMPRA') {
      return (
        <span className="font-semibold text-green-700">
          Compra ({signo})
        </span>
      );
    }
    if (tipo === 'VENTA') {
      return (
        <span className="font-semibold text-blue-700">
          Venta ({signo})
        </span>
      );
    }
    if (tipo === 'AJUSTE_INICIAL') {
      return (
        <span className="font-semibold text-emerald-700">
          Nuevo producto ({signo})
        </span>
      );
    }
    if (tipo === 'ENTRADA') {
      return (
        <span className="font-semibold text-teal-700">
          Carga de stock ({signo})
        </span>
      );
    }
    return (
      <span className="font-medium text-gray-700">
        Ajuste ({signo})
      </span>
    );
  };

  // Movimientos registrados Hoy y el último del día
  const movimientosHoy = movimientos.filter((m) => {
    const f = new Date(m.fecha);
    const ahora = new Date();
    return (
      f.getDate() === ahora.getDate() &&
      f.getMonth() === ahora.getMonth() &&
      f.getFullYear() === ahora.getFullYear()
    );
  });
  const ultimoMovHoy = movimientosHoy.length > 0 ? movimientosHoy[0] : null;

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

      {/* VISTA 1: INVENTARIO NORMAL (Por defecto) */}
      {!mostrarMovimientos && (
        <>
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

              {/* Filtro por Categoría */}
              <select
                value={categoriaFilter}
                onChange={(e) => setCategoriaFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
              >
                <option value="TODOS">Todas las Categorías</option>
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

              {/* Alertas de stock: ocultas hasta que se pidan */}
              {!loading && alertas.length > 0 && (
                <button
                  type="button"
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

              {/* Botón para ver Movimientos Recientes */}
              <button
                type="button"
                onClick={() => {
                  setMostrarMovimientos(true);
                  if (!movimientosCargados) loadMovimientos();
                }}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 bg-white hover:bg-gray-50 text-gray-700 rounded-lg font-medium text-sm transition-colors whitespace-nowrap shadow-sm"
                title="Ver historial de operaciones recientes"
              >
                <History size={16} className="text-gray-500" />
                Movimientos recientes
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
                <table className="w-full table-fixed min-w-[760px] divide-y divide-gray-200" ref={inventarioDrag.tableRef}>
                  <thead
                    ref={inventarioDrag.theadRef}
                    className={`bg-gray-50 ${inventarioDrag.hasOverflow ? 'cursor-grab select-none' : ''}`}
                    {...inventarioDrag.theadProps}
                  >
                    <tr>
                      <th className="w-[5%] px-1.5 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">ID</th>
                      <th className="w-[9%] px-2 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">SKU</th>
                      <th className="w-[25%] px-2.5 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Producto</th>
                      <th className="w-[10%] px-2 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Categoría</th>
                      <th className="w-[7%] px-1.5 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                        <span>STOCK</span>
                        <span className="block">ACTUAL</span>
                      </th>
                      <th className="w-[7%] px-1.5 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                        <span>STOCK</span>
                        <span className="block">MÍNIMO</span>
                      </th>
                      <th className="w-[11%] px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                        <span>ESTADO DE</span>
                        <span className="block">ALERTA</span>
                      </th>
                      <th className="w-[11%] px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                        <span>ÚLTIMA</span>
                        <span className="block">ACTUALIZACIÓN</span>
                      </th>
                      <th className="w-[15%] px-1.5 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {paginatedInventario.map((item) => (
                      <tr key={item.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-1.5 py-[7px] whitespace-nowrap text-sm text-center text-gray-500 font-mono">{item.id}</td>
                        <td className="px-2 py-[7px] whitespace-nowrap text-sm font-mono text-gray-900 truncate" title={item.skuProducto}>
                          {item.skuProducto}
                        </td>
                        <td 
                          className="px-2.5 py-[7px] truncate text-sm font-medium text-gray-900 cursor-pointer hover:text-primary-600"
                          onClick={() => handleVerDetalle(item)}
                          title={`${item.nombreProducto} (Click para ver detalles)`}
                        >
                          {item.nombreProducto}
                        </td>
                        <td className="px-2 py-[7px] truncate text-sm text-gray-600" title={item.nombreCategoria || 'Sin categoría'}>
                          {item.nombreCategoria || 'Sin categoría'}
                        </td>
                        <td className="px-1.5 py-[7px] whitespace-nowrap text-center">
                          <span className="text-sm font-semibold text-gray-900">
                            {item.cantidadDisponible}
                          </span>
                        </td>
                        <td className="px-1.5 py-[7px] whitespace-nowrap text-sm text-gray-600 text-center">
                          {item.stockMinimo}
                        </td>
                        <td className="px-2 py-[7px] whitespace-nowrap text-center">
                          {getStockBadge(item)}
                        </td>
                        <td className="px-2 py-[7px] text-center font-mono leading-tight" title={formatDate(item.fechaActualizacion)}>
                          <span className="text-xs text-gray-800 font-medium block">{formatDateShort(item.fechaActualizacion)}</span>
                          <span className="text-xs text-gray-500 font-medium block">{formatTimeShort(item.fechaActualizacion)}</span>
                        </td>
                        <td className="px-1.5 py-[7px] whitespace-nowrap text-center">
                          <div className="inline-flex items-center justify-center gap-1">
                            {user?.role === 'ADMIN' && (
                              <button
                                onClick={() => handleAjustarStock(item)}
                                className="p-1.5 bg-primary-600 text-white hover:bg-primary-700 rounded-lg transition-colors shadow-sm"
                                title="Ajustar Inventario"
                              >
                                <Edit size={15} />
                              </button>
                            )}
                            <button
                              onClick={() => handleVerHistorialProducto(item)}
                              className="p-1.5 border border-gray-200 bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-lg transition-colors"
                              title="Ver Historial"
                            >
                              <History size={15} />
                            </button>
                            {user?.role === 'ADMIN' && (
                              <button
                                onClick={() => handleConfigurarStockMinimo(item)}
                                className="p-1.5 border border-gray-200 bg-white text-gray-600 hover:text-primary-700 hover:bg-gray-50 rounded-lg transition-colors"
                                title="Configurar stock mínimo"
                              >
                                <Settings size={15} />
                              </button>
                            )}
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

              {/* Paginación */}
              <Paginacion
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={filteredInventario.length}
                itemsPerPage={ITEMS_PER_PAGE}
                onPageChange={setCurrentPage}
                itemName="items"
                startItem={startId}
                endItem={endId}
              />
            </div>
          )}
        </>
      )}

      {/* VISTA 2: MOVIMIENTOS RECIENTES */}
      {mostrarMovimientos && (
        <div>
          {/* Header de navegación y filtros */}
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-4">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setMostrarMovimientos(false)}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-lg text-sm font-semibold transition-colors"
              >
                ← Volver al Inventario
              </button>
              <div>
                <h1 className="text-xl font-bold text-gray-900">Movimientos Recientes</h1>
                <p className="text-gray-500 text-xs">Registro cronológico de operaciones de stock</p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {/* Buscador */}
              <div className="relative w-full sm:w-64">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={18} />
                <input
                  type="text"
                  placeholder="Buscar producto, motivo o usuario..."
                  value={searchMovimiento}
                  onChange={(e) => setSearchMovimiento(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
                />
                {searchMovimiento && (
                  <button
                    onClick={() => setSearchMovimiento('')}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    ✕
                  </button>
                )}
              </div>

              {/* Filtro por Fecha */}
              <select
                value={filtroFecha}
                onChange={(e) => setFiltroFecha(e.target.value as any)}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white text-sm"
              >
                <option value="HOY">Solo Hoy</option>
                <option value="AYER">Solo Ayer</option>
                <option value="FECHA">Elegir fecha</option>
              </select>

              {/* Selector de fecha cuando se elige "Elegir fecha" */}
              {filtroFecha === 'FECHA' && (
                <input
                  type="date"
                  value={fechaElegida}
                  onChange={(e) => setFechaElegida(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white text-sm"
                />
              )}

              {/* Filtro por Operación */}
              <select
                value={filtroOperacion}
                onChange={(e) => setFiltroOperacion(e.target.value as any)}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white text-sm"
              >
                <option value="TODOS">Todas las operaciones</option>
                <option value="COMPRA">Compras</option>
                <option value="VENTA">Ventas</option>
                <option value="CARGA">Nuevos productos / Cargas</option>
              </select>

              {/* Botón Refrescar */}
              <button
                type="button"
                onClick={loadMovimientos}
                disabled={loadingMovimientos}
                className="flex items-center gap-1.5 px-3 py-2 border border-gray-300 bg-white hover:bg-gray-50 text-gray-700 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
                title="Actualizar movimientos"
              >
                <RefreshCw size={15} className={loadingMovimientos ? 'animate-spin text-primary-600' : ''} />
                <span>Actualizar</span>
              </button>
            </div>
          </div>

          {/* Tarjeta de resumen de Hoy */}
          <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-3 mb-5 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-sm">
            <div>
              <span className="font-semibold text-gray-900">Actividad de Hoy: </span>
              {movimientosHoy.length > 0 ? (
                <span className="text-gray-700">
                  Se registraron <strong>{movimientosHoy.length}</strong> movimientos.
                </span>
              ) : (
                <span className="text-gray-500">Sin movimientos registrados hoy todavía. Mostrando operaciones anteriores.</span>
              )}
            </div>
            {ultimoMovHoy && (
              <div className="text-xs text-gray-600 bg-white border border-gray-200 rounded px-2.5 py-1 w-fit">
                <span>Último movimiento hoy: </span>
                <span className="font-mono font-bold text-gray-900">
                  {getInfoFecha(ultimoMovHoy.fecha).hora}
                </span>{' '}
                — <span className="font-medium text-gray-800">{ultimoMovHoy.nombreProducto}</span>
              </div>
            )}
          </div>

          {/* Contador de resultados cuando se busca */}
          {searchMovimiento && (
            <p className="text-sm text-gray-600 mb-3">
              Mostrando <strong>{filteredMovimientos.length}</strong> movimientos coincidentes
            </p>
          )}

          {/* Spinner de carga */}
          {loadingMovimientos && (
            <div className="flex items-center justify-center py-16 bg-white rounded-lg border border-gray-200">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          )}

          {/* Tabla de Movimientos limpia y sobria */}
          {!loadingMovimientos && (
            <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
              <div className="overflow-x-auto" ref={movimientosDrag.scrollContainerRef}>
                <table className="w-full table-fixed min-w-[760px] divide-y divide-gray-200" ref={movimientosDrag.tableRef}>
                  <thead
                    ref={movimientosDrag.theadRef}
                    className={`bg-gray-50 ${movimientosDrag.hasOverflow ? 'cursor-grab select-none' : ''}`}
                    {...movimientosDrag.theadProps}
                  >
                    <tr>
                      <th className="w-[14%] px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                        <span>DÍA Y</span>
                        <span className="block">HORA</span>
                      </th>
                      <th className="w-[28%] px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                        Producto
                      </th>
                      <th className="w-[18%] px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                        Operación
                      </th>
                      <th className="w-[12%] px-2 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                        <span>STOCK</span>
                        <span className="block">ANTERIOR ➔ NUEVO</span>
                      </th>
                      <th className="w-[16%] px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                        Detalle / Motivo
                      </th>
                      <th className="w-[12%] px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                        Responsable
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {paginatedMovimientos.map((mov) => {
                      const infoF = getInfoFecha(mov.fecha);
                      return (
                        <tr key={mov.id} className="hover:bg-gray-50 transition-colors">
                          <td className="px-3 py-[7px] text-center font-mono text-xs whitespace-nowrap leading-tight" title={formatDate(mov.fecha)}>
                            <span className={`font-semibold block ${infoF.esHoy ? 'text-primary-700' : 'text-gray-800'}`}>
                              {infoF.etiqueta}
                            </span>
                            <span className="text-gray-500 block">{infoF.hora}</span>
                          </td>
                          <td className="px-3 py-[7px] truncate" title={mov.nombreProducto}>
                            <div className="text-sm font-medium text-gray-900 truncate">{mov.nombreProducto}</div>
                            <div className="text-xs text-gray-400 font-mono truncate">{mov.skuProducto}</div>
                          </td>
                          <td className="px-3 py-[7px] text-sm whitespace-nowrap">
                            {getOperacionTexto(mov.tipoMovimiento, mov.cantidad)}
                          </td>
                          <td className="px-2 py-[7px] text-center font-mono text-sm whitespace-nowrap">
                            <span className="text-gray-500">{mov.cantidadAnterior}</span>
                            <span className="mx-1.5 text-gray-400">➔</span>
                            <span className="font-bold text-gray-900">{mov.cantidadNueva}</span>
                          </td>
                          <td className="px-3 py-[7px] text-sm text-gray-600 truncate" title={mov.motivo || '—'}>
                            {mov.motivo || '—'}
                          </td>
                          <td className="px-3 py-[7px] text-sm text-gray-700 truncate" title={mov.nombreUsuario || 'Sistema'}>
                            {mov.nombreUsuario || 'Sistema'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>

                {filteredMovimientos.length === 0 && (
                  <div className="text-center py-12">
                    <Clock size={40} className="mx-auto text-gray-400 mb-2" />
                    <p className="text-gray-600 font-medium text-sm">No se encontraron movimientos</p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {searchMovimiento || filtroOperacion !== 'TODOS' || filtroFecha !== 'HOY'
                        ? 'Prueba cambiando la fecha o los filtros seleccionados'
                        : 'No hay movimientos registrados hoy todavía'}
                    </p>
                  </div>
                )}
              </div>

              {/* Paginación */}
              {filteredMovimientos.length > 0 && (
                <Paginacion
                  currentPage={currentMovimientosPage}
                  totalPages={totalMovimientosPages}
                  totalItems={filteredMovimientos.length}
                  itemsPerPage={ITEMS_PER_PAGE_MOVIMIENTOS}
                  onPageChange={setCurrentMovimientosPage}
                  itemName="movimientos"
                  startItem={startMov}
                  endItem={endMov}
                />
              )}
            </div>
          )}
        </div>
      )}

      {/* MODALES */}
      {isAjusteModalOpen && selectedInventario && (
        <MovimientoInventarioModal
          inventario={selectedInventario}
          onClose={() => setIsAjusteModalOpen(false)}
          onSuccess={() => {
            loadData();
            if (movimientosCargados) {
              loadMovimientos();
            }
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
            // loadData evita mostrar el mínimo viejo hasta recargar.
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