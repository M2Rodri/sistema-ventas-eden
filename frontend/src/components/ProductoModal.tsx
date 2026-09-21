'use client';

import { useState, useRef, useEffect } from 'react';
import { Producto, ProductoRequest, Categoria, ImagenProducto, MultimediaProducto } from '@/types/producto';
import { createProducto, updateProducto, BACKEND_URL, getMultimediaProducto, subirModelo3D, eliminarMultimedia } from '@/lib/api';
import { X, Image as ImageIcon, Star, Upload, Trash2, Eye } from 'lucide-react';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';

interface ProductoModalProps {
  producto: Producto | null;
  productoParaEditar: Producto | null;
  imagenesActuales: ImagenProducto[];
  onImagenesChange: (imagenes: ImagenProducto[]) => void;
  onAgregarImagen: (file: File) => void;
  onEliminarImagen: (id: number) => void;
  onMarcarComoPrincipal: (id: number) => void;
  loadingImages: boolean;
  categorias: Categoria[];
  onClose: () => void;
  onSuccess: () => void;
}

export default function ProductoModal({ 
  producto, 
  productoParaEditar, 
  imagenesActuales,
  onImagenesChange,
  onAgregarImagen,
  onEliminarImagen,
  onMarcarComoPrincipal,
  loadingImages,
  categorias, 
  onClose, 
  onSuccess 
}: ProductoModalProps) {

  const [formData, setFormData] = useState<ProductoRequest>({
    sku: productoParaEditar?.sku || '',
    nombre: productoParaEditar?.nombre || '',
    descripcion: productoParaEditar?.descripcion || '',
    marca: productoParaEditar?.marca || '',
    modelo: productoParaEditar?.modelo || '',
    idCategoria: productoParaEditar?.idCategoria || categorias[0]?.id || 0,
    calidad: productoParaEditar?.calidad || '',
    costoReferencial: productoParaEditar?.costoReferencial || 0,
    precioVenta: productoParaEditar?.precioVenta || 0,
    peso: productoParaEditar?.peso || 0,
    dimensiones: productoParaEditar?.dimensiones || '',
    stockMinimo: productoParaEditar?.stockMinimo || 0,
    tipoProducto: productoParaEditar?.tipoProducto || 'CAMA',
    activo: productoParaEditar?.activo !== undefined ? productoParaEditar.activo : true,
    
    // ✅ CAMPOS CONDICIONALES
    firmeza: productoParaEditar?.firmeza || undefined,
    materialNucleo: productoParaEditar?.materialNucleo || undefined,
  });

  const [error, setError] = useState<string | null>(null);
  const [showImageModal, setShowImageModal] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const val = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
    
    // ✅ LIMPIAR CAMPOS CONDICIONALES al cambiar tipo
    if (name === 'tipoProducto') {
      setFormData(prev => ({
        ...prev,
        [name]: val as any,
        firmeza: undefined,
        materialNucleo: undefined,
      }));
    } else {
      setFormData(prev => ({ ...prev, [name]: val }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    // ✅ VALIDACIÓN: Limpiar campos condicionales según tipo
    const dataToSend = { ...formData };
    if (formData.tipoProducto !== 'COLCHON') {
      delete dataToSend.firmeza;
      delete dataToSend.materialNucleo;
    }
    
    try {
      if (producto) {
        await updateProducto(producto.id, dataToSend);
      } else {
        await createProducto(dataToSend);
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Error al guardar el producto');
    }
  };

  const isEditing = !!productoParaEditar;

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto">
        <div className="bg-white p-6 rounded-lg shadow-lg w-full max-w-4xl max-h-[90vh] overflow-y-auto m-4">
          <div className="flex justify-between items-start mb-4">
            <h2 className="text-xl font-bold">
              {isEditing ? `Editar Producto: ${productoParaEditar?.nombre}` : 'Crear Nuevo Producto'}
            </h2>
            <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
              <X size={24} />
            </button>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-800 rounded">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">SKU *</label>
                <input type="text" name="sku" value={formData.sku} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700">Nombre *</label>
                <input type="text" name="nombre" value={formData.nombre} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Tipo de Producto *</label>
                <select name="tipoProducto" value={formData.tipoProducto} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white" required>
                  <option value="CAMA">CAMA</option>
                  <option value="COLCHON">COLCHON</option>
                  <option value="ALMOHADA">ALMOHADA</option>
                  <option value="ACCESORIO">ACCESORIO</option>
                </select>
              </div>

              {/* ✅ CAMPOS CONDICIONALES - COLCHON */}
              {formData.tipoProducto === 'COLCHON' && (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">Firmeza</label>
                    <select name="firmeza" value={formData.firmeza || ''} onChange={handleChange}
                      className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white">
                      <option value="">Seleccionar...</option>
                      <option value="Suave">Suave</option>
                      <option value="Medio">Medio</option>
                      <option value="Firme">Firme</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700">Material del Núcleo</label>
                    <select name="materialNucleo" value={formData.materialNucleo || ''} onChange={handleChange}
                      className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white">
                      <option value="">Seleccionar...</option>
                      <option value="Espuma alta densidad">Espuma alta densidad</option>
                      <option value="Viscoelástica">Viscoelástica</option>
                      <option value="Látex">Látex</option>
                      <option value="Resortes ensacados">Resortes ensacados</option>
                      <option value="Fibra siliconada">Fibra siliconada</option>
                    </select>
                  </div>
                </>
              )}

              {/* ✅ CAMPOS CONDICIONALES - CAMA */}

              <div>
                <label className="block text-sm font-medium text-gray-700">Categoría *</label>
                <select name="idCategoria" value={formData.idCategoria} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white" required>
                  {categorias.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.nombre}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Marca</label>
                <input type="text" name="marca" value={formData.marca} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                  placeholder="Ej: Colchones del Oriente" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Modelo</label>
                <input type="text" name="modelo" value={formData.modelo} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Calidad</label>
                <input type="text" name="calidad" value={formData.calidad} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Costo referencial *</label>
                <input type="number" step="0.01" name="costoReferencial" value={formData.costoReferencial} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Precio de Venta *</label>
                <input type="number" step="0.01" name="precioVenta" value={formData.precioVenta} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Peso (kg)</label>
                <input type="number" step="0.01" name="peso" value={formData.peso} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Dimensiones</label>
                <input type="text" name="dimensiones" value={formData.dimensiones} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" placeholder="ej: 200x150x30 cm" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Stock Mínimo *</label>
                <input type="number" name="stockMinimo" value={formData.stockMinimo} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
              </div>

              <div className="flex items-center">
                <input type="checkbox" name="activo" checked={formData.activo} onChange={handleChange}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded" />
                <label className="ml-2 block text-sm text-gray-900">Activo</label>
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700">Descripción</label>
              <textarea name="descripcion" value={formData.descripcion} onChange={handleChange} rows={3}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"></textarea>
            </div>

            {/* ✅ BOTÓN GESTIONAR IMÁGENES */}
            {isEditing && (
              <div className="mb-4">
                <button
                  type="button"
                  onClick={() => setShowImageModal(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700"
                >
                  <ImageIcon size={20} />
                  Gestionar Imágenes ({imagenesActuales.length})
                </button>
              </div>
            )}

            
            {!isEditing && (
              <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <p className="text-sm text-blue-800">
                  ℹ️ <strong>Nota:</strong> Podrás gestionar imágenes y modelos 3D después de crear el producto.
                </p>
              </div>
            )}

            <div className="flex justify-end space-x-2 mt-6">
              <button type="button" onClick={onClose} className="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600">
                Cancelar
              </button>
              <button type="submit" className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600">
                {isEditing ? 'Guardar Cambios' : 'Crear Producto'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* ✅ MODAL GESTIONAR IMÁGENES (P3.6) */}
      {showImageModal && isEditing && (
        <GestionarImagenesModal
          productoId={productoParaEditar!.id}
          productoNombre={productoParaEditar!.nombre}
          imagenesActuales={imagenesActuales}
          onAgregarImagen={onAgregarImagen}
          onEliminarImagen={onEliminarImagen}
          onMarcarComoPrincipal={onMarcarComoPrincipal}
          loadingImages={loadingImages}
          onClose={() => setShowImageModal(false)}
        />
      )}
    </>
  );
}

// ========== COMPONENTE GESTIONAR IMÁGENES ==========

interface GestionarImagenesModalProps {
  productoId: number;
  productoNombre: string;
  imagenesActuales: ImagenProducto[];
  onAgregarImagen: (file: File) => void;
  onEliminarImagen: (id: number) => void;
  onMarcarComoPrincipal: (id: number) => void;
  loadingImages: boolean;
  onClose: () => void;
}

function GestionarImagenesModal({
  productoId,
  productoNombre,
  imagenesActuales,
  onAgregarImagen,
  onEliminarImagen,
  onMarcarComoPrincipal,
  loadingImages,
  onClose
}: GestionarImagenesModalProps) {
  
  // SECCIÓN 1: IMÁGENES 2D
  const [imagenPreview, setImagenPreview] = useState<string | null>(null);
  const [archivoImagenPendiente, setArchivoImagenPendiente] = useState<File | null>(null);
  const fileInputImagenRef = useRef<HTMLInputElement>(null);

  // SECCIÓN 2: MODELO 3D
  const [multimedia, setMultimedia] = useState<MultimediaProducto | null>(null);
  const [confirmarEliminarModelo, setConfirmarEliminarModelo] = useState(false);
  const [loadingModelo, setLoadingModelo] = useState(false);
  const [archivoModeloPendiente, setArchivoModeloPendiente] = useState<File | null>(null);
  const [archivoPreviewPendiente, setArchivoPreviewPendiente] = useState<File | null>(null);
  const [habilitadoRa, setHabilitadoRa] = useState(false);
  const [vistaPrevia3D, setVistaPrevia3D] = useState<string | null>(null);
  const fileInputModeloRef = useRef<HTMLInputElement>(null);
  const fileInputPreviewRef = useRef<HTMLInputElement>(null);
  const [mensaje3D, setMensaje3D] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Cargar multimedia existente al abrir
  useEffect(() => {
    cargarMultimedia();
  }, [productoId]);

  const cargarMultimedia = async () => {
    try {
      const data = await getMultimediaProducto(productoId);
      setMultimedia(data);
      if (data?.habilitadoRa) {
        setHabilitadoRa(true);
      }
    } catch (error) {
      console.error('Error al cargar multimedia:', error);
    }
  };

  // ========== SECCIÓN 1: IMÁGENES 2D ==========

  const handleSeleccionarImagen = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validación tamaño
      if (file.size > 5 * 1024 * 1024) {
        alert('La imagen excede el tamaño máximo permitido de 5MB');
        return;
      }
      // Validación formato
      if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
        alert('Formato no soportado. Por favor seleccione imágenes JPG o PNG');
        return;
      }

      setArchivoImagenPendiente(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagenPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleConfirmarCargaImagen = () => {
    if (archivoImagenPendiente) {
      onAgregarImagen(archivoImagenPendiente);
      setArchivoImagenPendiente(null);
      setImagenPreview(null);
      if (fileInputImagenRef.current) {
        fileInputImagenRef.current.value = '';
      }
    }
  };

  const handleCancelarImagen = () => {
    setArchivoImagenPendiente(null);
    setImagenPreview(null);
    if (fileInputImagenRef.current) {
      fileInputImagenRef.current.value = '';
    }
  };

  // ========== SECCIÓN 2: MODELO 3D ==========

  const handleSeleccionarModelo = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validación tamaño
      if (file.size > 20 * 1024 * 1024) {
        alert('El modelo 3D excede el tamaño máximo permitido de 20MB. Considere optimizar el modelo');
        return;
      }
      // Validación formato
      if (!file.name.toLowerCase().endsWith('.glb')) {
        alert('El archivo GLB no es válido');
        return;
      }

      setArchivoModeloPendiente(file);
      // Crear URL temporal para vista previa
      const url = URL.createObjectURL(file);
      setVistaPrevia3D(url);
    }
  };

  const handleSeleccionarPreview = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setArchivoPreviewPendiente(file);
    }
  };

  const handleConfirmarModelo = async () => {
    if (!archivoModeloPendiente) return;

    setLoadingModelo(true);
    setMensaje3D(null);
    
    try {
      const multimedia = await subirModelo3D(
        productoId,
        archivoModeloPendiente,
        archivoPreviewPendiente,
        habilitadoRa
      );
      
      setMultimedia(multimedia);
      setMensaje3D({
        type: 'success',
        text: 'Modelo 3D cargado. El producto ahora soporta visualización 3D y Realidad Aumentada'
      });
      
      // Limpiar estado
      setArchivoModeloPendiente(null);
      setArchivoPreviewPendiente(null);
      setVistaPrevia3D(null);
      if (fileInputModeloRef.current) fileInputModeloRef.current.value = '';
      if (fileInputPreviewRef.current) fileInputPreviewRef.current.value = '';
      
    } catch (error: any) {
      setMensaje3D({
        type: 'error',
        text: error.message || 'Error al subir modelo 3D'
      });
    } finally {
      setLoadingModelo(false);
    }
  };

  const handleEliminarModelo = async () => {
    if (!multimedia) return;

    setLoadingModelo(true);
    try {
      await eliminarMultimedia(multimedia.id);
      setMultimedia(null);
      setMensaje3D({ type: 'success', text: 'Modelo 3D eliminado correctamente' });
    } catch (error: any) {
      setMensaje3D({ type: 'error', text: error.message });
    } finally {
      setLoadingModelo(false);
      setConfirmarEliminarModelo(false);
    }
  };

  return (
    <>
    <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-[60] p-4">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-6xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b px-6 py-4 flex justify-between items-center z-10">
          <div>
            <h3 className="text-2xl font-bold text-gray-900">Gestionar Imágenes y Modelos 3D</h3>
            <p className="text-sm text-gray-600 mt-1">{productoNombre}</p>
          </div>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
            <X size={28} />
          </button>
        </div>

        <div className="p-6 space-y-8">
          {/* ========== SECCIÓN 1: IMÁGENES 2D ========== */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <h4 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
                📷 Imágenes 2D
              </h4>
              <label className={`px-4 py-2 rounded-md cursor-pointer flex items-center gap-2 ${
                loadingImages ? 'bg-gray-300 text-gray-500' : 'bg-blue-600 text-white hover:bg-blue-700'
              }`}>
                <Upload size={18} />
                <input 
                  ref={fileInputImagenRef}
                  type="file" 
                  accept="image/jpeg,image/jpg,image/png" 
                  onChange={handleSeleccionarImagen}
                  disabled={loadingImages} 
                  className="hidden" 
                />
                Cargar Imágenes
              </label>
            </div>

            {/* Vista Previa antes de confirmar */}
            {imagenPreview && (
              <div className="mb-4 p-4 border-2 border-blue-300 rounded-lg bg-blue-50">
                <p className="text-sm font-medium text-gray-700 mb-2">Vista previa:</p>
                <img src={imagenPreview} alt="Preview" className="w-48 h-48 object-contain mx-auto mb-3 bg-white rounded" />
                <div className="flex gap-2 justify-center">
                  <button
                    onClick={handleConfirmarCargaImagen}
                    disabled={loadingImages}
                    className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
                  >
                    Confirmar Carga
                  </button>
                  <button
                    onClick={handleCancelarImagen}
                    className="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            )}

            {imagenesActuales.length === 0 ? (
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center text-gray-500">
                ⚠️ Debe tener al menos 1 imagen
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                {imagenesActuales.map((img) => (
                  <div key={img.id} className="relative group border rounded-lg overflow-hidden bg-gray-50">
                    <img src={`${BACKEND_URL}${img.urlImagen}`} alt="Producto"
                      className="w-full h-40 object-contain" />
                    <div className="absolute inset-0 bg-black bg-opacity-60 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-2">
                      {!img.esPrincipal && (
                        <button onClick={() => onMarcarComoPrincipal(img.id)}
                          className="flex items-center gap-1 px-3 py-1 bg-yellow-500 text-white rounded hover:bg-yellow-600 text-sm">
                          <Star size={14} /> Principal
                        </button>
                      )}
                      <button onClick={() => onEliminarImagen(img.id)}
                        className="flex items-center gap-1 px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 text-sm">
                        <X size={14} /> Eliminar
                      </button>
                    </div>
                    {img.esPrincipal && (
                      <div className="absolute top-2 left-2 bg-yellow-500 text-white text-xs px-2 py-1 rounded font-semibold flex items-center gap-1">
                        <Star size={12} /> Principal
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* ========== SECCIÓN 2: MODELO 3D ========== */}
          <div className="border-t pt-6">
            <h4 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
              🎭 Modelo 3D (GLB) - Realidad Aumentada
            </h4>

            {mensaje3D && (
              <div className={`mb-4 p-3 rounded ${mensaje3D.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                {mensaje3D.text}
              </div>
            )}

            {/* Modelo existente */}
            {multimedia && !archivoModeloPendiente && (
              <div className="mb-4 p-4 border-2 border-purple-300 rounded-lg bg-purple-50">
                <div className="flex justify-between items-start mb-2">
                  <div>
                    <p className="font-medium text-gray-800">✅ Modelo 3D cargado</p>
                    <p className="text-sm text-gray-600 mt-1">Archivo: {multimedia.urlModelo3d.split('/').pop()}</p>
                    {multimedia.habilitadoRa && (
                      <p className="text-sm text-green-600 font-medium mt-1">🚀 Realidad Aumentada: Habilitada</p>
                    )}
                  </div>
                  <button
                    onClick={() => setConfirmarEliminarModelo(true)}
                    disabled={loadingModelo}
                    className="flex items-center gap-1 px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 text-sm disabled:opacity-50"
                  >
                    <Trash2 size={14} /> Eliminar
                  </button>
                </div>
              </div>
            )}

            {/* Cargar nuevo modelo */}
            {!multimedia || archivoModeloPendiente ? (
              <div className="space-y-4">
                {/* Vista previa 3D */}
                {vistaPrevia3D && (
                  <div className="border-2 border-purple-300 rounded-lg p-4 bg-purple-50">
                    <p className="text-sm font-medium text-gray-700 mb-2">🎨 Visor 3D en tiempo real:</p>
                    <div className="bg-gray-900 rounded-lg p-8 flex items-center justify-center h-64">
                      <p className="text-white text-center">
                        📦 Modelo GLB cargado<br/>
                        <span className="text-sm text-gray-400">{archivoModeloPendiente?.name}</span><br/>
                        <span className="text-xs text-gray-500">Tamaño: {((archivoModeloPendiente?.size || 0) / 1024 / 1024).toFixed(2)} MB</span>
                      </p>
                    </div>
                  </div>
                )}

                {/* Controles de carga */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
<div>
<label className="block text-sm font-medium text-gray-700 mb-2">
Modelo 3D (GLB) *
</label>
<input
                   ref={fileInputModeloRef}
                   type="file"
                   accept=".glb"
                   onChange={handleSeleccionarModelo}
                   className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-purple-50 file:text-purple-700 hover:file:bg-purple-100"
                 />
<p className="text-xs text-gray-500 mt-1">Máximo 20MB</p>
</div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Imagen Preview (opcional)
                </label>
                <input
                  ref={fileInputPreviewRef}
                  type="file"
                  accept="image/*"
                  onChange={handleSeleccionarPreview}
                  className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
                />
              </div>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="habilitadoRa"
                checked={habilitadoRa}
                onChange={(e) => setHabilitadoRa(e.target.checked)}
                className="h-4 w-4 text-purple-600 border-gray-300 rounded"
              />
              <label htmlFor="habilitadoRa" className="text-sm text-gray-700">
                🚀 Habilitar Realidad Aumentada (AR)
              </label>
            </div>

            <button
              onClick={handleConfirmarModelo}
              disabled={!archivoModeloPendiente || loadingModelo}
              className="w-full px-4 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed font-medium"
            >
              {loadingModelo ? 'Subiendo modelo...' : 'Confirmar Modelo'}
            </button>
          </div>
        ) : null}
      </div>
    </div>

    <div className="sticky bottom-0 bg-gray-50 border-t px-6 py-4 flex justify-end">
      <button onClick={onClose} className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700">
        Cerrar
      </button>
    </div>
  </div>
</div>

{confirmarEliminarModelo && (
  <DeleteConfirmModal
    title="Eliminar modelo 3D"
    message="¿Está seguro de eliminar el modelo 3D?"
    onConfirm={handleEliminarModelo}
    onCancel={() => setConfirmarEliminarModelo(false)}
  />
)}
</>
);
}