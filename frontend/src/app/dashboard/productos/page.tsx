'use client';

import { BACKEND_URL } from '@/lib/api';
import { useState, useEffect } from 'react';
import { 
  getAllProductos, 
  deleteProducto, 
  toggleProductoStatus, 
  getActiveCategorias,
  getProductoById,
  addImagenProducto,
  deleteImagenProducto,
  setImagenPrincipal
} from '@/lib/api';
import { Producto, Categoria, TipoProducto, ImagenProducto } from '@/types/producto';
import { Search, Package, Edit, Trash2, Power, Image as ImageIcon, FolderOpen, X } from 'lucide-react';
import Link from 'next/link';
import Breadcrumbs from '@/components/Breadcrumbs';
import ProductoModal from '@/components/ProductoModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';

export default function ProductosPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [filteredProductos, setFilteredProductos] = useState<Producto[]>([]);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
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
  const [categoriaFilter, setCategoriaFilter] = useState<string>('TODOS');
  const [tipoFilter, setTipoFilter] = useState<string>('TODOS');
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');
  
  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedProducto, setSelectedProducto] = useState<Producto | null>(null);
  const [productoToEdit, setProductoToEdit] = useState<Producto | null>(null);
  const [imagenesDelProducto, setImagenesDelProducto] = useState<ImagenProducto[]>([]);
  const [loadingImages, setLoadingImages] = useState(false);
  const [productoToDelete, setProductoToDelete] = useState<Producto | null>(null);
  const [idImagenAEliminar, setIdImagenAEliminar] = useState<number | null>(null);

  // Mensajes
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar productos y categorías
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled: sin categorias el filtro queda vacio, pero la lista de
      // productos se sigue viendo.
      const [rProductos, rCategorias] = await Promise.allSettled([
        getAllProductos(),
        getActiveCategorias()
      ]);

      const { tomar, fallos } = crearRecolector();
      const productosData = tomar(rProductos, 'los productos', [] as Producto[]);
      const categoriasData = tomar(rCategorias, 'las categorias', [] as Categoria[]);

      setProductos(productosData);
      setFilteredProductos(productosData);
      setCategorias(categoriasData);
      setFallosCarga(fallos);
    } catch (error: any) {
      showMessage('error', error.message);
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

    if (tipoFilter !== 'TODOS') {
      filtered = filtered.filter(producto => producto.tipoProducto === tipoFilter);
    }

    if (statusFilter === 'ACTIVOS') {
      filtered = filtered.filter(producto => producto.activo);
    } else if (statusFilter === 'INACTIVOS') {
      filtered = filtered.filter(producto => !producto.activo);
    }

    setFilteredProductos(filtered);
  }, [searchTerm, categoriaFilter, tipoFilter, statusFilter, productos]);

  // Los de éxito se cierran solos; los de error se quedan hasta que el
  // usuario los cierra a mano (el botón X del banner).
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
      console.log("=== INICIANDO EDICIÓN DE PRODUCTO ===");
      console.log("1. Producto seleccionado:", producto);
      
      setSelectedProducto(producto);
      setLoadingImages(true);
      
      console.log("2. Llamando a getProductoById con ID:", producto.id);
      const productoCompleto = await getProductoById(producto.id);
      
      console.log("3. Producto completo recibido:", productoCompleto);
      console.log("4. Imágenes del producto:", productoCompleto.imagenes);
      console.log("5. Cantidad de imágenes:", productoCompleto.imagenes?.length || 0);
      
      setProductoToEdit(productoCompleto);
      
      // ✅ CORREGIR: Asegurar que orden esté definido
      const imagenesConOrden = (productoCompleto.imagenes || []).map((img, index) => ({
        ...img,
        orden: img.orden ?? index
      }));
      setImagenesDelProducto(imagenesConOrden);
      
      console.log("6. Estado actualizado - imagenesDelProducto:", imagenesConOrden);
      console.log("=== ABRIENDO MODAL ===");
      
      setIsModalOpen(true);
    } catch (error: any) {
      console.error("❌ ERROR en handleEditProducto:", error);
      showMessage('error', `Error al cargar producto para editar: ${error.message}`);
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
      showMessage('error', error.message);
    }
  };

  const handleToggleStatus = async (producto: Producto) => {
    try {
      await toggleProductoStatus(producto.id);
      showMessage('success', `Producto ${producto.activo ? 'desactivado' : 'activado'} correctamente`);
      loadData();
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  // ✅ FUNCIONES DE GESTIÓN DE IMÁGENES (sin duplicados)
  const handleAgregarImagen = async (file: File) => {
    console.log("=== AGREGANDO IMAGEN ===");
    console.log("1. Archivo recibido:", file);
    console.log("2. productoToEdit:", productoToEdit);
    
    if (!productoToEdit) {
      console.error("❌ No hay producto para editar");
      return;
    }
    
    setLoadingImages(true);
    try {
      console.log("3. Llamando a addImagenProducto...");
      const nuevaImagen = await addImagenProducto(
        productoToEdit.id, 
        file, 
        imagenesDelProducto.length === 0
      );
      
      console.log("4. Imagen subida exitosamente:", nuevaImagen);
      
      // ✅ ASEGURAR que orden esté definido
      const imagenConOrden = {
        ...nuevaImagen,
        orden: nuevaImagen.orden ?? imagenesDelProducto.length
      };
      
      const nuevasImagenes = [...imagenesDelProducto, imagenConOrden];
      console.log("5. Actualizando estado con nuevas imágenes:", nuevasImagenes);
      
      setImagenesDelProducto(nuevasImagenes);
      showMessage('success', 'Imagen agregada correctamente');
    } catch (error: any) {
      console.error("❌ Error al subir imagen:", error);
      showMessage('error', `Error al subir imagen: ${error.message}`);
    } finally {
      setLoadingImages(false);
    }
  };

  const handleEliminarImagen = async (idImagen: number) => {
    setLoadingImages(true);
    try {
      await deleteImagenProducto(idImagen);
      const nuevasImagenes = imagenesDelProducto.filter(img => img.id !== idImagen);
      setImagenesDelProducto(nuevasImagenes);
      showMessage('success', 'Imagen eliminada correctamente');
    } catch (error: any) {
      console.error("❌ Error al eliminar imagen:", error);
      showMessage('error', `Error al eliminar imagen: ${error.message}`);
    } finally {
      setLoadingImages(false);
      setIdImagenAEliminar(null);
    }
  };

  const handleMarcarComoPrincipal = async (idImagen: number) => {
    console.log("=== MARCANDO COMO PRINCIPAL ===");
    console.log("ID de imagen:", idImagen);
    console.log("productoToEdit:", productoToEdit);
    
    if (!productoToEdit) {
      console.error("❌ No hay producto para editar");
      return;
    }
    
    setLoadingImages(true);
    try {
      console.log("Llamando a setImagenPrincipal...");
      await setImagenPrincipal(idImagen, productoToEdit.id);
      
      const nuevasImagenes = imagenesDelProducto.map(img => ({
        ...img,
        esPrincipal: img.id === idImagen
      }));
      console.log("Imágenes actualizadas:", nuevasImagenes);
      
      setImagenesDelProducto(nuevasImagenes);
      showMessage('success', 'Imagen principal actualizada');
    } catch (error: any) {
      console.error("❌ Error al marcar como principal:", error);
      showMessage('error', `Error al marcar como principal: ${error.message}`);
    } finally {
      setLoadingImages(false);
    }
  };

  const getTipoBadge = (tipo: TipoProducto) => {
    return 'bg-gray-100 text-gray-700 border border-gray-200';
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(price);
  };

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Productos' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
        <div className="flex-shrink-0">
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Productos</h1>
          <p className="text-gray-600 mt-1">Administra el catálogo de camas, colchones y almohadas</p>
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
            value={tipoFilter}
            onChange={(e) => setTipoFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            <option value="TODOS">Todos los tipos</option>
            <option value="CAMA">Camas</option>
            <option value="COLCHON">Colchones</option>
            <option value="ALMOHADA">Almohadas</option>
            <option value="ACCESORIO">Accesorios</option>
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
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Imagen</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">SKU</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Nombre</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Categoría</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Tipo</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Precio</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Stock Mín.</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Estado</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">Acciones</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredProductos.map((producto) => (
                <tr key={producto.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="h-12 w-12 rounded-lg bg-gray-100 flex items-center justify-center overflow-hidden flex-shrink-0">
                      {producto.imagenes && producto.imagenes.length > 0 ? (
                        <img 
                          src={`${BACKEND_URL}${producto.imagenes[0].urlImagen}`} 
                          alt={producto.nombre}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <ImageIcon size={24} className="text-gray-400" />
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-mono text-gray-900">{producto.sku}</td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900 max-w-xs truncate">{producto.nombre}</div>
                    {producto.modelo && (
                      <div className="text-xs text-gray-500 max-w-xs truncate">{producto.modelo}</div>
                    )}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">{producto.nombreCategoria}</td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getTipoBadge(producto.tipoProducto)}`}>
                      {producto.tipoProducto}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">{formatPrice(producto.precioVenta)}</td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600 text-center">{producto.stockMinimo}</td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <button
                      onClick={() => handleToggleStatus(producto)}
                      className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold transition-colors ${
                        producto.activo ? 'bg-green-100 text-green-800 hover:bg-green-200' : 'bg-red-100 text-red-800 hover:bg-red-200'
                      }`}
                    >
                      <Power size={14} />
                      {producto.activo ? 'Activo' : 'Inactivo'}
                    </button>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleEditProducto(producto)}
                        className="text-primary-600 hover:text-primary-800 transition-colors"
                        title="Editar"
                      >
                        <Edit size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(producto)}
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

          {filteredProductos.length === 0 && (
            <div className="text-center py-12">
              <Package size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-500">No se encontraron productos</p>
            </div>
          )}
        </div>
        )}
      </div>

      {/* Modals */}
      {isModalOpen && (
        <ProductoModal
          producto={selectedProducto}
          productoParaEditar={productoToEdit}
          imagenesActuales={imagenesDelProducto}
          onImagenesChange={(nuevasImagenes: ImagenProducto[]) => setImagenesDelProducto(nuevasImagenes)}
          onAgregarImagen={handleAgregarImagen}
          onEliminarImagen={setIdImagenAEliminar}
          onMarcarComoPrincipal={handleMarcarComoPrincipal}
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

      {idImagenAEliminar !== null && (
        <DeleteConfirmModal
          title="Eliminar imagen"
          message="¿Estás seguro de que deseas eliminar esta imagen?"
          onConfirm={() => handleEliminarImagen(idImagenAEliminar)}
          onCancel={() => setIdImagenAEliminar(null)}
        />
      )}
    </div>
  );
}