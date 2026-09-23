'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';
import {
  getAllProveedores,
  deleteProveedor,
  toggleProveedorStatus,
} from '@/lib/api';
import { Proveedor } from '@/types/proveedor';
import { Search, Building2, Edit, Trash2, Power, ShoppingCart, Package, X } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import ProveedorModal from '@/components/ProveedorModal';
import CompraModal from '@/components/CompraModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import { mensajeError } from '@/lib/errores';

export default function ProveedoresPage() {
  const router = useRouter();
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [filteredProveedores, setFilteredProveedores] = useState<Proveedor[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

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
      setSearchTerm(q);
    }
  }, []);
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');
  
  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCompraModalOpen, setIsCompraModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedProveedor, setSelectedProveedor] = useState<Proveedor | null>(null);
  const [proveedorToDelete, setProveedorToDelete] = useState<Proveedor | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const proveedoresData = await getAllProveedores();

      setProveedores(proveedoresData);
      setFilteredProveedores(proveedoresData);
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudieron cargar los proveedores.'));
    } finally {
      setLoading(false);
    }
  };

  // Filtrar proveedores
  useEffect(() => {
    let filtered = proveedores;

    // Filtro por búsqueda
    if (searchTerm) {
      filtered = filtered.filter(proveedor =>
        proveedor.nombreEmpresa.toLowerCase().includes(searchTerm.toLowerCase()) ||
        proveedor.nit.toLowerCase().includes(searchTerm.toLowerCase()) ||
        proveedor.contacto.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Filtro por estado
    if (statusFilter === 'ACTIVOS') {
      filtered = filtered.filter(proveedor => proveedor.activo);
    } else if (statusFilter === 'INACTIVOS') {
      filtered = filtered.filter(proveedor => !proveedor.activo);
    }

    setFilteredProveedores(filtered);
  }, [searchTerm, statusFilter, proveedores]);

  // Los de éxito se cierran solos; los de error se quedan hasta que el
  // usuario los cierra a mano (el botón X del banner).
  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    if (type === 'success') {
      setTimeout(() => setMessage(null), 4000);
    }
  };

  const handleCreateProveedor = () => {
    setSelectedProveedor(null);
    setIsModalOpen(true);
  };

  const handleEditProveedor = (proveedor: Proveedor) => {
    setSelectedProveedor(proveedor);
    setIsModalOpen(true);
  };

  const handleDeleteClick = (proveedor: Proveedor) => {
    setProveedorToDelete(proveedor);
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!proveedorToDelete) return;

    try {
      await deleteProveedor(proveedorToDelete.id);
      showMessage('success', 'Proveedor eliminado correctamente');
      loadData();
      setIsDeleteModalOpen(false);
      setProveedorToDelete(null);
    } catch (error: any) {
      showMessage('error', mensajeError(error));
    }
  };

  const handleToggleStatus = async (proveedor: Proveedor) => {
    try {
      await toggleProveedorStatus(proveedor.id);
      showMessage('success', `Proveedor ${proveedor.activo ? 'desactivado' : 'activado'} correctamente`);
      loadData();
    } catch (error: any) {
      showMessage('error', mensajeError(error));
    }
  };

  const handleRegistrarCompra = (proveedor: Proveedor) => {
    setSelectedProveedor(proveedor);
    setIsCompraModalOpen(true);
  };

  const handleVerCompras = (proveedor: Proveedor) => {
    router.push(`/dashboard/compras?proveedor=${proveedor.id}`);
  };

  // Arrastrar la tabla desde el encabezado, como si fuera una barra de
  // scroll horizontal. Ver hooks/useDragScrollTable.ts.
  const {
    scrollContainerRef,
    tableRef,
    theadRef,
    hasOverflow,
    theadProps,
  } = useDragScrollTable([loading, filteredProveedores]);

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Proveedores' }]} />

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Proveedores</h1>
          <p className="text-gray-600 mt-1">Administra proveedores y compras</p>
        </div>
        
        {/* Filtros y botón */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Búsqueda */}
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar por empresa o NIT..."
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

          {/* Botón Nuevo Proveedor */}
          <button
            onClick={handleCreateProveedor}
            className="flex items-center justify-center gap-2 bg-primary-600 text-white px-6 py-2.5 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap"
          >
            <Building2 size={20} />
            Nuevo Proveedor
          </button>
        </div>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 flex items-start justify-between gap-3 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="flex-shrink-0 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Tabla de Proveedores */}
      {loading ? (
        <div className="bg-white rounded-lg border border-gray-200 flex items-center justify-center py-16 mb-6">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      ) : (
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden mb-6">
        <div className="overflow-x-auto" ref={scrollContainerRef}>
          <table className="min-w-full divide-y divide-gray-200" ref={tableRef}>
            <thead
              ref={theadRef}
              className={`bg-gray-50 ${hasOverflow ? 'cursor-grab select-none' : ''}`}
              {...theadProps}
            >
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Empresa</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">NIT</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Contacto</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Teléfono</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Correo</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredProveedores.map((proveedor) => (
                <tr key={proveedor.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">{proveedor.id}</td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{proveedor.nombreEmpresa}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-mono text-gray-900">{proveedor.nit}</td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">{proveedor.contacto}</td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">{proveedor.telefono}</td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">{proveedor.email || '-'}</td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <button
                      onClick={() => handleToggleStatus(proveedor)}
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold transition-colors ${
                        proveedor.activo 
                          ? 'bg-green-100 text-green-800 hover:bg-green-200' 
                          : 'bg-red-100 text-red-800 hover:bg-red-200'
                      }`}
                    >
                      <Power size={14} />
                      {proveedor.activo ? 'Activo' : 'Inactivo'}
                    </button>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleRegistrarCompra(proveedor)}
                        className="text-primary-600 hover:text-primary-800 transition-colors"
                        title="Registrar compra"
                      >
                        <ShoppingCart size={18} />
                      </button>
                      <button
                        onClick={() => handleVerCompras(proveedor)}
                        className="text-gray-600 hover:text-gray-900 transition-colors"
                        title="Ver compras"
                      >
                        <Package size={18} />
                      </button>
                      <button
                        onClick={() => handleEditProveedor(proveedor)}
                        className="text-primary-600 hover:text-primary-800 transition-colors"
                        title="Editar"
                      >
                        <Edit size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(proveedor)}
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

          {filteredProveedores.length === 0 && (
            <div className="text-center py-12">
              <Building2 size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">No se encontraron proveedores</p>
            </div>
          )}
        </div>
      </div>
      )}

      {/* Modals */}
      {isModalOpen && (
        <ProveedorModal
          proveedor={selectedProveedor}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsModalOpen(false);
            showMessage('success', selectedProveedor ? 'Proveedor actualizado correctamente' : 'Proveedor creado correctamente');
          }}
        />
      )}

      {isCompraModalOpen && selectedProveedor && (
        <CompraModal
          idProveedor={selectedProveedor.id}
          nombreProveedor={selectedProveedor.nombreEmpresa}
          onClose={() => setIsCompraModalOpen(false)}
          onSuccess={() => {
            loadData();
            setIsCompraModalOpen(false);
            showMessage('success', 'Compra registrada correctamente');
          }}
        />
      )}

      {isDeleteModalOpen && proveedorToDelete && (
        <DeleteConfirmModal
          title="Eliminar Proveedor"
          message={`¿Estás seguro de que deseas eliminar a "${proveedorToDelete.nombreEmpresa}"?`}
          onConfirm={handleDeleteConfirm}
          onCancel={() => {
            setIsDeleteModalOpen(false);
            setProveedorToDelete(null);
          }}
        />
      )}

    </div>
  );
}