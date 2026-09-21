'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  getAllVentas,
  getVentasEstadisticas,
  getVentasDelDia,
  cancelarVenta,
  marcarVentaEntregada,
  getVentaById
} from '@/lib/api';
import { Venta, VentaEstadisticas, EstadoVenta, MetodoPago, EstadoEntrega } from '@/types/venta';
import { exportarCSV, fechaArchivo } from '@/lib/exportar';
import {
  Search,
  Plus,
  Eye,
  XCircle,
  TrendingUp,
  DollarSign,
  ShoppingCart,
  Calendar,
  Filter,
  RefreshCw,
  Download,
  Users,
  Truck,
  Banknote
} from 'lucide-react';
import RegistrarVentaModal from '@/components/RegistrarVentaModal';
import DetalleVentaModal from '@/components/DetalleVentaModal';
import CobrarSaldoModal from '@/components/CobrarSaldoModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import StatCard from '@/components/StatCard';
import { useAuth } from '@/hooks/useAuth';

import Link from 'next/link';

export default function VentasPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [ventas, setVentas] = useState<Venta[]>([]);
  const [ventasFiltradas, setVentasFiltradas] = useState<Venta[]>([]);
  const [estadisticas, setEstadisticas] = useState<VentaEstadisticas | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Estados para modales
  const [showRegistrarModal, setShowRegistrarModal] = useState(false);
  const [showDetalleModal, setShowDetalleModal] = useState(false);
  const [ventaSeleccionada, setVentaSeleccionada] = useState<Venta | null>(null);
  const [showCobrarSaldoModal, setShowCobrarSaldoModal] = useState(false);
  const [ventaACobrar, setVentaACobrar] = useState<Venta | null>(null);
  const [ventaACancelarId, setVentaACancelarId] = useState<number | null>(null);
  const [ventaAEntregarId, setVentaAEntregarId] = useState<number | null>(null);

  // Estados para filtros
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState<EstadoVenta | 'TODOS'>('TODOS');
  const [filtroMetodoPago, setFiltroMetodoPago] = useState<MetodoPago | 'TODOS'>('TODOS');
  const [filtroPeriodo, setFiltroPeriodo] = useState<'HOY' | 'SEMANA' | 'MES' | 'TODOS'>('TODOS');
  const [filtroVendedor, setFiltroVendedor] = useState<string>('TODOS');

  useEffect(() => {
    loadVentas();
    loadEstadisticas();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [ventas, busqueda, filtroEstado, filtroMetodoPago, filtroPeriodo, filtroVendedor]);

  const loadVentas = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getAllVentas();
      setVentas(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar las ventas');
    } finally {
      setLoading(false);
    }
  };

  const loadEstadisticas = async () => {
    try {
      const stats = await getVentasEstadisticas();
      setEstadisticas(stats);
    } catch (err) {
      console.error('Error al cargar estadísticas:', err);
    }
  };

  const aplicarFiltros = () => {
    let resultado = [...ventas];

    // Filtro por búsqueda (nombre de cliente o ID)
    if (busqueda.trim()) {
      const termino = busqueda.toLowerCase();
      resultado = resultado.filter(v =>
        v.nombreCliente.toLowerCase().includes(termino) ||
        v.id.toString().includes(termino) ||
        (v.telefonoCliente && v.telefonoCliente.includes(termino))
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
        case 'SEMANA':
          resultado = filtrarPor(7);
          break;
        case 'MES':
          resultado = filtrarPor(30);
          break;
      }
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
      await loadEstadisticas();
    } catch (err: any) {
      alert(err.message || 'Error al cancelar la venta');
    } finally {
      setVentaACancelarId(null);
    }
  };

  const handleMarcarEntregado = async (id: number) => {
    try {
      await marcarVentaEntregada(id);
      await loadVentas();
    } catch (err: any) {
      alert(err.message || 'Error al marcar la venta como entregada');
    } finally {
      setVentaAEntregarId(null);
    }
  };

  const handleAbrirCobrarSaldo = (venta: Venta) => {
    setVentaACobrar(venta);
    setShowCobrarSaldoModal(true);
  };

  // Antes esto solo mostraba un alert diciendo que el archivo se habia
  // generado, sin generar nada. Ahora arma el CSV con las ventas que estan
  // filtradas en pantalla y el navegador lo descarga.
  const handleExportarExcel = () => {
    if (ventasFiltradas.length === 0) {
      alert('No hay ventas para exportar con los filtros actuales.');
      return;
    }

    exportarCSV<Venta>(
      `Ventas_${fechaArchivo()}`,
      [
        { encabezado: 'N. Venta', valor: (v) => v.id },
        { encabezado: 'Fecha', valor: (v) => new Date(v.fechaVenta).toLocaleString('es-BO') },
        { encabezado: 'Cliente', valor: (v) => v.nombreCliente },
        { encabezado: 'Telefono', valor: (v) => v.telefonoCliente ?? '' },
        { encabezado: 'Cliente registrado', valor: (v) => (v.esClienteRegistrado ? 'Si' : 'No') },
        { encabezado: 'Vendedor', valor: (v) => v.nombreUsuario },
        { encabezado: 'Subtotal (Bs)', valor: (v) => Number(v.subtotal).toFixed(2) },
        { encabezado: 'Descuento (Bs)', valor: (v) => Number(v.descuento ?? 0).toFixed(2) },
        { encabezado: 'Total (Bs)', valor: (v) => Number(v.montoTotal).toFixed(2) },
        { encabezado: 'Saldo pendiente (Bs)', valor: (v) => Number(v.saldoPendiente ?? 0).toFixed(2) },
        { encabezado: 'Metodo de pago', valor: (v) => v.metodoPago ?? 'Sin pago' },
        { encabezado: 'Estado', valor: (v) => v.estado },
        { encabezado: 'Requiere envio', valor: (v) => (v.requiereEnvio ? 'Si' : 'No') },
        { encabezado: 'Productos', valor: (v) => v.detalles.map((d) => `${d.cantidad}x ${d.nombreProducto}`).join(' | ') },
      ],
      ventasFiltradas
    );
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

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Cargando ventas...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
              <ShoppingCart className="text-primary-600" size={36} />
              Gestión de Ventas
            </h1>
            <p className="text-gray-600 mt-2">Registro y seguimiento de ventas realizadas</p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/clientes"
              className="flex items-center gap-2 px-5 py-2.5 border border-gray-300 bg-white text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium text-sm shadow-sm"
            >
              <Users size={18} />
              Gestionar Clientes
            </Link>
            <button
              onClick={() => setShowRegistrarModal(true)}
              className="flex items-center gap-2 px-5 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium text-sm shadow-sm"
            >
              <Plus size={18} />
              Registrar Venta
            </button>
          </div>
        </div>

        {/* Estadísticas */}
        {estadisticas && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <StatCard
              titulo="Ventas del Día"
              valor={estadisticas.ventasDelDia}
              subtitulo={`Bs. ${estadisticas.montoDelDia.toFixed(2)}`}
              icon={<Calendar size={22} />}
            />
            <StatCard
              titulo="Completadas"
              valor={estadisticas.ventasCompletadas}
              subtitulo="Total ventas"
              icon={<TrendingUp size={22} />}
            />
            <StatCard
              titulo="Pendientes"
              valor={estadisticas.ventasPendientes}
              subtitulo="Por cobrar"
              icon={<ShoppingCart size={22} />}
            />
            <StatCard
              titulo="Monto Total"
              valor={`Bs. ${estadisticas.montoTotal.toFixed(2)}`}
              subtitulo="Ingresos totales"
              icon={<DollarSign size={22} />}
            />
          </div>
        )}

        {/* Filtros */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <Filter size={20} className="text-gray-600" />
            <h3 className="font-semibold text-gray-900">Filtros</h3>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            {/* Búsqueda */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={18} />
              <input
                type="text"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                placeholder="Buscar por cliente, ID o celular..."
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>

            {/* Filtro por período */}
            <select
              value={filtroPeriodo}
              onChange={(e) => setFiltroPeriodo(e.target.value as any)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="TODOS">Todos los períodos</option>
              <option value="HOY">Hoy</option>
              <option value="SEMANA">Última semana</option>
              <option value="MES">Último mes</option>
            </select>

            {/* Filtro por método de pago */}
            <select
              value={filtroMetodoPago}
              onChange={(e) => setFiltroMetodoPago(e.target.value as any)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="TODOS">Todos los métodos</option>
              {Object.values(MetodoPago).map(metodo => (
                <option key={metodo} value={metodo}>{metodo}</option>
              ))}
            </select>

            {/* Filtro por vendedor */}
            <select
              value={filtroVendedor}
              onChange={(e) => setFiltroVendedor(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="TODOS">Todos los vendedores</option>
              {obtenerVendedores().map(vendedor => (
                <option key={vendedor} value={vendedor}>{vendedor}</option>
              ))}
            </select>

            {/* Filtro por estado */}
            <select
              value={filtroEstado}
              onChange={(e) => setFiltroEstado(e.target.value as any)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="TODOS">Todos los estados</option>
              {Object.values(EstadoVenta).map(estado => (
                <option key={estado} value={estado}>{estado.replace('_', ' ')}</option>
              ))}
            </select>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm text-gray-600">
              Mostrando <span className="font-semibold">{ventasFiltradas.length}</span> de{' '}
              <span className="font-semibold">{ventas.length}</span> ventas
            </p>
            <div className="flex gap-3">
              <button
                onClick={handleExportarExcel}
                className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white hover:bg-green-700 rounded-lg transition-colors shadow-md"
              >
                <Download size={16} />
                Exportar a CSV
              </button>
              <button
                onClick={loadVentas}
                className="flex items-center gap-2 px-4 py-2 text-primary-600 hover:bg-primary-50 rounded-lg transition-colors font-medium"
              >
                <RefreshCw size={16} />
                Actualizar
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {/* Tabla de ventas */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  # Venta
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Cliente
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Fecha y Hora
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Productos
                </th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Total (Bs.)
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Método Pago
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Vendedor
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Estado
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Entrega
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {ventasFiltradas.length === 0 ? (
                <tr>
                  <td colSpan={10} className="px-6 py-12 text-center">
                    <ShoppingCart className="mx-auto text-gray-400 mb-3" size={48} />
                    <p className="text-gray-500 font-medium">No se encontraron ventas</p>
                    <p className="text-sm text-gray-400 mt-1">
                      {busqueda || filtroEstado !== 'TODOS' || filtroMetodoPago !== 'TODOS' || filtroPeriodo !== 'TODOS' || filtroVendedor !== 'TODOS'
                        ? 'Intenta ajustar los filtros de búsqueda'
                        : 'Registra tu primera venta para comenzar'}
                    </p>
                  </td>
                </tr>
              ) : (
                ventasFiltradas.map((venta) => (
                  <tr key={venta.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm font-bold text-primary-700">#{venta.id}</span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <div>
                          <div className="text-sm font-medium text-gray-900 flex items-center gap-2">
                            {venta.nombreCliente}
                            {!venta.esClienteRegistrado && (
                              <span className="px-2 py-0.5 text-xs bg-primary-50 text-primary-700 border border-primary-200 rounded-full font-medium">
                                Rápido
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-gray-500">
                            {venta.telefonoCliente || 'Sin teléfono'}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatFecha(venta.fechaVenta)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 max-w-xs">
                      <div className="truncate" title={obtenerResumenProductos(venta)}>
                        {obtenerResumenProductos(venta)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <span className="text-lg font-semibold text-gray-900">
                        Bs. {venta.montoTotal.toFixed(2)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <div className="flex flex-col items-center gap-1">
                        <span className="px-3 py-1 text-xs font-medium bg-gray-100 text-gray-800 rounded-full">
                          {venta.metodoPago}
                        </span>
                        {venta.tienePagosSinRespaldo && (
                          <span className="px-2 py-0.5 text-xs bg-orange-100 text-orange-700 rounded-full">
                            Sin respaldo
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{venta.nombreUsuario}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full border ${getEstadoBadgeColor(venta.estado)}`}>
                        {venta.estado.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full border ${
                        venta.estadoEntrega === EstadoEntrega.ENTREGADO
                          ? 'bg-green-100 text-green-800 border-green-200'
                          : 'bg-gray-100 text-gray-800 border-gray-200'
                      }`}>
                        {venta.estadoEntrega === EstadoEntrega.ENTREGADO ? 'Entregado' : 'Pendiente'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleVerDetalle(venta)}
                          className="p-2 text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                          title="Ver detalle"
                        >
                          <Eye size={18} />
                        </button>
                        {venta.estado !== EstadoVenta.CANCELADA && venta.estadoEntrega === EstadoEntrega.PENDIENTE && (
                          <button
                            onClick={() => setVentaAEntregarId(venta.id)}
                            className="p-2 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                            title="Marcar entregado"
                          >
                            <Truck size={18} />
                          </button>
                        )}
                        {venta.estado === EstadoVenta.PENDIENTE_PAGO && (venta.saldoPendiente ?? 0) > 0 && (
                          <button
                            onClick={() => handleAbrirCobrarSaldo(venta)}
                            className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors"
                            title="Cobrar saldo pendiente"
                          >
                            <Banknote size={18} />
                          </button>
                        )}
                        {user?.role === 'ADMIN' && venta.estado === EstadoVenta.COMPLETADA && (
                          <button
                            onClick={() => setVentaACancelarId(venta.id)}
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Cancelar venta"
                          >
                            <XCircle size={18} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modales */}
      <RegistrarVentaModal
        isOpen={showRegistrarModal}
        onClose={() => setShowRegistrarModal(false)}
        onSuccess={() => {
          loadVentas();
          loadEstadisticas();
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
            loadEstadisticas();
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
        <DeleteConfirmModal
          title="Marcar como entregada"
          message="¿Confirmar que esta venta ya fue entregada?"
          confirmLabel="Marcar entregado"
          onConfirm={() => handleMarcarEntregado(ventaAEntregarId)}
          onCancel={() => setVentaAEntregarId(null)}
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