'use client';

import { useState, useEffect } from 'react';
import { 
  getAllEnvios, 
  getEnviosEstadisticas,
  getAllTransportadoras 
} from '@/lib/api';
import { Envio, EnvioEstadisticas, EstadoEnvio } from '@/types/envio';
import { Transportadora } from '@/types/transportadora';
import { 
  Search, 
  Truck, 
  Package, 
  Clock, 
  CheckCircle, 
  MapPin, 
  Edit, 
  Eye,
  Building2
} from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import EnvioModal from '@/components/EnvioModal';
import EnvioDetalleModal from '@/components/EnvioDetalleModal';
import Link from 'next/link';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';

export default function EnviosPage() {
  const [envios, setEnvios] = useState<Envio[]>([]);
  const [filteredEnvios, setFilteredEnvios] = useState<Envio[]>([]);
  const [estadisticas, setEstadisticas] = useState<EnvioEstadisticas | null>(null);
  const [transportadoras, setTransportadoras] = useState<Transportadora[]>([]);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [estadoFilter, setEstadoFilter] = useState<string>('TODOS');
  const [transportadoraFilter, setTransportadoraFilter] = useState<string>('TODOS');
  
  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDetalleModalOpen, setIsDetalleModalOpen] = useState(false);
  const [selectedEnvio, setSelectedEnvio] = useState<Envio | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar datos
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled: si fallan las estadisticas o las transportadoras, la lista
      // de envios igual tiene que verse.
      const [rEnvios, rStats, rTransportadoras] = await Promise.allSettled([
        getAllEnvios(),
        getEnviosEstadisticas(),
        getAllTransportadoras()
      ]);

      const { tomar, fallos } = crearRecolector();
      const enviosData = tomar(rEnvios, 'los envios', [] as Envio[]);
      const statsData = tomar(rStats, 'las estadisticas de envios', null as EnvioEstadisticas | null);
      const transportadorasData = tomar(rTransportadoras, 'las transportadoras', [] as Transportadora[]);

      setEnvios(enviosData);
      setFilteredEnvios(enviosData);
      setEstadisticas(statsData);
      setTransportadoras(transportadorasData);
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar envíos
  useEffect(() => {
    let filtered = envios;

    // Filtro por búsqueda (guía, cliente)
    if (searchTerm) {
      filtered = filtered.filter(envio =>
        envio.guiaRemision?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        envio.nombreCliente.toLowerCase().includes(searchTerm.toLowerCase()) ||
        envio.direccionDestino.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Filtro por estado
    if (estadoFilter !== 'TODOS') {
      filtered = filtered.filter(envio => envio.estadoSeguimiento === estadoFilter);
    }

    // Filtro por transportadora
    if (transportadoraFilter !== 'TODOS') {
      filtered = filtered.filter(envio => 
        envio.idTransportadora === parseInt(transportadoraFilter)
      );
    }

    setFilteredEnvios(filtered);
  }, [searchTerm, estadoFilter, transportadoraFilter, envios]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  const handleCreateEnvio = () => {
    setSelectedEnvio(null);
    setIsModalOpen(true);
  };

  const handleEditEnvio = (envio: Envio) => {
    setSelectedEnvio(envio);
    setIsModalOpen(true);
  };

  const handleVerDetalle = (envio: Envio) => {
    setSelectedEnvio(envio);
    setIsDetalleModalOpen(true);
  };

  const getEstadoBadge = (estado: EstadoEnvio) => {
    const badges = {
      PENDIENTE: 'bg-gray-100 text-gray-800',
      EN_PREPARACION: 'bg-blue-100 text-blue-800',
      EN_CAMINO: 'bg-yellow-100 text-yellow-800',
      ENTREGADO: 'bg-green-100 text-green-800',
      DEVUELTO: 'bg-orange-100 text-orange-800',
      CANCELADO: 'bg-red-100 text-red-800',
    };
    return badges[estado] || 'bg-gray-100 text-gray-800';
  };

  const getEstadoIcon = (estado: EstadoEnvio) => {
    const icons = {
      PENDIENTE: <Clock size={16} />,
      EN_PREPARACION: <Package size={16} />,
      EN_CAMINO: <Truck size={16} />,
      ENTREGADO: <CheckCircle size={16} />,
      DEVUELTO: <MapPin size={16} />,
      CANCELADO: <MapPin size={16} />,
    };
    return icons[estado] || <Clock size={16} />;
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  if (loading) {
    return (
      <div>
        <Breadcrumbs items={[{ label: 'Envíos' }]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Envíos' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* Header con filtros responsive */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        {/* Título */}
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Envíos</h1>
          <p className="text-gray-600 mt-1">Administra y realiza seguimiento de envíos</p>
        </div>
        
        {/* Filtros - wrap automático */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar por guía o cliente..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* Filtro por estado */}
          <select
            value={estadoFilter}
            onChange={(e) => setEstadoFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los estados</option>
            <option value="PENDIENTE">Pendiente</option>
            {/*<option value="EN_PREPARACION">En Preparación</option>*/}
            {/*<option value="EN_CAMINO">En Camino</option>*/}
            <option value="ENTREGADO">Entregado</option>
            {/*<option value="DEVUELTO">Devuelto</option>*/}
            <option value="CANCELADO">Cancelado</option>
          </select>

          {/* Filtro por transportadora */}
          <select
            value={transportadoraFilter}
            onChange={(e) => setTransportadoraFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todas las transportadoras</option>
            {transportadoras.map((t) => (
              <option key={t.id} value={t.id}>{t.nombre}</option>
            ))}
          </select>

          {/* Botón Gestionar Transportadoras */}
          <Link
            href="/dashboard/envios/transportadoras"
            className="flex items-center gap-2 px-4 py-2 bg-secondary-600 text-white rounded-lg hover:bg-secondary-700 transition-colors font-medium whitespace-nowrap"
          >
            <Building2 size={20} />
            <span className="hidden sm:inline">Transportadoras</span>
          </Link>

          {/* Botón Nuevo Envío */}
          <button
            onClick={handleCreateEnvio}
            className="flex items-center gap-2 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap"
          >
            <Truck size={20} />
            Nuevo Envío
          </button>
        </div>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Resumen de Métricas */}
      {estadisticas && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="text-gray-600" size={20} />
              <span className="text-xs text-gray-600 font-medium">Pendientes</span>
            </div>
            <p className="text-2xl font-bold text-gray-900">{estadisticas.pendientes}</p>
          </div>
          {/*
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Package className="text-blue-600" size={20} />
              <span className="text-xs text-blue-700 font-medium">En Preparación</span>
            </div>
            <p className="text-2xl font-bold text-blue-700">{estadisticas.enPreparacion}</p>
          </div>
          */}
          {/*
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Truck className="text-yellow-600" size={20} />
              <span className="text-xs text-yellow-700 font-medium">En Camino</span>
            </div>
            <p className="text-2xl font-bold text-yellow-700">{estadisticas.enCamino}</p>
          </div>
          */}
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle className="text-green-600" size={20} />
              <span className="text-xs text-green-700 font-medium">Entregados</span>
            </div>
            <p className="text-2xl font-bold text-green-700">{estadisticas.entregados}</p>
          </div>
            {/*
          <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <MapPin className="text-orange-600" size={20} />
              <span className="text-xs text-orange-700 font-medium">Devueltos</span>
            </div>
            <p className="text-2xl font-bold text-orange-700">{estadisticas.devueltos}</p>
          </div>
            */}
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <MapPin className="text-red-600" size={20} />
              <span className="text-xs text-red-700 font-medium">Cancelados</span>
            </div>
            <p className="text-2xl font-bold text-red-700">{estadisticas.cancelados}</p>
          </div>
        </div>
      )}

      {/* Tabla con scroll horizontal responsive */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Guía</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Cliente</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Destino</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Transportadora</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">F. Estimada</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredEnvios.map((envio) => (
                <tr key={envio.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span className="text-sm font-mono font-semibold text-primary-600">
                      {envio.guiaRemision || '-'}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{envio.nombreCliente}</div>
                    <div className="text-xs text-gray-500">{envio.telefonoCliente}</div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="text-sm text-gray-900 max-w-xs truncate">{envio.direccionDestino}</div>
                    {envio.ciudad && (
                      <div className="text-xs text-gray-500">{envio.ciudad}, {envio.departamento}</div>
                    )}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {envio.nombreTransportadora || '-'}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatDate(envio.fechaEntregaEstimada)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold w-fit ${getEstadoBadge(envio.estadoSeguimiento)}`}>
                      {getEstadoIcon(envio.estadoSeguimiento)}
                      {envio.estadoSeguimiento.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleVerDetalle(envio)}
                        className="text-green-600 hover:text-green-800 transition-colors"
                        title="Ver Detalle"
                      >
                        <Eye size={18} />
                      </button>
                      <button
                        onClick={() => handleEditEnvio(envio)}
                        className="text-blue-600 hover:text-blue-800transition-colors"
                        title="Editar Estado"
                      >
                        <Edit size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {filteredEnvios.length === 0 && (
            <div className="text-center py-12">
              <Truck size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">No se encontraron envíos</p>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      {isModalOpen && (
        <EnvioModal
          envio={selectedEnvio}
          transportadoras={transportadoras}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsModalOpen(false);
            showMessage('success', selectedEnvio ? 'Envío actualizado correctamente' : 'Envío creado correctamente');
          }}
        />
      )}

      {isDetalleModalOpen && selectedEnvio && (
        <EnvioDetalleModal
          envio={selectedEnvio}
          onClose={() => setIsDetalleModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsDetalleModalOpen(false);
            showMessage('success', 'Estado actualizado correctamente');
          }}
        />
      )}
    </div>
  );
}