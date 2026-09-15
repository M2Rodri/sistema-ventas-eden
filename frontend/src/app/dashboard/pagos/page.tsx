'use client';

import { useState, useEffect } from 'react';
import { 
  getAllPagos, 
  getPagosEstadisticas,
  getTotalPagosHoy,
  getTotalPagosMes,
  getAllVentas
} from '@/lib/api';
import { Pago, EstadoPago, MetodoPago } from '@/types/pago';
import { Venta } from '@/types/venta';
import { 
  Search, 
  CreditCard, 
  DollarSign, 
  Calendar, 
  CheckCircle, 
  Clock, 
  XCircle,
  Eye,
  Plus,
  Banknote,
  Smartphone
} from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import PagoModal from '@/components/PagoModal';
import PagoDetalleModal from '@/components/PagoDetalleModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';

export default function PagosPage() {
  const [pagos, setPagos] = useState<Pago[]>([]);
  const [ventas, setVentas] = useState<Venta[]>([]);
  const [filteredPagos, setFilteredPagos] = useState<Pago[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [metodoPagoFilter, setMetodoPagoFilter] = useState<string>('TODOS');
  const [estadoFilter, setEstadoFilter] = useState<string>('TODOS');
  const [fechaFilter, setFechaFilter] = useState<string>('');
  
  // Estadísticas
  const [totalHoy, setTotalHoy] = useState<number>(0);
  const [totalMes, setTotalMes] = useState<number>(0);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDetalleModalOpen, setIsDetalleModalOpen] = useState(false);
  const [selectedPago, setSelectedPago] = useState<Pago | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar datos
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled: los totales del dia y del mes son indicadores de apoyo;
      // que uno falle no debe borrar el listado de pagos.
      const [rPagos, rVentas, rTotalHoy, rTotalMes] = await Promise.allSettled([
        getAllPagos(),
        getAllVentas(),
        getTotalPagosHoy(),
        getTotalPagosMes()
      ]);

      const { tomar, fallos } = crearRecolector();
      const pagosData = tomar(rPagos, 'los pagos', [] as Pago[]);
      const ventasData = tomar(rVentas, 'las ventas', [] as Venta[]);
      const totalHoyData = tomar(rTotalHoy, 'el total cobrado hoy', 0);
      const totalMesData = tomar(rTotalMes, 'el total cobrado del mes', 0);

      setPagos(pagosData);
      setVentas(ventasData);
      setFilteredPagos(pagosData);
      setTotalHoy(totalHoyData);
      setTotalMes(totalMesData);
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar pagos
  useEffect(() => {
    let filtered = pagos;

    // Filtro por búsqueda (ID venta, cliente, referencia)
    if (searchTerm) {
      filtered = filtered.filter(pago => {
        const venta = ventas.find(v => v.id === pago.idVenta);
        return (
          pago.idVenta.toString().includes(searchTerm) ||
          pago.referencia?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          venta?.nombreCliente.toLowerCase().includes(searchTerm.toLowerCase())
        );
      });
    }

    // Filtro por método de pago
    if (metodoPagoFilter !== 'TODOS') {
      filtered = filtered.filter(pago => pago.metodoPago === metodoPagoFilter);
    }

    // Filtro por estado
    if (estadoFilter !== 'TODOS') {
      filtered = filtered.filter(pago => pago.estado === estadoFilter);
    }

    // Filtro por fecha
    if (fechaFilter) {
      filtered = filtered.filter(pago => {
        const fechaPago = new Date(pago.fechaPago).toISOString().split('T')[0];
        return fechaPago === fechaFilter;
      });
    }

    setFilteredPagos(filtered);
  }, [searchTerm, metodoPagoFilter, estadoFilter, fechaFilter, pagos, ventas]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  const handleRegistrarPago = () => {
    setIsModalOpen(true);
  };

  const handleVerDetalle = (pago: Pago) => {
    setSelectedPago(pago);
    setIsDetalleModalOpen(true);
  };

  const getEstadoBadge = (estado: EstadoPago) => {
    const badges = {
      COMPLETADO: 'bg-green-100 text-green-800 border-green-300',
      PENDIENTE: 'bg-yellow-100 text-yellow-800 border-yellow-300',
      RECHAZADO: 'bg-red-100 text-red-800 border-red-300',
    };
    return badges[estado] || 'bg-gray-100 text-gray-800 border-gray-300';
  };

  const getEstadoIcon = (estado: EstadoPago) => {
    const icons = {
      COMPLETADO: <CheckCircle size={16} className="text-green-600" />,
      PENDIENTE: <Clock size={16} className="text-yellow-600" />,
      RECHAZADO: <XCircle size={16} className="text-red-600" />,
    };
    return icons[estado];
  };

  const getMetodoPagoIcon = (metodo: MetodoPago) => {
    const icons = {
      EFECTIVO: <Banknote size={18} className="text-green-600" />,
      TARJETA: <CreditCard size={18} className="text-blue-600" />,
      TRANSFERENCIA: <DollarSign size={18} className="text-purple-600" />,
      QR: <Smartphone size={18} className="text-orange-600" />,
    };
    return icons[metodo];
  };

  const getMetodoPagoLabel = (metodo: MetodoPago) => {
    const labels = {
      EFECTIVO: '💵 Efectivo',
      TARJETA: '💳 Tarjeta',
      TRANSFERENCIA: '🏦 Transferencia',
      QR: '📱 QR',
    };
    return labels[metodo];
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(value);
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getVentaInfo = (idVenta: number) => {
    return ventas.find(v => v.id === idVenta);
  };

  if (loading) {
    return (
      <div>
        <Breadcrumbs items={[{ label: 'Pagos' }]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Pagos' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* Header con filtros responsive */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        {/* Título */}
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Pagos</h1>
          <p className="text-gray-600 mt-1">Control de pagos recibidos y cuotas pendientes</p>
        </div>
        
        {/* Botón Registrar Pago */}
        <button
          onClick={handleRegistrarPago}
          className="flex items-center gap-2 bg-primary-600 text-white px-6 py-2.5 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap shadow-md"
        >
          <Plus size={20} />
          Registrar Pago
        </button>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Resumen de Métricas */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-gradient-to-br from-green-50 to-green-100 border-2 border-green-200 rounded-lg p-6 shadow-md">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-12 h-12 rounded-full bg-green-500 flex items-center justify-center">
              <DollarSign className="text-white" size={24} />
            </div>
            <div>
              <p className="text-xs text-green-700 font-semibold uppercase tracking-wide">Total Hoy</p>
              <p className="text-2xl font-bold text-green-900">{formatCurrency(totalHoy)}</p>
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-blue-50 to-blue-100 border-2 border-blue-200 rounded-lg p-6 shadow-md">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-12 h-12 rounded-full bg-blue-500 flex items-center justify-center">
              <Calendar className="text-white" size={24} />
            </div>
            <div>
              <p className="text-xs text-blue-700 font-semibold uppercase tracking-wide">Total Mes</p>
              <p className="text-2xl font-bold text-blue-900">{formatCurrency(totalMes)}</p>
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-purple-50 to-purple-100 border-2 border-purple-200 rounded-lg p-6 shadow-md">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-12 h-12 rounded-full bg-purple-500 flex items-center justify-center">
              <CreditCard className="text-white" size={24} />
            </div>
            <div>
              <p className="text-xs text-purple-700 font-semibold uppercase tracking-wide">Total Pagos</p>
              <p className="text-2xl font-bold text-purple-900">{filteredPagos.length}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Filtros - wrap automático */}
      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-6 shadow-sm">
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative flex-1 min-w-[250px]">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar por #venta, cliente o referencia..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* Filtro por método de pago */}
          <select
            value={metodoPagoFilter}
            onChange={(e) => setMetodoPagoFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los métodos</option>
            <option value="EFECTIVO">💵 Efectivo</option>
            <option value="TARJETA">💳 Tarjeta</option>
            <option value="TRANSFERENCIA">🏦 Transferencia</option>
            <option value="QR">📱 QR</option>
          </select>

          {/* Filtro por estado */}
          <select
            value={estadoFilter}
            onChange={(e) => setEstadoFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los estados</option>
            <option value="COMPLETADO">✅ Completado</option>
            <option value="PENDIENTE">⏳ Pendiente</option>
            <option value="RECHAZADO">❌ Rechazado</option>
          </select>

          {/* Filtro por fecha */}
          <input
            type="date"
            value={fechaFilter}
            onChange={(e) => setFechaFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          />

          {/* Limpiar filtros */}
          {(searchTerm || metodoPagoFilter !== 'TODOS' || estadoFilter !== 'TODOS' || fechaFilter) && (
            <button
              onClick={() => {
                setSearchTerm('');
                setMetodoPagoFilter('TODOS');
                setEstadoFilter('TODOS');
                setFechaFilter('');
              }}
              className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
            >
              Limpiar filtros
            </button>
          )}
        </div>
      </div>

      {/* Tabla con scroll horizontal responsive */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">#Venta</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Cliente</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Monto</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Método</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Fecha</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredPagos.map((pago) => {
                const venta = getVentaInfo(pago.idVenta);
                return (
                  <tr key={pago.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                      #{pago.id}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-semibold text-primary-600">
                      #{pago.idVenta}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{venta?.nombreCliente || '-'}</div>
                      <div className="text-xs text-gray-500">{venta?.telefonoCliente || '-'}</div>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-bold text-gray-900">
                      {formatCurrency(pago.monto)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        {getMetodoPagoIcon(pago.metodoPago)}
                        <span className="text-sm text-gray-700">{pago.metodoPago}</span>
                      </div>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                      {formatDateTime(pago.fechaPago)}
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap">
                      <span className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold w-fit border-2 ${getEstadoBadge(pago.estado)}`}>
                        {getEstadoIcon(pago.estado)}
                        {pago.estado}
                      </span>
                    </td>
                    <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                      <button
                        onClick={() => handleVerDetalle(pago)}
                        className="text-primary-600 hover:text-primary-800 transition-colors"
                        title="Ver Detalle"
                      >
                        <Eye size={20} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {filteredPagos.length === 0 && (
            <div className="text-center py-12">
              <CreditCard size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">No se encontraron pagos</p>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      {isModalOpen && (
        <PagoModal
          ventas={ventas.filter(v => v.estado !== 'CANCELADA')}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsModalOpen(false);
            showMessage('success', 'Pago registrado correctamente');
          }}
        />
      )}

      {isDetalleModalOpen && selectedPago && (
        <PagoDetalleModal
          pago={selectedPago}
          venta={getVentaInfo(selectedPago.idVenta) || null}
          onClose={() => setIsDetalleModalOpen(false)}
        />
      )}
    </div>
  );
}