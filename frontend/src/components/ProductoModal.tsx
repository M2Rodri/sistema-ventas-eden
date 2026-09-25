'use client';

import { useState } from 'react';
import { Producto, ProductoRequest, Categoria, ImagenProducto, TipoProducto } from '@/types/producto';
import { createProducto, updateProducto, BACKEND_URL } from '@/lib/api';
import { X, Upload, Trash2 } from 'lucide-react';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';

/** Medidas estándar de cama/colchón en Bolivia. */
const MEDIDAS_ESTANDAR = [
  { value: '1_PLAZA', label: '1 Plaza', ancho: 90, largo: 190 },
  { value: '1_PLAZA_MEDIA', label: '1 Plaza y Media', ancho: 105, largo: 190 },
  { value: '2_PLAZAS', label: '2 Plazas', ancho: 140, largo: 190 },
  { value: 'QUEEN', label: 'Queen Size', ancho: 160, largo: 200 },
  { value: 'KING', label: 'King Size', ancho: 180, largo: 200 },
] as const;
const MEDIDA_OTRA = 'OTRA';

/** Mismo prefijo de 3 letras que ya se usaba a mano en los SKU cargados
 * antes (ALM-001, CAM-002, COL-001...). Sale del Tipo de Producto, no del
 * nombre de la categoría, para que funcione igual sin importar cómo se
 * llame la categoría. */
const PREFIJOS_SKU: Record<TipoProducto, string> = {
  CAMA: 'CAM',
  COLCHON: 'COL',
  ALMOHADA: 'ALM',
  ACCESORIO: 'ACC',
};

/**
 * Propone el siguiente número de la serie mirando TODOS los productos que
 * ya existen (incluidos los dados de baja: siguen en la base y su SKU
 * sigue siendo suyo, así que no hay que repetirlo). No hay endpoint nuevo:
 * usa la misma lista que ya está cargada en la pantalla de Productos.
 */
function sugerirSiguienteSku(tipo: TipoProducto, productos: Producto[]): string {
  const prefijo = PREFIJOS_SKU[tipo];
  const regex = new RegExp(`^${prefijo}-(\\d+)$`);
  let maxNumero = 0;
  for (const p of productos) {
    const match = p.sku.match(regex);
    if (match) {
      const numero = parseInt(match[1], 10);
      if (numero > maxNumero) maxNumero = numero;
    }
  }
  return `${prefijo}-${String(maxNumero + 1).padStart(3, '0')}`;
}

/** Fecha + hora tal cual las guarda la base (fecha_creacion/fecha_actualizacion
 * son timestamp, no date: incluyen hora aunque no se mostraran hasta ahora). */
function formatearFechaHora(fechaIso: string): string {
  return new Date(fechaIso).toLocaleString('es-BO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

/**
 * "Dimensiones" se guarda como un solo texto (no se agregaron columnas
 * nuevas: nada en el sistema necesita filtrar por ancho/largo todavía).
 * Este texto lo arma siempre este mismo componente con un formato fijo
 * ("Etiqueta (AxLxA cm)" o "Medida especial (AxLxA cm)"), así que se puede
 * reconocer con certeza al reabrir el formulario. Lo que no venga en ese
 * formato es dato de antes del selector: se deja sin reconocer en vez de
 * arriesgar a interpretarlo mal (el orden de los números en los datos
 * viejos no era consistente).
 */
function parseDimensiones(texto?: string) {
  if (!texto) return null;
  const estandar = MEDIDAS_ESTANDAR.find((m) => texto.startsWith(`${m.label} (`));
  const esOtra = !estandar && texto.startsWith('Medida especial (');
  if (!estandar && !esOtra) return null;
  const match = texto.match(/\((\d+)x(\d+)(?:x(\d+))?\s*cm\)/);
  if (!match) return null;
  return {
    medida: estandar ? estandar.value : MEDIDA_OTRA,
    ancho: match[1],
    largo: match[2],
    alto: match[3] ?? '',
  };
}

function componerDimensiones(medidaSeleccionada: string, ancho: string, largo: string, alto: string): string {
  if (!ancho || !largo) return '';
  const estandar = MEDIDAS_ESTANDAR.find((m) => m.value === medidaSeleccionada);
  const etiqueta = estandar ? estandar.label : 'Medida especial';
  const medidas = `${ancho}x${largo}${alto ? 'x' + alto : ''} cm`;
  return `${etiqueta} (${medidas})`;
}

interface ProductoModalProps {
  producto: Producto | null;
  productoParaEditar: Producto | null;
  /** Cantidad real de Inventario. Solo para mostrar (de solo lectura acá);
   * cargarla se hace desde Inventario o recibiendo una Compra, no desde
   * este formulario. undefined mientras se está creando (todavía no existe). */
  stockActual?: number;
  /** Solo se usa la primera (o ninguna): un producto tiene a lo sumo una imagen. */
  imagenesActuales: ImagenProducto[];
  onAgregarImagen: (idProducto: number, file: File) => Promise<void>;
  onReemplazarImagen: (idImagenVieja: number, idProducto: number, file: File) => Promise<void>;
  /** Confirmación ya resuelta acá adentro: esto borra directo. */
  onEliminarImagen: (id: number) => Promise<void>;
  loadingImages: boolean;
  categorias: Categoria[];
  /** Todos los productos (activos e inactivos): se usa nomás para calcular
   * el siguiente SKU de la serie, mirando los que ya existen. */
  productos: Producto[];
  onClose: () => void;
  onSuccess: () => void;
}

export default function ProductoModal({
  producto,
  productoParaEditar,
  stockActual,
  imagenesActuales,
  onAgregarImagen,
  onReemplazarImagen,
  onEliminarImagen,
  loadingImages,
  categorias,
  productos,
  onClose,
  onSuccess
}: ProductoModalProps) {

  // El Tipo de Producto ya no se elige a mano: lo hereda de la Categoría
  // elegida (cada categoría tiene su tipo fijo, ver CategoriaModal). En este
  // negocio categoría y tipo eran el mismo dato pedido dos veces.
  const idCategoriaInicial = productoParaEditar?.idCategoria || categorias[0]?.id || 0;
  const tipoDeCategoria = (idCategoria: number) =>
    categorias.find((c) => c.id === idCategoria)?.tipoProducto;
  const tipoInicial = tipoDeCategoria(idCategoriaInicial) || productoParaEditar?.tipoProducto || 'CAMA';
  // El SKU se sugiere solo al crear, y solo mientras el usuario no haya
  // escrito el suyo a mano (acá arranca en true porque al editar el campo
  // ya trae el SKU real, no hay nada que sugerir).
  const [skuTocado, setSkuTocado] = useState(!!productoParaEditar);

  const [formData, setFormData] = useState<ProductoRequest>({
    sku: productoParaEditar?.sku || sugerirSiguienteSku(tipoInicial, productos),
    nombre: productoParaEditar?.nombre || '',
    descripcion: productoParaEditar?.descripcion || '',
    marca: productoParaEditar?.marca || '',
    modelo: productoParaEditar?.modelo || '',
    idCategoria: idCategoriaInicial,
    calidad: productoParaEditar?.calidad || '',
    precioCompra: productoParaEditar?.precioCompra,
    precioVenta: productoParaEditar?.precioVenta || 0,
    stockMinimo: productoParaEditar?.stockMinimo || 0,
    tipoProducto: tipoInicial,
    activo: productoParaEditar?.activo !== undefined ? productoParaEditar.activo : true,

    // ✅ CAMPOS CONDICIONALES
    firmeza: productoParaEditar?.firmeza || undefined,
    materialNucleo: productoParaEditar?.materialNucleo || undefined,
    materialArmazon: productoParaEditar?.materialArmazon || undefined,

    // Color no es condicional: aplica a cualquier tipo de producto.
    color: productoParaEditar?.color || '',
  });

  const [error, setError] = useState<string | null>(null);

  // Marca/Modelo/Calidad/Descripción son secundarios: plegado por defecto
  // para un producto nuevo, pero si ya traían algo cargado (al editar) no
  // conviene esconderlo sin que se note.
  const [mostrarMasDetalles, setMostrarMasDetalles] = useState(
    !!(productoParaEditar?.marca || productoParaEditar?.modelo ||
       productoParaEditar?.calidad || productoParaEditar?.descripcion)
  );

  // Dimensiones: se arma a partir de estos tres, no es un input directo.
  // Si el producto ya tenía un texto con el formato nuevo (generado por
  // este mismo componente), se reconstruye; si es dato de antes del
  // selector, queda sin reconocer (dimensionesSinReconocer) y hay que
  // volver a cargarlo a mano.
  const dimensionesParseadas = parseDimensiones(productoParaEditar?.dimensiones);
  const dimensionesSinReconocer =
    productoParaEditar?.dimensiones && !dimensionesParseadas ? productoParaEditar.dimensiones : null;
  const [medidaSeleccionada, setMedidaSeleccionada] = useState(dimensionesParseadas?.medida ?? '');
  const [ancho, setAncho] = useState(dimensionesParseadas?.ancho ?? '');
  const [largo, setLargo] = useState(dimensionesParseadas?.largo ?? '');
  const [altoGrosor, setAltoGrosor] = useState(dimensionesParseadas?.alto ?? '');

  // Imagen del producto: elegir/arrastrar un archivo, cambiarlo o quitarlo
  // solo queda en memoria acá. Nada de esto pega contra el backend hasta
  // que se aprieta "Crear Producto" / "Guardar Cambios" — igual que
  // cualquier otro campo del formulario. Si se cierra con Cancelar o la X,
  // no quedó nada guardado.
  const [archivoImagenPendiente, setArchivoImagenPendiente] = useState<File | null>(null);
  const [previewImagenPendiente, setPreviewImagenPendiente] = useState<string | null>(null);
  const [quitarImagenAlGuardar, setQuitarImagenAlGuardar] = useState(false);
  const [mostrarVistaGrande, setMostrarVistaGrande] = useState(false);
  // "Cambiar imagen" no abre el explorador directo: primero vuelve a
  // mostrar el recuadro para arrastrar/elegir, igual que cuando no hay
  // ninguna imagen todavía.
  const [reemplazandoImagen, setReemplazandoImagen] = useState(false);
  // Reemplazar o quitar una imagen que ya existe se confirma antes (aunque
  // el cambio real recién se aplique al guardar). Elegir una imagen por
  // primera vez no hace falta confirmarlo.
  const [confirmacionImagen, setConfirmacionImagen] = useState<'cambiar' | 'quitar' | null>(null);
  const imagenActual = imagenesActuales[0] ?? null;
  const urlImagenMostrada = previewImagenPendiente
    ?? (quitarImagenAlGuardar || !imagenActual ? null : `${BACKEND_URL}${imagenActual.urlImagen}`);
  const mostrarRecuadroVacio = !urlImagenMostrada || reemplazandoImagen;
  // Dimensiones solo tiene sentido para Cama y Colchón: una almohada o un
  // accesorio no se describen por su medida de la misma forma.
  const tipoTieneDimensiones = formData.tipoProducto === 'CAMA' || formData.tipoProducto === 'COLCHON';
  // Se bloquean Ancho/Largo cuando la medida elegida es una estándar: si
  // hiciera falta cambiarlos habría que pasar a "Medida Especial / Otra".
  const medidaEsEstandar = medidaSeleccionada !== '' && medidaSeleccionada !== MEDIDA_OTRA;

  // Ejemplos de los placeholders según el tipo elegido: no tiene sentido
  // sugerir "Colchones del Oriente" como ejemplo de Marca cuando la
  // categoría es Cama.
  const EJEMPLOS_MARCA: Record<string, string> = {
    CAMA: 'Ej: Muebles del Oriente',
    COLCHON: 'Ej: Colchones del Oriente',
    ALMOHADA: 'Ej: Textiles del Hogar',
    ACCESORIO: 'Ej: Hogar y Confort',
  };
  const EJEMPLOS_COLOR: Record<string, string> = {
    CAMA: 'Ej: Nogal, Blanco, Wengue',
    COLCHON: 'Ej: Blanco, Beige, Gris',
    ALMOHADA: 'Ej: Blanco, Beige',
    ACCESORIO: 'Ej: Blanco, Negro',
  };
  const placeholderMarca = EJEMPLOS_MARCA[formData.tipoProducto] ?? 'Ej: Muebles del Oriente';
  const placeholderColor = EJEMPLOS_COLOR[formData.tipoProducto] ?? 'Ej: Blanco, Negro';

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const val = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;

    if (name === 'sku') {
      setSkuTocado(true);
    }

    // La categoría trae su tipo de producto pegado: cambiarla puede cambiar
    // el tipo también, así que se limpian los campos condicionales igual
    // que antes se hacía al cambiar el Tipo a mano. También recalcula la
    // sugerencia de SKU, pero solo si el usuario todavía no escribió el
    // suyo a mano.
    if (name === 'precioCompra') {
      // Opcional: un campo vacío tiene que quedar en undefined, no en 0 ni
      // en un string vacío, para que el backend lo reciba como "sin dato".
      setFormData(prev => ({ ...prev, precioCompra: value === '' ? undefined : Number(value) }));
    } else if (name === 'idCategoria') {
      const nuevoTipo = tipoDeCategoria(Number(val)) || formData.tipoProducto;
      setFormData(prev => ({
        ...prev,
        idCategoria: Number(val),
        tipoProducto: nuevoTipo,
        sku: skuTocado ? prev.sku : sugerirSiguienteSku(nuevoTipo, productos),
        firmeza: undefined,
        materialNucleo: undefined,
        materialArmazon: undefined,
      }));
    } else {
      setFormData(prev => ({ ...prev, [name]: val }));
    }
  };

  const handleSeleccionarMedida = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    setMedidaSeleccionada(value);
    const estandar = MEDIDAS_ESTANDAR.find((m) => m.value === value);
    if (estandar) {
      setAncho(String(estandar.ancho));
      setLargo(String(estandar.largo));
    } else {
      setAncho('');
      setLargo('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (tipoTieneDimensiones && (!ancho || !largo)) {
      setError('Elegí la medida del producto: Ancho y Largo son obligatorios para Cama y Colchón.');
      return;
    }

    // ✅ VALIDACIÓN: Limpiar campos condicionales según tipo
    const dataToSend = { ...formData };
    if (formData.tipoProducto !== 'COLCHON') {
      delete dataToSend.firmeza;
      delete dataToSend.materialNucleo;
    }
    if (formData.tipoProducto !== 'CAMA') {
      delete dataToSend.materialArmazon;
    }
    dataToSend.dimensiones = tipoTieneDimensiones
      ? componerDimensiones(medidaSeleccionada, ancho, largo, altoGrosor)
      : '';

    try {
      if (producto) {
        await updateProducto(producto.id, dataToSend);
        // Recién acá se confirma el guardado: es el momento de aplicar lo
        // que haya quedado pendiente sobre la imagen (nueva, reemplazo o
        // quitar). Si no se tocó nada, ninguna de las tres corre.
        if (archivoImagenPendiente) {
          if (imagenActual) {
            await onReemplazarImagen(imagenActual.id, producto.id, archivoImagenPendiente);
          } else {
            await onAgregarImagen(producto.id, archivoImagenPendiente);
          }
        } else if (quitarImagenAlGuardar && imagenActual) {
          await onEliminarImagen(imagenActual.id);
        }
      } else {
        const nuevo = await createProducto(dataToSend);
        // Recién acá el producto tiene id: es el primer momento en que se
        // puede subir la imagen que se eligió en el formulario.
        if (archivoImagenPendiente) {
          await onAgregarImagen(nuevo.id, archivoImagenPendiente);
        }
      }
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Error al guardar el producto');
    }
  };

  const isEditing = !!productoParaEditar;

  // Valida el archivo y lo deja en memoria, sin importar si vino de
  // elegirlo con clic o de soltarlo arrastrado: mismo camino para los dos.
  // No pega contra el backend — eso lo resuelve handleSubmit al guardar.
  const procesarArchivoImagen = (file: File) => {
    if (file.size > 5 * 1024 * 1024) {
      setError('La imagen excede el tamaño máximo permitido de 5MB');
      return;
    }
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
      setError('Formato no soportado. Usá una imagen JPG o PNG');
      return;
    }
    setError(null);
    setQuitarImagenAlGuardar(false);
    setArchivoImagenPendiente(file);
    setPreviewImagenPendiente(URL.createObjectURL(file));
    setReemplazandoImagen(false);
  };

  const handleSeleccionarImagen = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) procesarArchivoImagen(file);
    e.target.value = '';
  };

  const [arrastrandoImagen, setArrastrandoImagen] = useState(false);

  const handleDragOverImagen = (e: React.DragEvent<HTMLElement>) => {
    e.preventDefault();
    if (!loadingImages) setArrastrandoImagen(true);
  };

  const handleDragLeaveImagen = (e: React.DragEvent<HTMLElement>) => {
    e.preventDefault();
    setArrastrandoImagen(false);
  };

  const handleDropImagen = (e: React.DragEvent<HTMLElement>) => {
    e.preventDefault();
    setArrastrandoImagen(false);
    if (loadingImages) return;
    const file = e.dataTransfer.files?.[0];
    if (file) procesarArchivoImagen(file);
  };

  // Si hay una imagen ya guardada de por medio, se confirma antes de
  // tocarla (aunque el borrado real recién pasa al guardar el formulario).
  // Si es solo una selección que todavía no se guardó, se limpia nomás.
  const handleQuitarImagen = () => {
    if (imagenActual) {
      setConfirmacionImagen('quitar');
      return;
    }
    if (previewImagenPendiente) URL.revokeObjectURL(previewImagenPendiente);
    setArchivoImagenPendiente(null);
    setPreviewImagenPendiente(null);
  };

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto">
        <div className="bg-white rounded-lg shadow-lg w-full max-w-4xl max-h-[90vh] overflow-y-auto m-4">
          <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                {isEditing ? `Editar Producto: ${productoParaEditar?.nombre}` : 'Crear Nuevo Producto'}
              </h2>
              <p className="text-sm text-gray-600 mt-1">
                {isEditing ? 'Los cambios se aplican de inmediato' : 'Completá los datos para agregarlo al catálogo'}
              </p>
            </div>
            <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
              <X size={24} />
            </button>
          </div>

          <div className="p-6">

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
                {!isEditing && (
                  <p className="text-xs text-gray-500 mt-1">Sugerido según la categoría; lo podés cambiar.</p>
                )}
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700">Nombre *</label>
                <input type="text" name="nombre" value={formData.nombre} onChange={handleChange}
                  list="nombres-productos-existentes"
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
                {/* Sugiere nombres ya usados para evitar duplicados casi
                    iguales ("Cama Nido" vs "Cama nido"), pero no obliga:
                    el campo sigue aceptando cualquier texto libre. */}
                <datalist id="nombres-productos-existentes">
                  {Array.from(new Set(productos.map(p => p.nombre))).map(nombre => (
                    <option key={nombre} value={nombre} />
                  ))}
                </datalist>
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
              {formData.tipoProducto === 'CAMA' && (
                <div>
                  <label className="block text-sm font-medium text-gray-700">Material del Armazón</label>
                  <select name="materialArmazon" value={formData.materialArmazon || ''} onChange={handleChange}
                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white">
                    <option value="">Seleccionar...</option>
                    <option value="Madera">Madera</option>
                    <option value="Fierro">Fierro</option>
                    <option value="Combinado">Combinado (madera y metal)</option>
                  </select>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-900">Categoría *</label>
                <select name="idCategoria" value={formData.idCategoria} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white" required>
                  {categorias.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.nombre}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Color</label>
                <input type="text" name="color" value={formData.color || ''} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                  placeholder={placeholderColor} />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Precio de Compra</label>
                {productoParaEditar?.tieneComprasConfirmadas ? (
                  <p className="mt-1 px-2 py-2 text-gray-700">
                    {formData.precioCompra != null
                      ? `Bs ${Number(formData.precioCompra).toLocaleString('es-BO', { minimumFractionDigits: 2 })}`
                      : '—'}
                    <span className="text-xs text-gray-500 ml-2">Ya tiene compras confirmadas: se administra desde Compras, no aquí.</span>
                  </p>
                ) : (
                  <>
                    <input type="number" step="0.01" name="precioCompra" value={formData.precioCompra ?? ''} onChange={handleChange}
                      className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                      placeholder="Opcional: si ya sabés cuánto te costó" />
                    <p className="text-xs text-gray-500 mt-1">
                      {isEditing
                        ? 'Todavía sin compras confirmadas: se puede corregir. En cuanto tenga una, este campo se bloquea.'
                        : 'Opcional: si ya sabés cuánto te costó. Después lo actualizan las compras confirmadas.'}
                    </p>
                  </>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Precio de Venta *</label>
                <input type="number" step="0.01" name="precioVenta" value={formData.precioVenta} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
              </div>

              {tipoTieneDimensiones && (
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Medida *</label>
                  <select
                    value={medidaSeleccionada}
                    onChange={handleSeleccionarMedida}
                    className="block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white mb-3"
                    required
                  >
                    <option value="" disabled>Seleccioná una medida</option>
                    {MEDIDAS_ESTANDAR.map((m) => (
                      <option key={m.value} value={m.value}>{m.label} ({m.ancho}x{m.largo} cm)</option>
                    ))}
                    <option value={MEDIDA_OTRA}>Medida Especial / Otra</option>
                  </select>

                  {dimensionesSinReconocer && !medidaSeleccionada && (
                    <p className="text-xs text-amber-600 mb-3">
                      Dato anterior sin el formato nuevo: "{dimensionesSinReconocer}". Elegí la medida y volvé a cargar Ancho/Largo.
                    </p>
                  )}

                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">Ancho (cm) *</label>
                      <input
                        type="number"
                        value={ancho}
                        onChange={(e) => setAncho(e.target.value)}
                        disabled={medidaEsEstandar}
                        required
                        className="block w-full border border-gray-300 rounded-md shadow-sm p-2 disabled:bg-gray-100 disabled:text-gray-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">Largo (cm) *</label>
                      <input
                        type="number"
                        value={largo}
                        onChange={(e) => setLargo(e.target.value)}
                        disabled={medidaEsEstandar}
                        required
                        className="block w-full border border-gray-300 rounded-md shadow-sm p-2 disabled:bg-gray-100 disabled:text-gray-500"
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">Alto / Grosor (cm)</label>
                      <input
                        type="number"
                        value={altoGrosor}
                        onChange={(e) => setAltoGrosor(e.target.value)}
                        className="block w-full border border-gray-300 rounded-md shadow-sm p-2"
                      />
                    </div>
                  </div>
                </div>
              )}

              {isEditing && (
                <div>
                  <label className="block text-sm font-medium text-gray-700">Stock actual</label>
                  <p className="mt-1 px-2 py-2 text-gray-700">
                    {stockActual ?? 0}
                    <span className="text-xs text-gray-500 ml-2">Se administra desde Inventario, no aquí.</span>
                  </p>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700">Stock Mínimo *</label>
                <input type="number" name="stockMinimo" value={formData.stockMinimo} onChange={handleChange}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" required />
                <p className="text-xs text-gray-500 mt-1">Umbral que dispara la alerta de stock bajo, no la cantidad real.</p>
              </div>

              <div className="flex items-center">
                <input type="checkbox" name="activo" checked={formData.activo} onChange={handleChange}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded" />
                <label className="ml-2 block text-sm text-gray-900">Activo</label>
              </div>
            </div>

            {/* Más detalles: Marca, Modelo, Calidad y Descripción son
                secundarios (ninguno es obligatorio para guardar). Plegado
                por defecto para no abrumar al cargar un producto nuevo;
                si el producto ya tenía algo cargado ahí, arranca abierto
                para no esconder datos que ya existían. */}
            <div className="mb-4 border border-gray-200 rounded-md bg-slate-50">
              <button
                type="button"
                onClick={() => setMostrarMasDetalles(v => !v)}
                className="w-full flex items-center justify-between px-3 py-2 text-sm font-medium text-gray-700 hover:bg-slate-100 rounded-md"
              >
                Más detalles (opcional)
                <span className="text-gray-400">{mostrarMasDetalles ? '▲' : '▼'}</span>
              </button>

              {mostrarMasDetalles && (
                <div className="px-3 pb-3 pt-1 border-t border-gray-200">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Marca</label>
                      <input type="text" name="marca" value={formData.marca} onChange={handleChange}
                        className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                        placeholder={placeholderMarca} />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700">Modelo</label>
                      <input type="text" name="modelo" value={formData.modelo} onChange={handleChange}
                        className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2" />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700">Calidad</label>
                      <select name="calidad" value={formData.calidad} onChange={handleChange}
                        className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2 bg-white">
                        <option value="">Sin especificar</option>
                        <option value="Estándar">Estándar</option>
                        <option value="Premium">Premium</option>
                        <option value="Alta gama">Alta gama</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700">Descripción</label>
                    <textarea name="descripcion" value={formData.descripcion} onChange={handleChange} rows={3}
                      className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"></textarea>
                  </div>
                </div>
              )}
            </div>

            {/* Imagen del producto */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">Imagen del producto</label>

              {!mostrarRecuadroVacio ? (
                <div
                  className="w-full h-48 rounded-lg border border-gray-200 bg-gray-50"
                >
                  <img
                    src={urlImagenMostrada!}
                    alt="Producto"
                    onClick={() => setMostrarVistaGrande(true)}
                    className="w-full h-full object-contain cursor-zoom-in"
                  />
                </div>
              ) : (
                <label
                  onDragOver={handleDragOverImagen}
                  onDragLeave={handleDragLeaveImagen}
                  onDrop={handleDropImagen}
                  className={`flex flex-col items-center justify-center gap-2 w-full h-48 rounded-lg border-2 border-dashed cursor-pointer transition-colors ${
                    arrastrandoImagen
                      ? 'border-primary-500 bg-primary-50'
                      : loadingImages
                        ? 'border-gray-200 bg-gray-50 cursor-not-allowed'
                        : 'border-gray-300 bg-gray-50 hover:border-primary-400 hover:bg-primary-50/40'
                  }`}
                >
                  <Upload size={24} className="text-gray-400" />
                  <span className="text-sm font-medium text-gray-600">Arrastrá una imagen aquí o hacé clic para adjuntar</span>
                  <span className="text-xs text-gray-400">JPG o PNG, hasta 5MB</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/jpg,image/png"
                    className="hidden"
                    disabled={loadingImages}
                    onChange={handleSeleccionarImagen}
                  />
                </label>
              )}

              {urlImagenMostrada && !reemplazandoImagen && (
                <div className="flex items-center gap-2 mt-2">
                  <button
                    type="button"
                    onClick={() => imagenActual ? setConfirmacionImagen('cambiar') : setReemplazandoImagen(true)}
                    disabled={loadingImages}
                    className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium bg-primary-600 text-white hover:bg-primary-700 disabled:opacity-50"
                  >
                    <Upload size={14} />
                    Cambiar imagen
                  </button>
                  <button
                    type="button"
                    onClick={handleQuitarImagen}
                    disabled={loadingImages}
                    className="inline-flex items-center gap-1 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 rounded-lg disabled:opacity-50"
                  >
                    <Trash2 size={14} /> Quitar
                  </button>
                </div>
              )}
            </div>

            {/* Vista grande de la imagen */}
            {mostrarVistaGrande && urlImagenMostrada && (
              <div
                className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-[70] p-4"
                onClick={() => setMostrarVistaGrande(false)}
              >
                <img
                  src={urlImagenMostrada}
                  alt="Producto"
                  className="max-w-full max-h-full object-contain"
                  onClick={(e) => e.stopPropagation()}
                />
                <button
                  type="button"
                  onClick={() => setMostrarVistaGrande(false)}
                  className="absolute top-4 right-4 text-white hover:text-gray-300"
                >
                  <X size={28} />
                </button>
              </div>
            )}

            {/* Metadato de la fila, no un dato de negocio: solo lectura,
                nunca se envía al guardar. Va al final, no en la cabecera,
                para no competir con el título de la acción. */}
            {isEditing && productoParaEditar && (
              <p className="text-xs text-gray-500 mt-4 text-right">
                Creado el {formatearFechaHora(productoParaEditar.fechaCreacion)}
                {' · '}Última edición {formatearFechaHora(productoParaEditar.fechaActualizacion)}
              </p>
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
      </div>

      {/* Confirmar antes de tocar una imagen que ya está guardada. El
          cambio real (subir, reemplazar o borrar) recién se aplica cuando
          se aprieta "Guardar Cambios", no acá. */}
      {confirmacionImagen && (
        <DeleteConfirmModal
          title={confirmacionImagen === 'cambiar' ? 'Cambiar imagen' : 'Quitar imagen'}
          message={
            confirmacionImagen === 'cambiar'
              ? 'La imagen actual se va a reemplazar por la que elijas ahora. El cambio recién queda guardado cuando apretés "Guardar Cambios".'
              : 'La imagen actual se va a quitar. El cambio recién queda guardado cuando apretés "Guardar Cambios".'
          }
          confirmLabel={confirmacionImagen === 'cambiar' ? 'Cambiar' : 'Quitar'}
          onConfirm={() => {
            if (confirmacionImagen === 'cambiar') {
              setReemplazandoImagen(true);
            } else {
              setQuitarImagenAlGuardar(true);
              setArchivoImagenPendiente(null);
              setPreviewImagenPendiente(null);
            }
            setConfirmacionImagen(null);
          }}
          onCancel={() => setConfirmacionImagen(null)}
        />
      )}
    </>
  );
}
