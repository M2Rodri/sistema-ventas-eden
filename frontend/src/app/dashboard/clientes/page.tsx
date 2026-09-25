'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';
import { getClientesConEstadisticas } from '@/lib/api';
import { ClienteConEstadisticas } from '@/types/cliente';
import {
  Search,
  Users,
  ArrowUpDown,
  Edit2,
  History,
  X,
} from 'lucide-react';
import ModificarClienteModal from '@/components/ModificarClienteModal';
import HistorialComprasModal from '@/components/HistorialComprasModal';
import Breadcrumbs from '@/components/Breadcrumbs';
import { mensajeError } from '@/lib/errores';

export default function ClientesPage() {
  const router = useRouter();

  const [clientes, setClientes] = useState<ClienteConEstadisticas[]>([]);
  const [clientesFiltrados, setClientesFiltrados] = useState<ClienteConEstadisticas[]>([]);
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

  // El orden ahora es un filtro más (select + dirección), no clickear el
  // encabezado de la tabla: nada en el encabezado avisaba que se podía
  // clickear, y de las 8 columnas solo 4 respondían, así que quedaba
  // inconsistente. Con esto queda igual de claro que los demás filtros de
  // arriba (búsqueda, categoría, etc. en otros módulos).
  const [ordenColumna, setOrdenColumna] = useState<
    'nombre' | 'numeroCompras' | 'montoTotal' | 'ultimaCompra'
  >('nombre');
  const [ordenDireccion, setOrdenDireccion] = useState<'asc' | 'desc'>('asc');

  useEffect(() => {
    loadClientes();
  }, []);

  useEffect(() => {
    aplicarFiltros();
  }, [clientes, busqueda, ordenColumna, ordenDireccion]);

  const loadClientes = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getClientesConEstadisticas();
      setClientes(data);
    } catch (err: any) {
      setError(mensajeError(err, 'No se pudieron cargar los clientes.'));
    } finally {
      setLoading(false);
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

  const handleModificar = (cliente: ClienteConEstadisticas) => {
    setClienteSeleccionado(cliente);
    setShowModificarModal(true);
  };

  const handleVerHistorial = (cliente: ClienteConEstadisticas) => {
    setClienteSeleccionado(cliente);
    setShowHistorialModal(true);
  };

  const formatFecha = (fecha?: string) => {
    if (!fecha) return 'Sin compras';
    return new Date(fecha).toLocaleDateString('es-BO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
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
  } = useDragScrollTable([loading, clientesFiltrados]);

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Clientes' }]} />

      {/* Header con búsqueda */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Clientes</h1>
          <p className="text-gray-600 mt-1">
            Administra la información de tus clientes y consulta su historial de compras
          </p>
        </div>

        {/* Filtros: búsqueda + orden */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar por nombre, teléfono o CI..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <select
            value={ordenColumna}
            onChange={(e) => setOrdenColumna(e.target.value as typeof ordenColumna)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="nombre">Ordenar por Nombre</option>
            <option value="numeroCompras">Ordenar por # Compras</option>
            <option value="montoTotal">Ordenar por Monto Total</option>
            <option value="ultimaCompra">Ordenar por Última Compra</option>
          </select>

          <button
            onClick={() => setOrdenDireccion(ordenDireccion === 'asc' ? 'desc' : 'asc')}
            className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg bg-white text-gray-700 hover:bg-gray-50 transition-colors"
            title={ordenDireccion === 'asc' ? 'Ascendente' : 'Descendente'}
          >
            <ArrowUpDown size={18} className={ordenDireccion === 'desc' ? 'rotate-180' : ''} />
            {ordenDireccion === 'asc' ? 'Ascendente' : 'Descendente'}
          </button>
        </div>
      </div>

      {/* Contador */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <p className="text-sm text-gray-600">
          Mostrando <span className="font-semibold">{clientesFiltrados.length}</span> de{' '}
          <span className="font-semibold">{clientes.length}</span> clientes
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

      {/* Tabla de clientes (P6.1) */}
      {loading ? (
        <div className="bg-white rounded-lg shadow-md flex items-center justify-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      ) : (
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="overflow-x-auto" ref={scrollContainerRef}>
          <table className="w-full" ref={tableRef}>
            <thead
              ref={theadRef}
              className={`bg-gray-50 border-b border-gray-200 ${hasOverflow ? 'cursor-grab select-none' : ''}`}
              {...theadProps}
            >
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Nombre Completo
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Teléfono
                </th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  CI
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  # Compras
                </th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Monto Total (Bs.)
                </th>
                <th className="px-6 py-4 text-center text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Última Compra
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
                      {busqueda
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
                      <span className="px-3 py-1 text-sm font-semibold bg-primary-50 text-primary-700 border border-primary-200 rounded-full">
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
                          className="p-2 text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
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
      )}

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
