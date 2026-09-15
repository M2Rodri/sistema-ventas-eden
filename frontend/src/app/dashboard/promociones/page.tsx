'use client';

import { useState, useEffect } from 'react';
import { 
  getAllPromociones, 
  deletePromocion, 
  togglePromocionStatus,
  getActiveProductos
} from '@/lib/api';
import { Promocion, EstadoPromocion } from '@/types/promocion';
import { Producto } from '@/types/producto';
import { Search, Tag, Edit, Trash2, Power, Plus } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import PromocionModal from '@/components/PromocionModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';

export default function PromocionesPage() {
  // Estados para Promociones
  const [promociones, setPromocions] = useState<Promocion[]>([]);
  const [filteredPromocions, setFilteredPromocions] = useState<Promocion[]>([]);
  const [searchPromocion, setSearchPromocion] = useState('');
  const [estadoPromocionFilter, setEstadoPromocionFilter] = useState<string>('TODAS');
  
  // Estados generales
  const [productos, setProductos] = useState<Producto[]>([]);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
  
  // Modals
  const [isPromocionModalOpen, setIsPromocionModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedPromocion, setSelectedPromocion] = useState<Promocion | null>(null);
  const [promocionToDelete, setPromocionToDelete] = useState<Promocion | null>(null);

  // Cargar datos
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled: la lista de productos solo alimenta el selector del
      // formulario; que falle no debe tapar las promociones ya cargadas.
      const [rPromociones, rProductos] = await Promise.allSettled([
        getAllPromociones(),
        getActiveProductos()
      ]);

      const { tomar, fallos } = crearRecolector();
      const promocionsData = tomar(rPromociones, 'las promociones', [] as Promocion[]);
      const productosData = tomar(rProductos, 'los productos', [] as Producto[]);

      setPromocions(promocionsData);
      setFilteredPromocions(promocionsData);
      setProductos(productosData);
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar Promociones
  useEffect(() => {
    let filtered = promociones;

    if (searchPromocion) {
      filtered = filtered.filter(o =>
        o.nombre.toLowerCase().includes(searchPromocion.toLowerCase()) ||
        (o.descripcion ?? '').toLowerCase().includes(searchPromocion.toLowerCase())
      );
    }

    if (estadoPromocionFilter !== 'TODAS') {
      filtered = filtered.filter(o => getEstadoPromocion(o) === estadoPromocionFilter);
    }

    setFilteredPromocions(filtered);
  }, [searchPromocion, estadoPromocionFilter, promociones]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  // ========== PROMOCIONS ==========
  const getEstadoPromocion = (promocion: Promocion): EstadoPromocion => {
    const hoy = new Date();
    const inicio = new Date(promocion.fechaInicio);
    const fin = new Date(promocion.fechaFin);

    if (!promocion.activo) return 'INACTIVA';
    if (hoy < inicio) return 'PROGRAMADA';
    if (hoy > fin) return 'VENCIDA';
    return 'ACTIVA';
  };

  const getEstadoBadge = (estado: EstadoPromocion) => {
    const colors = {
      ACTIVA: 'bg-green-100 text-green-800',
      PROGRAMADA: 'bg-yellow-100 text-yellow-800',
      VENCIDA: 'bg-red-100 text-red-800',
      INACTIVA: 'bg-gray-100 text-gray-800',
    };
    const icons = {
      ACTIVA: '🟢',
      PROGRAMADA: '🟡',
      VENCIDA: '🔴',
      INACTIVA: '⚫',
    };
    return { color: colors[estado], icon: icons[estado] };
  };

  const handleCreatePromocion = () => {
    setSelectedPromocion(null);
    setIsPromocionModalOpen(true);
  };

  const handleEditPromocion = (promocion: Promocion) => {
    setSelectedPromocion(promocion);
    setIsPromocionModalOpen(true);
  };

  const handleDeletePromocionClick = (promocion: Promocion) => {
    setPromocionToDelete(promocion);
    setIsDeleteModalOpen(true);
  };

  const handleDeletePromocionConfirm = async () => {
    if (!promocionToDelete) return;

    try {
      await deletePromocion(promocionToDelete.id);
      showMessage('success', 'Promoción eliminada correctamente');
      loadData();
      setIsDeleteModalOpen(false);
      setPromocionToDelete(null);
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  const handleTogglePromocionStatus = async (promocion: Promocion) => {
    try {
      await togglePromocionStatus(promocion.id);
      showMessage('success', `Promoción ${promocion.activo ? 'desactivada' : 'activada'} correctamente`);
      loadData();
    } catch (error: any) {
      showMessage('error', error.message);
    }
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
        <Breadcrumbs items={[{ label: 'Promociones' }]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Promociones' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* Header: Título a la izquierda, Filtros a la derecha en la misma línea */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        {/* Título y descripción */}
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Promociones</h1>
          <p className="text-gray-600 mt-1">Gestiona los descuentos vigentes y programados</p>
        </div>
        
        {/* Filtros en una línea - wrap cuando sea necesario */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="text"
                placeholder="Buscar promoción..."
                value={searchPromocion}
                onChange={(e) => setSearchPromocion(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* Filtro por estado */}
          <select
              value={estadoPromocionFilter}
              onChange={(e) => setEstadoPromocionFilter(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
            >
              <option value="TODAS">Todos los estados</option>
              <option value="ACTIVA">Activas</option>
              <option value="PROGRAMADA">Programadas</option>
              <option value="VENCIDA">Vencidas</option>
            <option value="INACTIVA">Inactivas</option>
          </select>

          {/* Botón Nueva Promoción */}
          <button
            onClick={handleCreatePromocion}
              className="flex items-center justify-center gap-2 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap"
            >
            <Plus size={20} />
            Nueva Promoción
          </button>
        </div>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* ========== CONTENIDO: PROMOCIONS ========== */}
      <div>
          {/* Tabla de Promociones */}
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Promoción</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Descuento</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Inicio</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Fin</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Productos</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredPromocions.map((promocion) => {
                    const estado = getEstadoPromocion(promocion);
                    const badge = getEstadoBadge(estado);
                    return (
                      <tr key={promocion.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-6 py-4">
                          <div className="text-sm font-medium text-gray-900 max-w-xs truncate">
                            {promocion.nombre}
                          </div>
                          {promocion.descripcion && (
                            <div className="text-xs text-gray-500 max-w-xs truncate">
                              {promocion.descripcion}
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className="text-lg font-bold text-primary-600">{promocion.descuento}%</span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                          {formatDate(promocion.fechaInicio)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                          {formatDate(promocion.fechaFin)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                          {promocion.productos.length} productos
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${badge.color}`}>
                            {badge.icon} {estado}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                          <div className="flex items-center gap-3">
                            <button
                              onClick={() => handleTogglePromocionStatus(promocion)}
                              className={`${
                                promocion.activo ? 'text-yellow-600 hover:text-yellow-800' : 'text-green-600 hover:text-green-800'
                              } transition-colors`}
                              title={promocion.activo ? 'Desactivar' : 'Activar'}
                            >
                              <Power size={18} />
                            </button>
                            <button
                              onClick={() => handleEditPromocion(promocion)}
                              className="text-blue-600 hover:text-blue-800 transition-colors"
                              title="Editar"
                            >
                              <Edit size={18} />
                            </button>
                            <button
                              onClick={() => handleDeletePromocionClick(promocion)}
                              className="text-red-600 hover:text-red-800 transition-colors"
                              title="Eliminar"
                            >
                              <Trash2 size={18} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {filteredPromocions.length === 0 && (
                <div className="text-center py-12">
                  <Tag size={48} className="mx-auto text-gray-400 mb-4" />
                  <p className="text-gray-500">No se encontraron promociones</p>
                </div>
              )}
            </div>
          </div>
      </div>

      {/* Modals */}
      {isPromocionModalOpen && (
        <PromocionModal
          promocion={selectedPromocion}
          productos={productos}
          onClose={() => setIsPromocionModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsPromocionModalOpen(false);
            showMessage('success', selectedPromocion ? 'Promoción actualizada correctamente' : 'Promoción creada correctamente');
          }}
        />
      )}

      {isDeleteModalOpen && promocionToDelete && (
        <DeleteConfirmModal
          title="Eliminar Promoción"
          message={`¿Estás seguro de que deseas eliminar la promocion "${promocionToDelete.descripcion}"?`}
          onConfirm={handleDeletePromocionConfirm}
          onCancel={() => {
            setIsDeleteModalOpen(false);
            setPromocionToDelete(null);
          }}
        />
      )}
    </div>
  );
}