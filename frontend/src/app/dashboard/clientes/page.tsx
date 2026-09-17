'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  getClientesConEstadisticas,
  getEstadisticasGeneralesClientes,
  searchClientes,
} from '@/lib/api';
import { ClienteConEstadisticas, ClienteEstadisticas } from '@/types/cliente';
import { exportarCSV, fechaArchivo } from '@/lib/exportar';
import {
  Search,
  ArrowLeft,
  Users,
  TrendingUp,
  ShoppingBag,
  Crown,
  Edit2,
  History,
  Filter,
  RefreshCw,
  Download,
  X,
} from 'lucide-react';
import ModificarClienteModal from '@/components/ModificarClienteModal';
import HistorialComprasModal from '@/components/HistorialComprasModal';
import { useAuth } from '@/contexts/AuthContext';

import Link from 'next/link';

export default function ClientesPage() {
  const router = useRouter();
  const { user } = useAuth();

  const [clientes, setClientes] = useState<ClienteConEstadisticas[]>([]);
  const [clientesFiltrados, setClientesFiltrados] = useState<ClienteConEstadisticas[]>([]);
  const [estadisticas, setEstadisticas] = useState<ClienteEstadisticas | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Estados para modales
  const [showModificarModal, setShowModificarModal] = useState(false);
  const [showHistorialModal, setShowHistorialModal] = useState(false);
  const [clienteSeleccionado, setClienteSeleccionado] = useState<ClienteConEstadisticas | null>(
    null
  );

  // Estados para filtros
  const [busqueda, setBusqueda] = useState('');

  // El buscador de la cabecera navega hasta aca con ?q=<texto>. Se copia al
  // filtro de la pantalla para que el elemento que se eligio alla quede a la
  // vista sin tener que volver a escribirlo.
  //
  // Se lee de window y no con useSearchParams a proposito: ese hook obliga a
  // envolver la pagina en un <Suspense> y, sin eso, el build de produccion
  // falla. Aca el valor solo hace falta despues de montar, asi que alcanza con
  // mirar la URL dentro del efecto.
  useEffect(() => {
    const q = new URLSearchParams(window.location.search).get('q');
    if (q) {
      setBusqueda(q);
    }
  }, []);
  const [filtroFechaDesde, setFiltroFechaDesde] = useState('');
  const [filtroFechaHasta, setFiltroFechaHasta] = useState('');
  const [filtroMontoMin, setFiltroMontoMin] = useState('');
  const [filtroMontoMax, setFiltroMontoMax] = useState('');
  const [ordenColumna, setOrdenColumna] = useState<
    'nombre' | 'numeroCompras' | 'montoTotal' | 'ultimaCompra'
  >('nombre');
  const [ordenDireccion, setOrdenDireccion] = useState<'asc' | 'desc'>('asc');

  useEffect(() => {
    loadClientes();
    loadEstadisticas();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [
    clientes,
    busqueda,
    filtroFechaDesde,
    filtroFechaHasta,
    filtroMontoMin,
    filtroMontoMax,
    ordenColumna,
    ordenDireccion,
  ]);

  const loadClientes = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getClientesConEstadisticas();
      setClientes(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar clientes');
    } finally {
      setLoading(false);
    }
  };

  const loadEstadisticas = async () => {
    try {
      const stats = await getEstadisticasGeneralesClientes();
      setEstadisticas(stats);
    } catch (err) {
      console.error('Error al cargar estadísticas:', err);
    }
  };

  const aplicarFiltros = () => {
    let resultado = [...clientes];

    // Filtro por búsqueda
    if (busqueda.trim()) {
      const termino = busqueda.toLowerCase();
      resultado = resultado.filter(
        (c) =>
          c.nombreCompleto.toLowerCase().includes(termino) ||
          (c.telefono ?? "").includes(termino) ||
          (c.nitCi && c.nitCi.includes(termino))
      );
    }

    // Filtro por rango de fechas
    if (filtroFechaDesde && filtroFechaHasta) {
      const desde = new Date(filtroFechaDesde);
      const hasta = new Date(filtroFechaHasta);

      if (desde > hasta) {
        setError('La fecha inicial no puede ser posterior a la fecha final');
        return;
      }

      resultado = resultado.filter((c) => {
        if (!c.ultimaFechaCompra) return false;
        const fecha = new Date(c.ultimaFechaCompra);
        return fecha >= desde && fecha <= hasta;
      });
    }

    // Filtro por rango de montos
    if (filtroMontoMin || filtroMontoMax) {
      const min = filtroMontoMin ? parseFloat(filtroMontoMin) : 0;
      const max = filtroMontoMax ? parseFloat(filtroMontoMax) : Infinity;

      if (min > max) {
        setError('El monto mínimo no puede ser mayor al monto máximo');
        return;
      }

      resultado = resultado.filter((c) => c.montoTotalComprado >= min && c.montoTotalComprado <= max);
    }

    // Ordenar
    resultado.sort((a, b) => {
      let valorA: any;
      let valorB: any;

      switch (ordenColumna) {
        case 'nombre':
          valorA = a.nombreCompleto.toLowerCase();
          valorB = b.nombreCompleto.toLowerCase();
          break;
        case 'numeroCompras':
          valorA = a.numeroCompras;
          valorB = b.numeroCompras;
          break;
        case 'montoTotal':
          valorA = a.montoTotalComprado;
          valorB = b.montoTotalComprado;
          break;
        case 'ultimaCompra':
          valorA = a.ultimaFechaCompra ? new Date(a.ultimaFechaCompra).getTime() : 0;
          valorB = b.ultimaFechaCompra ? new Date(b.ultimaFechaCompra).getTime() : 0;
          break;
        default:
          return 0;
      }

      if (valorA < valorB) return ordenDireccion === 'asc' ? -1 : 1;
      if (valorA > valorB) return ordenDireccion === 'asc' ? 1 : -1;
      return 0;
    });

    setClientesFiltrados(resultado);
    setError(null);
  };

  const handleOrdenar = (columna: typeof ordenColumna) => {
    if (ordenColumna === columna) {
      setOrdenDireccion(ordenDireccion === 'asc' ? 'desc' : 'asc');
    } else {
      setOrdenColumna(columna);
      setOrdenDireccion('asc');
    }
  };

  const limpiarFiltros = () => {
    setBusqueda('');
    setFiltroFechaDesde('');
    setFiltroFechaHasta('');
    setFiltroMontoMin('');
    setFiltroMontoMax('');
  };

  const handleModificar = (cliente: ClienteConEstadisticas) => {
    setClienteSeleccionado(cliente);
    setShowModificarModal(true);
  };

  const handleVerHistorial = (cliente: ClienteConEstadisticas) => {
    setClienteSeleccionado(cliente);
    setShowHistorialModal(true);
  };

  // Antes solo mostraba un alert de exito sin producir archivo. Ahora exporta
  // de verdad los clientes que quedaron despues de aplicar los filtros.
  const handleExportarExcel = () => {
    if (clientesFiltrados.length === 0) {
      alert('No hay clientes para exportar con los filtros actuales.');
      return;
    }

    exportarCSV<ClienteConEstadisticas>(
      `Clientes_${fechaArchivo()}`,
      [
        { encabezado: 'Codigo', valor: (c) => c.id },
        { encabezado: 'Nombre completo', valor: (c) => c.nombreCompleto },
        { encabezado: 'NIT / CI', valor: (c) => c.nitCi ?? '' },
        { encabezado: 'Telefono', valor: (c) => c.telefono ?? '' },
        { encabezado: 'Email', valor: (c) => c.email ?? '' },
        { encabezado: 'Tipo', valor: (c) => c.tipoCliente },
        { encabezado: 'Estado', valor: (c) => (c.activo ? 'Activo' : 'Inactivo') },
        { encabezado: 'Compras', valor: (c) => c.numeroCompras },
        { encabezado: 'Total comprado (Bs)', valor: (c) => Number(c.montoTotalComprado ?? 0).toFixed(2) },
        {
          encabezado: 'Ultima compra',
          valor: (c) => (c.ultimaFechaCompra ? new Date(c.ultimaFechaCompra).toLocaleDateString('es-BO') : 'Sin compras'),
        },
        { encabezado: 'Fecha de registro', valor: (c) => new Date(c.fechaRegistro).toLocaleDateString('es-BO') },
      ],
      clientesFiltrados
    );
  };

  const formatFecha = (fecha?: string) => {
    if (!fecha) return 'Sin compras';
    return new Date(fecha).toLocaleDateString('es-BO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Cargando clientes...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header con botón volver */}
      <div className="mb-8">
        <Link href="/dashboard/ventas" className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4 transition-colors">
        <ArrowLeft size={20} />
        <span className="font-medium">Volver a Ventas</span>
      </Link>

        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
              <Users className="text-blue-600" size={36} />
              Gestión de Clientes
            </h1>
            <p className="text-gray-600 mt-2">
              Administra la información de tus clientes y consulta su historial de compras
            </p>
          </div>
        </div>

        {/* Estadísticas - Indicadores superiores (P6.1) */}
        {estadisticas && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg p-6 text-white shadow-lg">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-blue-100 text-sm font-medium">Total de Clientes</p>
                  <p className="text-3xl font-bold mt-2">{estadisticas.totalClientes}</p>
                  <p className="text-blue-100 text-sm mt-1">Registrados en el sistema</p>
                </div>
                <Users size={40} className="text-blue-200" />
              </div>
            </div>

            <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-lg p-6 text-white shadow-lg">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-green-100 text-sm font-medium">Compras Este Mes</p>
                  <p className="text-3xl font-bold mt-2">{estadisticas.clientesConComprasEsteMes}</p>
                  <p className="text-green-100 text-sm mt-1">Clientes activos</p>
                </div>
                <ShoppingBag size={40} className="text-green-200" />
              </div>
            </div>

            <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg p-6 text-white shadow-lg">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-purple-100 text-sm font-medium">Cliente Top</p>
                  {estadisticas.clienteTopNombre ? (
                    <>
                      <p className="text-xl font-bold mt-2 truncate">
                        {estadisticas.clienteTopNombre}
                      </p>
                      <p className="text-purple-100 text-sm mt-1">
                        Bs. {estadisticas.clienteTopMonto?.toFixed(2)}
                      </p>
                    </>
                  ) : (
                    <p className="text-lg mt-2">Sin datos</p>
                  )}
                </div>
                <Crown size={40} className="text-purple-200" />
              </div>
            </div>
          </div>
        )}

        {/* Filtros */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <Filter size={20} className="text-gray-600" />
            <h3 className="font-semibold text-gray-900">Filtros de Búsqueda</h3>
          </div>

          {/* Búsqueda principal */}
          <div className="mb-4">
            <div className="relative">
              <Search
                className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"
                size={18}
              />
              <input
                type="text"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                placeholder="Buscar por nombre, teléfono o NIT/CI..."
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Filtros avanzados */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Fecha Desde</label>
              <input
                type="date"
                value={filtroFechaDesde}
                onChange={(e) => setFiltroFechaDesde(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Fecha Hasta</label>
              <input
                type="date"
                value={filtroFechaHasta}
                onChange={(e) => setFiltroFechaHasta(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Monto Mínimo (Bs.)</label>
              <input
                type="number"
                value={filtroMontoMin}
                onChange={(e) => setFiltroMontoMin(e.target.value)}
                placeholder="0.00"
                step="0.01"
                min="0"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Monto Máximo (Bs.)</label>
              <input
                type="number"
                value={filtroMontoMax}
                onChange={(e) => setFiltroMontoMax(e.target.value)}
                placeholder="10000.00"
                step="0.01"
                min="0"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm text-gray-600">
              Mostrando <span className="font-semibold">{clientesFiltrados.length}</span> de{' '}
              <span className="font-semibold">{clientes.length}</span> clientes
            </p>
            <div className="flex gap-3">
              <button
                onClick={handleExportarExcel}
                className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white hover:bg-green-700 rounded-lg transition-colors shadow-md"
              >
                <Download size={16} />
                Exportar a Excel
              </button>
              <button
                onClick={limpiarFiltros}
                className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <X size={16} />
                Limpiar Filtros
              </button>
              <button
                onClick={() => {
                  loadClientes();
                  loadEstadisticas();
                }}
                className="flex items-center gap-2 px-4 py-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
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

      {/* Tabla de clientes (P6.1) */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th
                  onClick={() => handleOrdenar('nombre')}
                  className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                >
                  <div className="flex items-center gap-2">
                    Nombre Completo
                    {ordenColumna === 'nombre' && (
                      <TrendingUp size={14} className={ordenDireccion === 'desc' ? 'rotate-180' : ''} />
                    )}
                  </div>
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Teléfono
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  NIT / CI
                </th>
                <th
                  onClick={() => handleOrdenar('numeroCompras')}
                  className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                >
                  <div className="flex items-center justify-center gap-2">
                    # Compras
                    {ordenColumna === 'numeroCompras' && (
                      <TrendingUp size={14} className={ordenDireccion === 'desc' ? 'rotate-180' : ''} />
                    )}
                  </div>
                </th>
                <th
                  onClick={() => handleOrdenar('montoTotal')}
                  className="px-6 py-4 text-right text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                >
                  <div className="flex items-center justify-end gap-2">
                    Monto Total (Bs.)
                    {ordenColumna === 'montoTotal' && (
                      <TrendingUp size={14} className={ordenDireccion === 'desc' ? 'rotate-180' : ''} />
                    )}
                  </div>
                </th>
                <th
                  onClick={() => handleOrdenar('ultimaCompra')}
                  className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                >
                  <div className="flex items-center justify-center gap-2">
                    Última Compra
                    {ordenColumna === 'ultimaCompra' && (
                      <TrendingUp size={14} className={ordenDireccion === 'desc' ? 'rotate-180' : ''} />
                    )}
                  </div>
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {clientesFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center">
                    <Users className="mx-auto text-gray-400 mb-3" size={48} />
                    <p className="text-gray-500 font-medium">No se encontraron clientes</p>
                    <p className="text-sm text-gray-400 mt-1">
                      {busqueda ||
                      filtroFechaDesde ||
                      filtroFechaHasta ||
                      filtroMontoMin ||
                      filtroMontoMax
                        ? 'Intenta ajustar los filtros de búsqueda'
                        : 'No hay clientes registrados en el sistema'}
                    </p>
                  </td>
                </tr>
              ) : (
                clientesFiltrados.map((cliente) => (
                  <tr key={cliente.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{cliente.nombreCompleto}</div>
                      {cliente.email && (
                        <div className="text-xs text-gray-500">{cliente.email}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {cliente.telefono || "-"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {cliente.nitCi || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className="px-3 py-1 text-sm font-semibold bg-blue-100 text-blue-800 rounded-full">
                        {cliente.numeroCompras}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <span className="text-lg font-semibold text-gray-900">
                        Bs. {cliente.montoTotalComprado.toFixed(2)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center text-sm text-gray-900">
                      {formatFecha(cliente.ultimaFechaCompra)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => handleModificar(cliente)}
                          className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="Modificar cliente"
                        >
                          <Edit2 size={18} />
                        </button>
                        <button
                          onClick={() => handleVerHistorial(cliente)}
                          className="p-2 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                          title="Ver historial de compras"
                        >
                          <History size={18} />
                        </button>
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
      {clienteSeleccionado && (
        <>
          <ModificarClienteModal
            isOpen={showModificarModal}
            onClose={() => {
              setShowModificarModal(false);
              setClienteSeleccionado(null);
            }}
            onSuccess={() => {
              loadClientes();
              loadEstadisticas();
            }}
            cliente={clienteSeleccionado}
          />

          <HistorialComprasModal
            isOpen={showHistorialModal}
            onClose={() => {
              setShowHistorialModal(false);
              setClienteSeleccionado(null);
            }}
            clienteId={clienteSeleccionado.id}
          />
        </>
      )}
    </div>
  );
}