'use client';

import { useState, useEffect } from 'react';
import { getAllCategorias, deleteCategoria, toggleCategoriaStatus } from '@/lib/api';
import { Categoria } from '@/types/producto';
import { Search, FolderOpen, Edit, Trash2, Power, Package } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import CategoriaModal from '@/components/CategoriaModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';

export default function CategoriasPage() {
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [filteredCategorias, setFilteredCategorias] = useState<Categoria[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');
  
  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedCategoria, setSelectedCategoria] = useState<Categoria | null>(null);
  const [categoriaToDelete, setCategoriaToDelete] = useState<Categoria | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar categorías
  useEffect(() => {
    loadCategorias();
  }, []);

  const loadCategorias = async () => {
    try {
      setLoading(true);
      const data = await getAllCategorias();
      setCategorias(data);
      setFilteredCategorias(data);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar categorías
  useEffect(() => {
    let filtered = categorias;

    // Filtro por búsqueda
    if (searchTerm) {
      filtered = filtered.filter(categoria =>
        categoria.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (categoria.descripcion && categoria.descripcion.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    // Filtro por estado
    if (statusFilter === 'ACTIVOS') {
      filtered = filtered.filter(categoria => categoria.activo);
    } else if (statusFilter === 'INACTIVOS') {
      filtered = filtered.filter(categoria => !categoria.activo);
    }

    setFilteredCategorias(filtered);
  }, [searchTerm, statusFilter, categorias]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  const handleCreateCategoria = () => {
    setSelectedCategoria(null);
    setIsModalOpen(true);
  };

  const handleEditCategoria = (categoria: Categoria) => {
    setSelectedCategoria(categoria);
    setIsModalOpen(true);
  };

  const handleDeleteClick = (categoria: Categoria) => {
    setCategoriaToDelete(categoria);
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!categoriaToDelete) return;

    try {
      await deleteCategoria(categoriaToDelete.id);
      showMessage('success', 'Categoría eliminada correctamente');
      loadCategorias();
      setIsDeleteModalOpen(false);
      setCategoriaToDelete(null);
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  const handleToggleStatus = async (categoria: Categoria) => {
    try {
      await toggleCategoriaStatus(categoria.id);
      showMessage('success', `Categoría ${categoria.activo ? 'desactivada' : 'activada'} correctamente`);
      loadCategorias();
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
        <Breadcrumbs items={[{ label: 'Categorías' }]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Categorías' }]} />

      {/* Header: Título a la izquierda, Filtros a la derecha */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        {/* Título y descripción */}
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Categorías</h1>
          <p className="text-gray-600 mt-1">Administra las categorías de productos</p>
        </div>
        
        {/* Filtros en una línea */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar categoría..."
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

          {/* Botón Nueva Categoría */}
          <button
            onClick={handleCreateCategoria}
            className="flex items-center justify-center gap-2 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap"
          >
            <FolderOpen size={20} />
            Nueva Categoría
          </button>
        </div>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Tabla */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nombre</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Descripción</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Productos</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Estado</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha Creación</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredCategorias.map((categoria) => (
                <tr key={categoria.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{categoria.id}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <FolderOpen size={18} className="text-primary-600" />
                      <span className="text-sm font-medium text-gray-900">{categoria.nombre}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-600 max-w-md truncate">
                      {categoria.descripcion || '-'}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-1 text-sm text-gray-600">
                      <Package size={16} className="text-gray-400" />
                      {categoria.cantidadProductos}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={() => handleToggleStatus(categoria)}
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold transition-colors ${
                        categoria.activo ? 'bg-green-100 text-green-800 hover:bg-green-200' : 'bg-red-100 text-red-800 hover:bg-red-200'
                      }`}
                    >
                      <Power size={14} />
                      {categoria.activo ? 'Activo' : 'Inactivo'}
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">{formatDate(categoria.fechaCreacion)}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleEditCategoria(categoria)}
                        className="text-blue-600 hover:text-blue-800 transition-colors"
                        title="Editar"
                      >
                        <Edit size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(categoria)}
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

          {filteredCategorias.length === 0 && (
            <div className="text-center py-12">
              <FolderOpen size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">No se encontraron categorías</p>
            </div>
          )}
        </div>
      </div>

      {/* Modals */}
      {isModalOpen && (
        <CategoriaModal
          categoria={selectedCategoria}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            loadCategorias();
            setIsModalOpen(false);
            showMessage('success', selectedCategoria ? 'Categoría actualizada correctamente' : 'Categoría creada correctamente');
          }}
        />
      )}

      {isDeleteModalOpen && categoriaToDelete && (
        <DeleteConfirmModal
          title="Eliminar Categoría"
          message={`¿Estás seguro de que deseas eliminar "${categoriaToDelete.nombre}"?`}
          onConfirm={handleDeleteConfirm}
          onCancel={() => {
            setIsDeleteModalOpen(false);
            setCategoriaToDelete(null);
          }}
        />
      )}
    </div>
  );
}