'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';
import {
  getAllVentas,
  getVentasDelDia,
  cancelarVenta,
  marcarVentaEntregada,
  getVentaById
} from '@/lib/api';
import { Venta, EstadoVenta, MetodoPago, EstadoEntrega, ModalidadEntrega } from '@/types/venta';
import {
  Search,
  Plus,
  Eye,
  XCircle,
  DollarSign,
  ShoppingCart,
  Truck,
  Banknote,
  X
} from 'lucide-react';
import RegistrarVentaModal from '@/components/RegistrarVentaModal';
import DetalleVentaModal from '@/components/DetalleVentaModal';
import CobrarSaldoModal from '@/components/CobrarSaldoModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import Breadcrumbs from '@/components/Breadcrumbs';
import { mensajeError } from '@/lib/errores';
import { CORREGIR_ENTREGA_ACTIVO, claseBadgeEstadoEntrega, esPorEntregar, etiquetaEstadoEntrega, etiquetaModalidadCorta, faltaCompletarEnvio } from '@/lib/entrega';
import DeshacerEntregaModal from '@/components/DeshacerEntregaModal';
import { useAuth } from '@/hooks/useAuth';


export default function VentasPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [ventas, setVentas] = useState<Venta[]>([]);
  const [ventasFiltradas, setVentasFiltradas] = useState<Venta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Reemplaza los alert() nativos del navegador (mostraban "localhost dice"
  // en vez de un mensaje con la identidad del sistema) por el mismo cartel
  // con estilo que ya usan Usuarios y Productos.
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    if (type === 'success') {
      setTimeout(() => setMessage(null), 4000);
    }
  };

  // Estados para modales
  const [showRegistrarModal, setShowRegistrarModal] = useState(false);
  const [showDetalleModal, setShowDetalleModal] = useState(false);
  const [ventaSeleccionada, setVentaSeleccionada] = useState<Venta | null>(null);
  const [showCobrarSaldoModal, setShowCobrarSaldoModal] = useState(false);
  const [ventaACobrar, setVentaACobrar] = useState<Venta | null>(null);
  const [ventaACancelarId, setVentaACancelarId] = useState<number | null>(null);
  const [ventaAEntregarId, setVentaAEntregarId] = useState<number | null>(null);
  // Venta cuya entrega se corrige a Pendiente (clic en la etiqueta, solo ADMIN).
  const [ventaACorregir, setVentaACorregir] = useState<Venta | null>(null);

  // Estados para filtros
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState<EstadoVenta | 'TODOS'>('TODOS');
  const [filtroMetodoPago, setFiltroMetodoPago] = useState<MetodoPago | 'TODOS'>('TODOS');
  const [filtroPeriodo, setFiltroPeriodo] = useState<'HOY' | 'SEMANA' | 'MES' | 'TODOS'>('TODOS');
  const [filtroVendedor, setFiltroVendedor] = useState<string>('TODOS');
  const [filtroEntrega, setFiltroEntrega] = useState<'TODOS' | 'POR_ENTREGAR' | 'PENDIENTE' | 'ENTREGADO'>('TODOS');

  useEffect(() => {
    loadVentas();
  }, []);

  // Clickear "Ventas" en el menú, aunque ya estés en esta pantalla, tiene
  // que volver todo a su estado normal — si no, una vez que tocás una
  // tarjeta no hay forma de volver sin tocar cada filtro a mano. El link
  // del Sidebar dispara este evento (ver Sidebar.tsx) porque Next no
  // re-renderiza solo por navegar a la misma ruta en la que ya estás.
  useEffect(() => {
    const resetearFiltros = () => {
      setBusqueda('');
      setFiltroEstado('TODOS');
      setFiltroMetodoPago('TODOS');
      setFiltroPeriodo('TODOS');
      setFiltroVendedor('TODOS');
      setFiltroEntrega('TODOS');
    };
    window.addEventListener('ventas:reset-filtros', resetearFiltros);
    return () => window.removeEventListener('ventas:reset-filtros', resetearFiltros);
  }, []);

  // El "Período" que llega desde una tarjeta de Inicio (ej. "Ventas del
  // mes") queda ya filtrado acá, sin tener que volver a elegirlo a mano.
  useEffect(() => {
    const periodo = new URLSearchParams(window.location.search).get('periodo');
    if (periodo === 'HOY' || periodo === 'SEMANA' || periodo === 'MES') {
      setFiltroPeriodo(periodo);
    }
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ventas, busqueda, filtroEstado, filtroMetodoPago, filtroPeriodo, filtroVendedor, filtroEntrega]);

  const loadVentas = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getAllVentas();
      setVentas(data);
    } catch (err: any) {
      setError(mensajeError(err, 'No se pudieron cargar las ventas.'));
    } finally {
      setLoading(false);
    }
  };

  const aplicarFiltros = () => {
    let resultado = [...ventas];

    // Filtro por búsqueda (cliente, ID, celular o producto). Sin tildes:
    // nadie escribe tildes buscando rápido, y antes "colchon" no encontraba
    // "Colchón".
    if (busqueda.trim()) {
      const normalizar = (s: string) =>
        s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
      const termino = normalizar(busqueda.trim());
      resultado = resultado.filter(v =>
        normalizar(v.nombreCliente).includes(termino) ||
        v.id.toString().includes(termino) ||
        (v.telefonoCliente && v.telefonoCliente.includes(termino)) ||
        v.detalles.some(d => normalizar(d.nombreProducto).includes(termino))
      );
    }

    // Filtro por estado
    if (filtroEstado !== 'TODOS') {
      resultado = resultado.filter(v => v.estado === filtroEstado);
    }

    // Filtro por método de pago
    if (filtroMetodoPago !== 'TODOS') {
      resultado = resultado.filter(v => v.metodoPago === filtroMetodoPago);
    }

    // Filtro por vendedor
    if (filtroVendedor !== 'TODOS') {
      resultado = resultado.filter(v => v.nombreUsuario === filtroVendedor);
    }

    // Filtro por período
    if (filtroPeriodo !== 'TODOS') {
      const ahora = new Date();
      const filtrarPor = (dias: number) => {
        const fecha = new Date(ahora);
        fecha.setDate(fecha.getDate() - dias);
        return resultado.filter(v => new Date(v.fechaVenta) >= fecha);
      };

      switch (filtroPeriodo) {
        case 'HOY':
          const hoy = new Date();
          hoy.setHours(0, 0, 0, 0);
          resultado = resultado.filter(v => new Date(v.fechaVenta) >= hoy);
          break;
        case 'SEMANA': {
          // Semana calendario, de lunes a hoy: la misma que cuenta la tarjeta
          // "Ventas de la semana" del Inicio.
          const lunes = new Date();
          lunes.setHours(0, 0, 0, 0);
          lunes.setDate(lunes.getDate() - ((lunes.getDay() + 6) % 7));
          resultado = resultado.filter(v => new Date(v.fechaVenta) >= lunes);
          break;
        }
        case 'MES':
          resultado = filtrarPor(30);
          break;
      }
    }

    // Filtro por entrega. "Por entregar" es la definición única del sistema
    // (estado distinto de ENTREGADO y venta no cancelada); los otros tres
    // filtran por un estado puntual.
    if (filtroEntrega === 'POR_ENTREGAR') {
      resultado = resultado.filter(esPorEntregar);
    } else if (filtroEntrega === 'PENDIENTE') {
      resultado = resultado.filter(v => v.estadoEntrega === EstadoEntrega.PENDIENTE);
    } else if (filtroEntrega === 'ENTREGADO') {
      resultado = resultado.filter(v => v.estadoEntrega === EstadoEntrega.ENTREGADO);
    }

    setVentasFiltradas(resultado);
  };

  const handleVerDetalle = (venta: Venta) => {
    setVentaSeleccionada(venta);
    setShowDetalleModal(true);
  };

  const handleCancelarVenta = async (id: number) => {
    try {
      await cancelarVenta(id);
      await loadVentas();
      showMessage('success', 'Venta cancelada correctamente.');
    } catch (err: any) {
      showMessage('error', mensajeError(err, 'No se pudo cancelar la venta.'));
    } finally {
      setVentaACancelarId(null);
    }
  };

  const handleMarcarEntregado = async (id: number) => {
    try {
      await marcarVentaEntregada(id);
      await loadVentas();
      showMessage('success', 'Venta marcada como entregada.');
    } catch (err: any) {
      showMessage('error', mensajeError(err, 'No se pudo marcar la venta como entregada.'));
    } finally {
      setVentaAEntregarId(null);
    }
  };

  const handleAbrirCobrarSaldo = (venta: Venta) => {
    setVentaACobrar(venta);
    setShowCobrarSaldoModal(true);
  };

  const obtenerVendedores = (): string[] => {
    const vendedoresUnicos = Array.from(new Set(ventas.map(v => v.nombreUsuario)));
    return vendedoresUnicos.sort();
  };

  const obtenerResumenProductos = (venta: Venta): string => {
    if (venta.detalles.length === 0) return 'Sin productos';
    if (venta.detalles.length === 1) {
      const detalle = venta.detalles[0];
      return `${detalle.nombreProducto} (x${detalle.cantidad})`;
    }
    const primerProducto = venta.detalles[0];
    const totalItems = venta.detalles.reduce((sum, d) => sum + d.cantidad, 0);
    return `${primerProducto.nombreProducto} (x${primerProducto.cantidad}) +${venta.detalles.length - 1} más (${totalItems} items)`;
  };

  const getEstadoBadgeColor = (estado: EstadoVenta) => {
    switch (estado) {
      case EstadoVenta.COMPLETADA:
        return 'bg-green-100 text-green-800 border-green-200';
      case EstadoVenta.PENDIENTE_PAGO:
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case EstadoVenta.CANCELADA:
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatFecha = (fecha: string) => {
    return new Date(fecha).toLocaleString('es-BO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  // Arrastrar la tabla desde el encabezado, como si fuera una barra de
  // scroll horizontal. Ver hooks/useDragScrollTable.ts.
  const {
    scrollContainerRef,
    tableRef,
    theadRef,
    hasOverflow,
    theadProps,
  } = useDragScrollTable([loading, ventasFiltradas]);

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Ventas' }]} />

      {/* Header con filtros */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3 mb-4">
        <div className="flex-shrink-0">
          <h1 className="text-xl lg:text-2xl font-bold text-gray-900">Gestión de Ventas</h1>
          <p className="text-xs lg:text-sm text-gray-600">Registro y seguimiento de ventas realizadas</p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <div className="relative w-full sm:w-56">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={17} />
            <input
              type="text"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar por cliente, ID, celular o producto..."
              className="w-full pl-9 pr-3 py-1.5 text-xs lg:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <select
            value={filtroPeriodo}
            onChange={(e) => setFiltroPeriodo(e.target.value as any)}
            className="px-3 py-1.5 text-xs lg:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los períodos</option>
            <option value="HOY">Hoy</option>
            <option value="SEMANA">Esta semana</option>
            <option value="MES">Último mes</option>
          </select>

          <select
            value={filtroMetodoPago}
            onChange={(e) => setFiltroMetodoPago(e.target.value as any)}
            className="px-3 py-1.5 text-xs lg:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los métodos</option>
            {Object.values(MetodoPago).map(metodo => (
              <option key={metodo} value={metodo}>{metodo}</option>
            ))}
          </select>

          <select
            value={filtroVendedor}
            onChange={(e) => setFiltroVendedor(e.target.value)}
            className="px-3 py-1.5 text-xs lg:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los vendedores</option>
            {obtenerVendedores().map(vendedor => (
              <option key={vendedor} value={vendedor}>{vendedor}</option>
            ))}
          </select>

          <select
            value={filtroEstado}
            onChange={(e) => setFiltroEstado(e.target.value as any)}
            className="px-3 py-1.5 text-xs lg:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los estados</option>
            {Object.values(EstadoVenta).map(estado => (
              <option key={estado} value={estado}>{estado.replace('_', ' ')}</option>
            ))}
          </select>

          <select
            value={filtroEntrega}
            onChange={(e) => setFiltroEntrega(e.target.value as any)}
            className="px-3 py-1.5 text-xs lg:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Toda entrega</option>
            <option value="POR_ENTREGAR">Por entregar</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="ENTREGADO">Entregado</option>
          </select>

          <button
            onClick={() => setShowRegistrarModal(true)}
            className="flex items-center justify-center gap-1.5 bg-primary-600 text-white px-4 py-1.5 rounded-lg hover:bg-primary-700 transition-colors text-xs lg:text-sm font-medium whitespace-nowrap"
          >
            <Plus size={17} />
            Registrar Venta
          </button>
        </div>
      </div>

      {/* Contador */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <p className="text-sm text-gray-600">
          Mostrando <span className="font-semibold">{ventasFiltradas.length}</span> de{' '}
          <span className="font-semibold">{ventas.length}</span> ventas
        </p>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start justify-between gap-3">
          <p className="text-red-800">{error}</p>
          <button onClick={() => setError(null)} className="flex-shrink-0 text-red-700 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {message && (
        <div className={`mb-6 p-4 rounded-lg flex items-start justify-between gap-3 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="flex-shrink-0 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Tabla de ventas */}
      {loading ? (
        <div className="bg-white rounded-lg shadow-md flex items-center justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      ) : (() => {
        // El casillero de una acción se reserva en TODA la tabla, no fila
        // por fila: si ninguna venta visible la necesita, no ocupa espacio
        // en ninguna; en cuanto una la necesita, vuelve a reservarse en
        // todas para mantener la columna alineada.
        const hayEntregaPendiente = ventasFiltradas.some(esPorEntregar);
        const haySaldoPendiente = ventasFiltradas.some(v =>
          v.estado === EstadoVenta.PENDIENTE_PAGO && (v.saldoPendiente ?? 0) > 0
        );
        return (
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="overflow-x-auto" ref={scrollContainerRef}>
          <table className="w-full" ref={tableRef}>
            <thead
              ref={theadRef}
              className={`bg-gray-50 border-b border-gray-200 ${hasOverflow ? 'cursor-grab select-none' : ''}`}
              {...theadProps}
            >
              <tr>
                <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  ID
                </th>
                <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Cliente
                </th>
                <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Fecha y Hora
                </th>
                <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Productos
                </th>
                <th className="px-3 py-2.5 text-right text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Total (Bs.)
                </th>
                <th className="px-3 py-2.5 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Estado
                </th>
                <th className="px-3 py-2.5 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Entrega
                </th>
                <th className="px-3 py-2.5 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {ventasFiltradas.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center">
                    <ShoppingCart className="mx-auto text-gray-400 mb-3" size={48} />
                    <p className="text-gray-500 font-medium">No se encontraron ventas</p>
                    <p className="text-sm text-gray-400 mt-1">
                      {busqueda || filtroEstado !== 'TODOS' || filtroMetodoPago !== 'TODOS' || filtroPeriodo !== 'TODOS' || filtroVendedor !== 'TODOS' || filtroEntrega !== 'TODOS'
                        ? 'Intenta ajustar los filtros de búsqueda'
                        : 'Registra tu primera venta para comenzar'}
                    </p>
                  </td>
                </tr>
              ) : (
                ventasFiltradas.map((venta) => (
                  <tr key={venta.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <span className="text-sm font-bold text-primary-700">{venta.id}</span>
                    </td>
                    <td className="px-3 py-2.5 whitespace-nowrap max-w-[9.5rem]">
                      <div className="text-sm font-medium text-gray-900 truncate" title={venta.nombreCliente}>
                        {venta.nombreCliente}
                      </div>
                    </td>
                    <td className="px-3 py-2.5 whitespace-nowrap text-sm text-gray-900">
                      {formatFecha(venta.fechaVenta)}
                    </td>
                    <td className="px-3 py-2.5 text-sm text-gray-600 max-w-[12.5rem]">
                      <div className="truncate" title={obtenerResumenProductos(venta)}>
                        {obtenerResumenProductos(venta)}
                      </div>
                    </td>
                    <td className="px-3 py-2.5 whitespace-nowrap text-right">
                      <span className="text-sm font-bold text-gray-900">
                        Bs. {venta.montoTotal.toFixed(2)}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 whitespace-nowrap text-center">
                      <span className={`px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full border ${getEstadoBadgeColor(venta.estado)}`}>
                        {venta.estado.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 whitespace-nowrap text-center">
                      {venta.modalidadEntrega === ModalidadEntrega.RETIRO ? (
                        <span className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full border bg-blue-50 text-blue-700 border-blue-200">
                          En tienda
                        </span>
                      ) : (
                        <div className="flex flex-col items-center gap-0.5">
                          <span className="text-[11px] leading-4 text-gray-500">
                            {etiquetaModalidadCorta(venta.modalidadEntrega)}
                          </span>
                          {CORREGIR_ENTREGA_ACTIVO
                            && user?.role === 'ADMIN'
                            && venta.estadoEntrega === EstadoEntrega.ENTREGADO
                            && venta.estado !== EstadoVenta.CANCELADA ? (
                            <button
                              type="button"
                              onClick={() => setVentaACorregir(venta)}
                              title="Corregir a Pendiente"
                              className={`px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full border cursor-pointer hover:opacity-80 transition-opacity ${claseBadgeEstadoEntrega(venta.estadoEntrega)}`}
                            >
                              {etiquetaEstadoEntrega(venta.estadoEntrega)}
                            </button>
                          ) : (
                            <span className={`px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full border ${claseBadgeEstadoEntrega(venta.estadoEntrega)}`}>
                              {etiquetaEstadoEntrega(venta.estadoEntrega)}
                            </span>
                          )}
                          {faltaCompletarEnvio(venta) && (
                            <span className="px-2 py-0.5 inline-flex text-[11px] leading-4 font-semibold rounded-full border bg-orange-50 text-orange-700 border-orange-200">
                              Falta completar
                            </span>
                          )}
                        </div>
                      )}
                    </td>
                    <td className="px-3 py-2.5 whitespace-nowrap text-center">
                      {/* 4 casilleros fijos, siempre en el mismo orden. Lo
                          que no aplica a esta venta queda invisible (no
                          "hidden"): sigue ocupando su lugar, así el ícono
                          de más a la derecha cae en la misma columna
                          vertical en todas las filas, tenga 2, 3 o 4
                          acciones activas. Estilo inline para la visibilidad
                          porque la utilidad "invisible" de Tailwind no está
                          generada en este proyecto (se probó y no aplicaba). */}
                      {(() => {
                        // El ícono de entregar se ofrece en toda venta por entregar
                        // (pendiente o despachada, no cancelada). El saldo
                        // pendiente no la bloquea: el modal de confirmación solo
                        // lo avisa. Corregir a pendiente y editar los datos de
                        // entrega están en el detalle de la venta.
                        const puedeEntregar = esPorEntregar(venta);
                        const puedeCobrarSaldo = venta.estado === EstadoVenta.PENDIENTE_PAGO
                          && (venta.saldoPendiente ?? 0) > 0;
                        // Antes solo se ofrecía para Completada: el backend
                        // ya permite cancelar una venta con saldo pendiente
                        // (por ejemplo, una seña que nunca se terminó de
                        // pagar), pero el botón nunca aparecía para ese
                        // caso. Ahora se ofrece para cualquier venta que no
                        // esté cancelada ya.
                        const puedeCancelar = user?.role === 'ADMIN' && venta.estado !== EstadoVenta.CANCELADA;

                        return (
                          <div className="flex items-center justify-center gap-0">
                            <button
                              onClick={() => handleVerDetalle(venta)}
                              className="p-1 text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                              title="Ver detalle"
                            >
                              <Eye size={18} />
                            </button>

                            {hayEntregaPendiente && (
                              <button
                                onClick={() => setVentaAEntregarId(venta.id)}
                                disabled={!puedeEntregar}
                                style={{ visibility: puedeEntregar ? 'visible' : 'hidden' }}
                                className="p-1 text-green-600 hover:bg-green-50 rounded-lg transition-colors pointer-events-auto disabled:pointer-events-none"
                                title="Marcar entregado"
                              >
                                <Truck size={18} />
                              </button>
                            )}

                            {haySaldoPendiente && (
                              <button
                                onClick={() => handleAbrirCobrarSaldo(venta)}
                                disabled={!puedeCobrarSaldo}
                                style={{ visibility: puedeCobrarSaldo ? 'visible' : 'hidden' }}
                                className="p-1 text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors pointer-events-auto disabled:pointer-events-none"
                                title="Cobrar saldo pendiente"
                              >
                                <Banknote size={18} />
                              </button>
                            )}

                            <button
                              onClick={() => setVentaACancelarId(venta.id)}
                              disabled={!puedeCancelar}
                              style={{ visibility: puedeCancelar ? 'visible' : 'hidden' }}
                              className="p-1 text-red-600 hover:bg-red-50 rounded-lg transition-colors pointer-events-auto disabled:pointer-events-none"
                              title="Cancelar venta"
                            >
                              <XCircle size={18} />
                            </button>
                          </div>
                        );
                      })()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
        );
      })()}

      {/* Modales */}
      <RegistrarVentaModal
        isOpen={showRegistrarModal}
        onClose={() => setShowRegistrarModal(false)}
        onSuccess={() => {
          loadVentas();
        }}
        userRole={user?.role === 'ADMIN' ? 'ADMIN' : 'EMPLEADO'}
      />

      {ventaSeleccionada && (
        <DetalleVentaModal
          isOpen={showDetalleModal}
          onClose={() => {
            setShowDetalleModal(false);
            setVentaSeleccionada(null);
          }}
          venta={ventaSeleccionada}
          userRole={user?.role === 'ADMIN' ? 'ADMIN' : 'EMPLEADO'}
          onVentaActualizada={(actualizada) => {
            setVentaSeleccionada(actualizada);
            loadVentas();
            }}
          onUpdated={loadVentas}
          onCobrarSaldo={() => {
            setVentaACobrar(ventaSeleccionada);
            setShowCobrarSaldoModal(true);
          }}
        />
      )}

      {showCobrarSaldoModal && ventaACobrar && (
        <CobrarSaldoModal
          venta={ventaACobrar}
          onClose={() => {
            setShowCobrarSaldoModal(false);
            setVentaACobrar(null);
          }}
          onSuccess={async () => {
            loadVentas();
              // Si la venta que se acaba de cobrar es la que está abierta en
            // el detalle, se refresca también ahí (si no, queda mostrando
            // el saldo y los pagos viejos hasta cerrar y volver a abrir).
            if (ventaSeleccionada && ventaACobrar && ventaSeleccionada.id === ventaACobrar.id) {
              try {
                const actualizada = await getVentaById(ventaACobrar.id);
                setVentaSeleccionada(actualizada);
              } catch (err) {
                console.error('Error al refrescar la venta:', err);
              }
            }
          }}
        />
      )}

      {ventaAEntregarId !== null && (
        // El saldo pendiente no bloquea ni avisa: es una confirmación simple.
        <DeleteConfirmModal
          title="Marcar como entregada"
          message="¿Confirmar que esta venta ya fue entregada?"
          confirmLabel="Marcar entregado"
          onConfirm={() => handleMarcarEntregado(ventaAEntregarId)}
          onCancel={() => setVentaAEntregarId(null)}
        />
      )}

      {ventaACorregir && (
        <DeshacerEntregaModal
          venta={ventaACorregir}
          onClose={() => setVentaACorregir(null)}
          onHecho={() => loadVentas()}
        />
      )}

      {ventaACancelarId !== null && (
        <DeleteConfirmModal
          title="Cancelar venta"
          message="¿Está seguro de cancelar esta venta? Esta acción restaurará el stock de los productos."
          confirmLabel="Cancelar venta"
          onConfirm={() => handleCancelarVenta(ventaACancelarId)}
          onCancel={() => setVentaACancelarId(null)}
        />
      )}
    </div>
  );
}