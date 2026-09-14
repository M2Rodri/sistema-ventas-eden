'use client';

import { useState, useEffect } from 'react';
import { 
  getAllTransportadoras, 
  deleteTransportadora, 
  toggleTransportadoraStatus 
} from '@/lib/api';
import { Transportadora } from '@/types/transportadora';
import { Search, Building2, Edit, Trash2, Power, ArrowLeft } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import TransportadoraModal from '@/components/TransportadoraModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import Link from 'next/link';

export default function TransportadorasPage() {
  const [transportadoras, setTransportadoras] = useState<Transportadora[]>([]);
  const [filteredTransportadoras, setFilteredTransportadoras] = useState<Transportadora[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');
  
  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedTransportadora, setSelectedTransportadora] = useState<Transportadora | null>(null);
  const [transportadoraToDelete, setTransportadoraToDelete] = useState<Transportadora | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar transportadoras
  useEffect(() => {
    loadTransportadoras();
  }, []);

  const loadTransportadoras = async () => {
    try {
      setLoading(true);
      const data = await getAllTransportadoras();
      setTransportadoras(data);
      setFilteredTransportadoras(data);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar transportadoras
  useEffect(() => {
    let filtered = transportadoras;

    // Filtro por búsqueda
    if (searchTerm) {
      filtered = filtered.filter(t =>
        t.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.email?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Filtro por estado
    if (statusFilter === 'ACTIVOS') {
      filtered = filtered.filter(t => t.activo);
    } else if (statusFilter === 'INACTIVOS') {
      filtered = filtered.filter(t => !t.activo);
    }

    setFilteredTransportadoras(filtered);
  }, [searchTerm, statusFilter, transportadoras]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  const handleCreateTransportadora = () => {
    setSelectedTransportadora(null);
    setIsModalOpen(true);
  };

  const handleEditTransportadora = (transportadora: Transportadora) => {
    setSelectedTransportadora(transportadora);
    setIsModalOpen(true);
  };

  const handleDeleteClick = (transportadora: Transportadora) => {
    setTransportadoraToDelete(transportadora);
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!transportadoraToDelete) return;

    try {
      await deleteTransportadora(transportadoraToDelete.id);
      showMessage('success', 'Transportadora eliminada correctamente');
      loadTransportadoras();
      setIsDeleteModalOpen(false);
      setTransportadoraToDelete(null);
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  const handleToggleStatus = async (transportadora: Transportadora) => {
    try {
      await toggleTransportadoraStatus(transportadora.id);
      showMessage('success', `Transportadora ${transportadora.activo ? 'desactivada' : 'activada'} correctamente`);
      loadTransportadoras();
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(value);
  };

  if (loading) {
    return (
      <div>
        <Breadcrumbs items={[
          { label: 'Envíos', href: '/dashboard/envios' },
          { label: 'Transportadoras' }
        ]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[
        { label: 'Envíos', href: '/dashboard/envios' },
        { label: 'Transportadoras' }
      ]} />

      {/* Header con filtros responsive */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        {/* Título y botón volver */}
        <div className="flex items-center gap-3">
          <Link
            href="/dashboard/envios"
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            title="Volver a Envíos"
          >
            <ArrowLeft size={24} className="text-gray-600" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Gestión de Transportadoras</h1>
            <p className="text-gray-600 mt-1">Administra las empresas de envío</p>
          </div>
        </div>
        
        {/* Filtros - wrap automático */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar transportadora..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* Filtro por estado */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los estados</option>
            <option value="ACTIVOS">Activos</option>
            <option value="INACTIVOS">Inactivos</option>
          </select>

          {/* Botón Nueva Transportadora */}
          <button
            onClick={handleCreateTransportadora}
            className="flex items-center gap-2 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap"
          >
            <Building2 size={20} />
            Nueva Transportadora
          </button>
        </div>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Tabla con scroll horizontal responsive */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Nombre</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Contacto</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Tarifa Base</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Tiempo Est.</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredTransportadoras.map((transportadora) => (
                <tr key={transportadora.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">{transportadora.id}</td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <div className="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center">
                        <Building2 className="text-primary-600" size={20} />
                      </div>
                      <div className="text-sm font-medium text-gray-900">{transportadora.nombre}</div>
                    </div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{transportadora.telefono || '-'}</div>
                    <div className="text-xs text-gray-500">{transportadora.email || "-"}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                    {formatCurrency(transportadora.tarifaBase)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600 text-center">
                    {transportadora.tiempoEstimadoDias} {transportadora.tiempoEstimadoDias === 1 ? 'día' : 'días'}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <button
                      onClick={() => handleToggleStatus(transportadora)}
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold transition-colors ${
                        transportadora.activo 
                          ? 'bg-green-100 text-green-800 hover:bg-green-200' 
                          : 'bg-red-100 text-red-800 hover:bg-red-200'
                      }`}
                    >
                      <Power size={14} />
                      {transportadora.activo ? 'Activo' : 'Inactivo'}
                    </button>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleEditTransportadora(transportadora)}
                        className="text-blue-600 hover:text-blue-800 transition-colors"
                        title="Editar"
                      >
                        <Edit size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(transportadora)}
                        className="text-red-600 hover:text-red-800 transition-colors"
                        title="Eliminar"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {filteredTransportadoras.length === 0 && (
            <div className="text-center py-12">
              <Building2 size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">No se encontraron transportadoras</p>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      {isModalOpen && (
        <TransportadoraModal
          transportadora={selectedTransportadora}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            loadTransportadoras();
            setIsModalOpen(false);
            showMessage('success', selectedTransportadora ? 'Transportadora actualizada correctamente' : 'Transportadora creada correctamente');
          }}
        />
      )}

      {isDeleteModalOpen && transportadoraToDelete && (
        <DeleteConfirmModal
          title="Eliminar Transportadora"
          message={`¿Estás seguro de que deseas eliminar "${transportadoraToDelete.nombre}"?`}
          onConfirm={handleDeleteConfirm}
          onCancel={() => {
            setIsDeleteModalOpen(false);
            setTransportadoraToDelete(null);
          }}
        />
      )}
    </div>
  );
}