'use client';

import { urlArchivo } from '@/lib/api';
import { useState, useEffect } from 'react';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';
import {
  getAllProductos,
  deleteProducto,
  toggleProductoStatus,
  getActiveCategorias,
  getProductoById,
  addImagenProducto,
  deleteImagenProducto,
  getAllInventario
} from '@/lib/api';
import { Producto, Categoria, ImagenProducto } from '@/types/producto';
import { Inventario } from '@/types/inventario';
import { Search, Package, Edit, Trash2, Power, Image as ImageIcon, FolderOpen, X } from 'lucide-react';
import Link from 'next/link';
import Breadcrumbs from '@/components/Breadcrumbs';
import ProductoModal from '@/components/ProductoModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';
import { mensajeError } from '@/lib/errores';
import { useAuth } from '@/hooks/useAuth';
import Paginacion from '@/components/Paginacion';

const ITEMS_PER_PAGE = 30;

export default function ProductosPage() {
  const { user } = useAuth();
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [filteredProductos, setFilteredProductos] = useState<Producto[]>([]);
  // Stock actual por producto, tal como vive en Inventario (solo lectura).
  const [stockPorProducto, setStockPorProducto] = useState<Map<number, number>>(new Map());
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
  // Ids de productos cuya foto no cargó; se muestra el placeholder en vez de una imagen rota.
  const [imagenesConError, setImagenesConError] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  // El buscador de la cabecera navega con ?q=<texto>; se lee de window porque useSearchParams exige un Suspense.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const q = params.get('q');
    if (q) setSearchTerm(q);
    const estado = params.get('estado');
    if (estado === 'ACTIVOS' || estado === 'INACTIVOS') setStatusFilter(estado);
  }, []);
  const [categoriaFilter, setCategoriaFilter] = useState<string>('TODOS');
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedProducto, setSelectedProducto] = useState<Producto | null>(null);
  const [productoToEdit, setProductoToEdit] = useState<Producto | null>(null);
  const [imagenesDelProducto, setImagenesDelProducto] = useState<ImagenProducto[]>([]);
  const [loadingImages, setLoadingImages] = useState(false);
  const [productoToDelete, setProductoToDelete] = useState<Producto | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar productos y categorías
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled: sin categorías o inventario igual se ve la lista de productos.
      const [rProductos, rCategorias, rInventario] = await Promise.allSettled([
        getAllProductos(),
        getActiveCategorias(),
        getAllInventario()
      ]);

      const { tomar, fallos } = crearRecolector();
      const productosData = tomar(rProductos, 'los productos', [] as Producto[]);
      const categoriasData = tomar(rCategorias, 'las categorias', [] as Categoria[]);
      const inventarioData = tomar(rInventario, 'el stock de inventario', [] as Inventario[]);

      // Lo más nuevo arriba (por ID, de mayor a menor), como Ventas.
      setProductos([...productosData].sort((a, b) => Number(b.id) - Number(a.id)));
      setFilteredProductos(productosData);
      setCategorias(categoriasData);
      setStockPorProducto(new Map(inventarioData.map((i) => [i.idProducto, i.cantidadDisponible])));
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', mensajeError(error));
    } finally {
      setLoading(false);
    }
  };

  // Filtrar productos
  useEffect(() => {
    let filtered = productos;

    if (searchTerm) {
      filtered = filtered.filter(producto =>
        producto.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
        producto.sku.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    if (categoriaFilter !== 'TODOS') {
      filtered = filtered.filter(producto => producto.idCategoria === parseInt(categoriaFilter));
    }

    if (statusFilter === 'ACTIVOS') {
      filtered = filtered.filter(producto => producto.activo);
    } else if (statusFilter === 'INACTIVOS') {
      filtered = filtered.filter(producto => !producto.activo);
    }

    setCurrentPage(1);
    setFilteredProductos(filtered);
  }, [searchTerm, categoriaFilter, statusFilter, productos]);

  // Los avisos de éxito se cierran solos; los de error esperan a que el usuario los cierre.
  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    if (type === 'success') {
      setTimeout(() => setMessage(null), 4000);
    }
  };

  const handleCreateProducto = () => {
    setSelectedProducto(null);
    setProductoToEdit(null);
    setImagenesDelProducto([]);
    setIsModalOpen(true);
  };

  const handleEditProducto = async (producto: Producto) => {
    try {
      setSelectedProducto(producto);
      setLoadingImages(true);
      const productoCompleto = await getProductoById(producto.id);

      setProductoToEdit(productoCompleto);

      const imagenesConOrden = (productoCompleto.imagenes || []).map((img, index) => ({
        ...img,
        orden: img.orden ?? index
      }));
      setImagenesDelProducto(imagenesConOrden);

      setIsModalOpen(true);
    } catch (error: any) {
      console.error("❌ ERROR en handleEditProducto:", error);
      showMessage('error', mensajeError(error, 'No se pudo cargar el producto para editar.'));
    } finally {
      setLoadingImages(false);
    }
  };

  const handleDeleteClick = (producto: Producto) => {
    setProductoToDelete(producto);
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!productoToDelete) return;

    try {
      await deleteProducto(productoToDelete.id);
      showMessage('success', 'Producto eliminado correctamente');
      loadData();
      setIsDeleteModalOpen(false);
      setProductoToDelete(null);
    } catch (error: any) {
      showMessage('error', mensajeError(error));
    }
  };

  const handleToggleStatus = async (producto: Producto) => {
    try {
      await toggleProductoStatus(producto.id);
      showMessage('success', `Producto ${producto.activo ? 'desactivado' : 'activado'} correctamente`);
      loadData();
    } catch (error: any) {
      showMessage('error', mensajeError(error));
    }
  };

  // Un producto tiene a lo sumo una imagen: siempre se sube como principal.
  const handleAgregarImagen = async (idProducto: number, file: File) => {
    setLoadingImages(true);
    try {
      const nuevaImagen = await addImagenProducto(idProducto, file, true);
      setImagenesDelProducto([{ ...nuevaImagen, orden: 0 }]);
      showMessage('success', 'Imagen agregada correctamente');
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudo subir la imagen.'));
    } finally {
      setLoadingImages(false);
    }
  };

  // Cambiar la imagen: se borra la anterior y se sube la nueva en un solo paso.
  const handleReemplazarImagen = async (idImagenVieja: number, idProducto: number, file: File) => {
    setLoadingImages(true);
    try {
      await deleteImagenProducto(idImagenVieja);
      const nuevaImagen = await addImagenProducto(idProducto, file, true);
      setImagenesDelProducto([{ ...nuevaImagen, orden: 0 }]);
      showMessage('success', 'Imagen actualizada correctamente');
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudo actualizar la imagen.'));
    } finally {
      setLoadingImages(false);
    }
  };

  // La confirmación ya la resuelve ProductoModal; esto borra directo.
  const handleEliminarImagen = async (idImagen: number) => {
    setLoadingImages(true);
    try {
      await deleteImagenProducto(idImagen);
      setImagenesDelProducto([]);
      showMessage('success', 'Imagen eliminada correctamente');
    } catch (error: any) {
      showMessage('error', mensajeError(error, 'No se pudo eliminar la imagen.'));
    } finally {
      setLoadingImages(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(price);
  };

  // Arrastrar la tabla desde el encabezado (ver hooks/useDragScrollTable.ts).
  const {
    scrollContainerRef,
    tableRef,
    theadRef,
    hasOverflow,
    theadProps,
  } = useDragScrollTable([loading, filteredProductos]);

  // Paginación: se calcula a partir de filteredProductos
  const totalPages = Math.ceil(filteredProductos.length / ITEMS_PER_PAGE);
  const paginatedProductos = filteredProductos.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE,
  );
  // Rangos reales de ID mostrados en la página (para lista ordenada desc o asc)
  const startId = paginatedProductos.length > 0 ? paginatedProductos[0].id : undefined;
  const endId = paginatedProductos.length > 0 ? paginatedProductos[paginatedProductos.length - 1].id : undefined;

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Productos' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Productos</h1>
          <p className="text-gray-600 mt-1">Administra el catálogo de productos</p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="Buscar por nombre o SKU..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <select
            value={categoriaFilter}
            onChange={(e) => setCategoriaFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todas las categorías</option>
            {categorias.map((cat) => (
              <option key={cat.id} value={cat.id}>{cat.nombre}</option>
            ))}
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los estados</option>
            <option value="ACTIVOS">Activos</option>
            <option value="INACTIVOS">Inactivos</option>
          </select>

          <Link
            href="/dashboard/categorias"
            className="flex items-center justify-center gap-2 border border-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors font-medium whitespace-nowrap"
          >
            <FolderOpen size={20} />
            Categorías
          </Link>

          <button
            onClick={handleCreateProducto}
            className="flex items-center justify-center gap-2 bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium whitespace-nowrap"
          >
            <Package size={20} />
            Nuevo Producto
          </button>
        </div>
      </div>

      {/* Mensaje */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 flex items-start justify-between gap-3 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="flex-shrink-0 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Tabla */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto" ref={scrollContainerRef}>
          <table className="w-full table-fixed min-w-[750px] divide-y divide-gray-200" ref={tableRef}>
            <thead
              ref={theadRef}
              className={`bg-gray-50 ${hasOverflow ? 'cursor-grab select-none' : ''}`}
              {...theadProps}
            >
              <tr>
                <th className="w-[5%] px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">ID</th>
                <th className="w-[9%] px-2 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">SKU</th>
                <th className="w-[7%] px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Imagen</th>
                <th className="w-[25%] px-2.5 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Nombre</th>
                <th className="w-[12%] px-2 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Categoría</th>
                <th className="w-[14%] px-2 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                  <span>PRECIO DE</span>
                  <span className="block">VENTA</span>
                </th>
                <th className="w-[8%] px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider leading-tight">
                  <span>STOCK</span>
                  <span className="block">ACTUAL</span>
                </th>
                <th className="w-[11%] px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                <th className="w-[9%] px-1 py-2 text-center text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {paginatedProductos.map((producto, index) => (
                <tr key={producto.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-1 py-[7px] whitespace-nowrap text-sm text-center text-gray-500 font-mono">{producto.id}</td>
                  <td className="px-2 py-[7px] whitespace-nowrap text-sm font-mono text-gray-700 truncate" title={producto.sku}>{producto.sku}</td>
                  <td className="px-1 py-1 whitespace-nowrap text-center">
                    <div className="w-11 h-[35px] rounded-md bg-gray-100 flex items-center justify-center overflow-hidden flex-shrink-0 mx-auto border border-gray-200 shadow-sm">
                      {producto.imagenes && producto.imagenes.length > 0 && !imagenesConError.has(producto.id) ? (
                        <img
                          src={urlArchivo(producto.imagenes[0].urlImagen)}
                          alt={producto.nombre}
                          className="h-full w-full object-cover"
                          onError={() =>
                            setImagenesConError((prev) => new Set(prev).add(producto.id))
                          }
                        />
                      ) : (
                        <ImageIcon size={16} className="text-gray-400" />
                      )}
                    </div>
                  </td>
                  <td className="px-2.5 py-[7px]">
                    <div className="text-sm font-medium text-gray-900 truncate" title={producto.nombre}>{producto.nombre}</div>
                    {producto.modelo && (
                      <div className="text-xs text-gray-400 truncate" title={producto.modelo}>{producto.modelo}</div>
                    )}
                  </td>
                  <td className="px-2 py-[7px] truncate text-sm text-gray-600" title={producto.nombreCategoria}>{producto.nombreCategoria}</td>
                  <td className="px-2 py-[7px] whitespace-nowrap text-sm font-semibold text-gray-900 text-right">{formatPrice(producto.precioVenta)}</td>
                  <td className="px-1 py-[7px] whitespace-nowrap text-sm text-gray-700 font-mono text-center">
                    {stockPorProducto.get(producto.id) ?? '—'}
                  </td>
                  <td className="px-1 py-[7px] whitespace-nowrap text-center">
                    {user?.role === 'ADMIN' ? (
                      <button
                        onClick={() => handleToggleStatus(producto)}
                        className={`inline-flex items-center justify-center gap-1 px-2.5 py-[3px] rounded-full text-xs font-semibold transition-colors ${
                          producto.activo ? 'bg-green-100 text-green-800 hover:bg-green-200' : 'bg-red-100 text-red-800 hover:bg-red-200'
                        }`}
                      >
                        <Power size={13} />
                        {producto.activo ? 'Activo' : 'Inactivo'}
                      </button>
                    ) : (
                      <span
                        className={`inline-flex items-center justify-center gap-1 px-2.5 py-[3px] rounded-full text-xs font-semibold w-fit ${
                          producto.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}
                      >
                        <Power size={13} />
                        {producto.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    )}
                  </td>
                  <td className="px-1 py-[7px] whitespace-nowrap text-sm font-medium text-center">
                    <div className="inline-flex items-center justify-center gap-1">
                      <button
                        onClick={() => handleEditProducto(producto)}
                        disabled={user?.role !== 'ADMIN'}
                        className="p-1 text-primary-600 hover:bg-primary-50 rounded transition-colors disabled:text-gray-300 disabled:hover:bg-transparent disabled:cursor-not-allowed"
                        title={user?.role === 'ADMIN' ? 'Editar' : 'Solo el administrador puede editar'}
                      >
                        <Edit size={16} />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(producto)}
                        disabled={user?.role !== 'ADMIN'}
                        className="p-1 text-red-600 hover:bg-red-50 rounded transition-colors disabled:text-gray-300 disabled:hover:bg-transparent disabled:cursor-not-allowed"
                        title={user?.role === 'ADMIN' ? 'Eliminar' : 'Solo el administrador puede eliminar'}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

              {filteredProductos.length === 0 && (
                <div className="text-center py-12">
                  <Package size={48} className="mx-auto text-gray-400 mb-4" />
                  <p className="text-gray-500">No se encontraron productos</p>
                </div>
              )}
            </div>

            {/* Paginación */}
            <Paginacion
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={filteredProductos.length}
              itemsPerPage={ITEMS_PER_PAGE}
              onPageChange={setCurrentPage}
              startItem={startId}
              endItem={endId}
            />
          </>
        )}
      </div>

      {/* Modals */}
      {isModalOpen && (
        <ProductoModal
          producto={selectedProducto}
          productoParaEditar={productoToEdit}
          stockActual={productoToEdit ? stockPorProducto.get(productoToEdit.id) : undefined}
          productos={productos}
          imagenesActuales={imagenesDelProducto}
          onAgregarImagen={handleAgregarImagen}
          onReemplazarImagen={handleReemplazarImagen}
          onEliminarImagen={handleEliminarImagen}
          loadingImages={loadingImages}
          categorias={categorias}
          onClose={() => {
            setIsModalOpen(false);
            setProductoToEdit(null);
            setImagenesDelProducto([]);
          }}
          onSuccess={() => {
            loadData();
            setIsModalOpen(false);
            setProductoToEdit(null);
            setImagenesDelProducto([]);
            showMessage('success', selectedProducto ? 'Producto actualizado correctamente' : 'Producto creado correctamente');
          }}
        />
      )}

      {isDeleteModalOpen && productoToDelete && (
        <DeleteConfirmModal
          title="Eliminar Producto"
          message={`¿Estás seguro de que deseas eliminar "${productoToDelete.nombre}"?`}
          onConfirm={handleDeleteConfirm}
          onCancel={() => {
            setIsDeleteModalOpen(false);
            setProductoToDelete(null);
          }}
        />
      )}
    </div>
  );
}