'use client';

import { useState, useEffect } from 'react';
import { X, AlertCircle, Search, Trash2, Package, Plus, Check } from 'lucide-react';
import {
  createCompra, getActiveProductos, getAllProductos, getActiveProveedores, getAllCategorias, getAllInventario,
  addImagenProducto, deleteImagenProducto,
} from '@/lib/api';
import { CompraRequest, Proveedor } from '@/types/proveedor';
import { Categoria, Producto, TipoProducto } from '@/types/producto';
import ProductoModal from '@/components/ProductoModal';
import ProductoDetalleModal from '@/components/ProductoDetalleModal';

interface CompraModalProps {
  /** Si viene, el proveedor queda fijo (se abre desde su ficha). */
  idProveedor?: number;
  nombreProveedor?: string;
  onClose: () => void;
  onSuccess: () => void;
}

interface LineaCompra {
  idProducto: number;
  cantidad: number;
  precioUnitario: number;
}

const TIPOS: { valor: TipoProducto; etiqueta: string }[] = [
  { valor: 'CAMA', etiqueta: 'Camas' },
  { valor: 'COLCHON', etiqueta: 'Colchones' },
  { valor: 'ALMOHADA', etiqueta: 'Almohadas' },
  { valor: 'ACCESORIO', etiqueta: 'Accesorios' },
  // { valor: 'MUEBLE', etiqueta: 'Muebles' }, // oculto por ahora
];

const normalizar = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

/**
 * Registro de una compra a proveedor.
 *
 * Los productos se eligen desde un modal aparte: buscador + filtro por
 * categoría + tildar los que querés y agregarlos todos juntos de una — como
 * elegir productos para una venta, pero permitiendo marcar varios antes de
 * confirmar en vez de agregar de a uno.
 *
 * El costo unitario arranca vacío a propósito, sin sugerir ningún precio:
 * el sistema solo sabe lo que se pagó la ÚLTIMA vez (y eso puede ya no ser
 * lo que se está pagando ahora), así que sugerirlo podía hacer pensar que
 * "ya está bien puesto" cuando en realidad hay que cargar el precio real de
 * esta compra.
 */
export default function CompraModal({
  idProveedor,
  nombreProveedor,
  onClose,
  onSuccess,
}: CompraModalProps) {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [productos, setProductos] = useState<Producto[]>([]);
  /** Todos los productos (activos e inactivos): solo para sugerir el siguiente SKU al crear uno. */
  const [todosLosProductos, setTodosLosProductos] = useState<Producto[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [stockPorProducto, setStockPorProducto] = useState<Map<number, number>>(new Map());
  const [productoVer, setProductoVer] = useState<Producto | null>(null);
  const [nuevoProductoAbierto, setNuevoProductoAbierto] = useState(false);
  const [proveedorSeleccionado, setProveedorSeleccionado] = useState<number>(
    idProveedor ?? 0
  );
  const [numeroFactura, setNumeroFactura] = useState('');
  const [notas, setNotas] = useState('');
  const [lineas, setLineas] = useState<LineaCompra[]>([]);
  const [loading, setLoading] = useState(false);
  const [cargandoDatos, setCargandoDatos] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal de selección de productos
  const [pickerAbierto, setPickerAbierto] = useState(false);
  const [busquedaPicker, setBusquedaPicker] = useState('');
  const [tipoPickerActivo, setTipoPickerActivo] = useState<TipoProducto | null>('CAMA');
  const [seleccionados, setSeleccionados] = useState<Set<number>>(new Set());

  const cargarDatos = async () => {
    const [prods, todos, provs, cats, inv] = await Promise.allSettled([
      getActiveProductos(),
      getAllProductos(),
      getActiveProveedores(),
      getAllCategorias(),
      getAllInventario(),
    ]);
    if (prods.status === 'fulfilled') setProductos(prods.value);
    if (todos.status === 'fulfilled') setTodosLosProductos(todos.value);
    if (provs.status === 'fulfilled') setProveedores(provs.value);
    if (cats.status === 'fulfilled') setCategorias(cats.value);
    if (inv.status === 'fulfilled') {
      setStockPorProducto(new Map(inv.value.map((i) => [i.idProducto, i.cantidadDisponible])));
    }
    setCargandoDatos(false);
  };

  useEffect(() => {
    cargarDatos();
  }, []);

  /** Lo que distingue a un producto de otro parecido: color, medida, material y marca. */
  const detalleCorto = (p: Producto) =>
    [p.color, p.dimensiones, p.materialArmazon || p.materialNucleo, p.marca].filter(Boolean).join(' · ');

  const imagenesDeProductoNuevo = {
    agregar: async (idProducto: number, file: File) => {
      await addImagenProducto(idProducto, file, true);
    },
    reemplazar: async (idImagenVieja: number, idProducto: number, file: File) => {
      await deleteImagenProducto(idImagenVieja);
      await addImagenProducto(idProducto, file, true);
    },
    eliminar: async (idImagen: number) => {
      await deleteImagenProducto(idImagen);
    },
  };

  /** Producto recién creado: queda agregado a la compra y se vuelve a ella. */
  const alCrearProducto = async (nuevo?: Producto) => {
    setNuevoProductoAbierto(false);
    setPickerAbierto(false);
    if (nuevo) {
      setLineas((actuales) => [...actuales, { idProducto: nuevo.id, cantidad: 1, precioUnitario: 0 }]);
    }
    await cargarDatos();
  };

  const idsUsados = new Set(lineas.map((l) => l.idProducto));

  const abrirPicker = () => {
    setBusquedaPicker('');
    setTipoPickerActivo('CAMA');
    setSeleccionados(new Set());
    setPickerAbierto(true);
  };

  const toggleSeleccion = (idProducto: number) => {
    setSeleccionados((prev) => {
      const nuevo = new Set(prev);
      if (nuevo.has(idProducto)) nuevo.delete(idProducto);
      else nuevo.add(idProducto);
      return nuevo;
    });
  };

  const confirmarSeleccion = () => {
    const nuevasLineas = Array.from(seleccionados).map((idProducto) => ({
      idProducto,
      cantidad: 1,
      precioUnitario: 0,
    }));
    setLineas([...lineas, ...nuevasLineas]);
    setPickerAbierto(false);
  };

  const productosDelPicker = productos.filter((p) => {
    if (idsUsados.has(p.id)) return false;
    if (tipoPickerActivo && p.tipoProducto !== tipoPickerActivo) return false;
    const texto = normalizar(busquedaPicker.trim());
    if (!texto) return true;
    return [p.nombre, p.sku, p.nombreCategoria, p.color, p.dimensiones, p.materialArmazon, p.materialNucleo, p.marca, p.modelo]
      .some((campo) => campo && normalizar(campo).includes(texto));
  });

  const actualizarLinea = (indice: number, cambios: Partial<LineaCompra>) => {
    setLineas(lineas.map((l, i) => (i === indice ? { ...l, ...cambios } : l)));
  };

  const quitarLinea = (indice: number) => {
    setLineas(lineas.filter((_, i) => i !== indice));
  };

  const total = lineas.reduce((suma, l) => suma + l.cantidad * l.precioUnitario, 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!proveedorSeleccionado) {
      setError('Seleccioná el proveedor de la compra');
      return;
    }
    if (lineas.length === 0) {
      setError('Agregá al menos un producto: una compra sin productos no carga stock');
      return;
    }
    if (lineas.some((l) => l.cantidad < 1)) {
      setError('Las cantidades deben ser mayores a cero');
      return;
    }
    if (lineas.some((l) => l.precioUnitario <= 0)) {
      setError('Cargá el costo real de cada producto (no puede quedar en blanco ni en 0)');
      return;
    }

    setLoading(true);
    try {
      const datos: CompraRequest = {
        idProveedor: proveedorSeleccionado,
        numeroFactura: numeroFactura.trim() || undefined,
        notas: notas.trim() || undefined,
        items: lineas,
      };
      await createCompra(datos);
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const bs = (n: number) => `Bs ${n.toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-3">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
        <div className="flex items-center justify-between px-5 py-2 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 shrink-0">
          <h2 className="text-base font-bold text-gray-900">
            Registrar compra
            {nombreProveedor && (
              <span className="font-normal text-gray-600"> · Proveedor: <strong>{nombreProveedor}</strong></span>
            )}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" disabled={loading}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
          <div className="flex-1 overflow-y-auto px-5 pt-4 pb-3 space-y-3">
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 text-red-800 rounded-lg">
              <AlertCircle size={18} className="flex-shrink-0" />
              <span className="text-sm">{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 items-start">
            {!idProveedor && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Proveedor *</label>
                <select
                  value={proveedorSeleccionado}
                  onChange={(e) => setProveedorSeleccionado(Number(e.target.value))}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
                  required
                >
                  <option value={0}>Seleccionar…</option>
                  {proveedores.map((p) => (
                    <option key={p.id} value={p.id}>{p.nombreEmpresa}</option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Número de factura
              </label>
              <input
                type="text"
                value={numeroFactura}
                onChange={(e) => setNumeroFactura(e.target.value)}
                maxLength={50}
                className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: F-2026-0451"
              />
            </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notas</label>
            <textarea
              value={notas}
              onChange={(e) => setNotas(e.target.value)}
              rows={2}
              maxLength={500}
              className="w-full px-2 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              placeholder="Ej: Reposición de stock para temporada"
            />
          </div>
          </div>

          {/* Productos */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-sm font-medium text-gray-700">Productos comprados *</label>
              <button
                type="button"
                onClick={abrirPicker}
                disabled={cargandoDatos}
                className="inline-flex items-center gap-1.5 text-sm px-3 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
              >
                <Plus size={16} />
                Buscar y agregar productos
              </button>
            </div>

            {/* Lista de lo que se fue agregando */}
            {lineas.length === 0 ? (
              <div className="text-center py-5 bg-gray-50 border border-dashed border-gray-300 rounded-lg">
                <Package size={32} className="mx-auto text-gray-400 mb-2" />
                <p className="text-sm text-gray-600">Todavía no agregaste productos</p>
                <p className="text-xs text-gray-500 mt-1">
                  Usá el botón de arriba para elegirlos. Al registrar la compra, estas cantidades se suman al inventario.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {lineas.map((linea, i) => {
                  const producto = productos.find((p) => p.id === linea.idProducto);
                  return (
                    <div key={linea.idProducto} className="border border-gray-200 rounded-lg p-3">
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <p className="font-medium text-sm text-gray-900">{producto?.nombre ?? `Producto #${linea.idProducto}`}</p>
                          <p className="text-xs text-gray-500">SKU: {producto?.sku ?? '—'}{producto && detalleCorto(producto) ? ` · ${detalleCorto(producto)}` : ''}</p>
                          {producto && (
                            <button type="button" onClick={() => setProductoVer(producto)} className="text-xs text-primary-600 hover:underline">
                              Ver detalle del producto
                            </button>
                          )}
                        </div>
                        <button
                          type="button"
                          onClick={() => quitarLinea(i)}
                          className="text-red-400 hover:text-red-600"
                          title="Quitar producto"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Cantidad</label>
                          <input
                            type="number"
                            min={1}
                            value={linea.cantidad}
                            onChange={(e) => actualizarLinea(i, { cantidad: Number(e.target.value) })}
                            className="w-full px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                          />
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Costo unitario</label>
                          <input
                            type="number"
                            min={0}
                            step="0.01"
                            value={linea.precioUnitario === 0 ? '' : linea.precioUnitario}
                            onChange={(e) => actualizarLinea(i, { precioUnitario: Number(e.target.value) || 0 })}
                            placeholder="Cargá el precio real"
                            className="w-full px-2 py-1.5 border border-gray-300 rounded-lg text-sm"
                          />
                        </div>
                        <div>
                          <p className="block text-xs text-gray-500 mb-1">Subtotal</p>
                          <p className="px-2 py-1.5 text-sm font-semibold text-gray-900">
                            {bs(linea.cantidad * linea.precioUnitario)}
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })}

                <div className="flex justify-end items-baseline gap-3 pt-3 border-t border-gray-200">
                  <span className="text-sm text-gray-600">Total de la compra:</span>
                  <span className="text-lg font-bold text-primary-600">{bs(total)}</span>
                </div>
              </div>
            )}
          </div>

          </div>

          <div className="flex justify-end gap-3 px-5 py-2.5 border-t border-gray-200 bg-white shrink-0">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading || cargandoDatos}
              className="px-4 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium disabled:opacity-50"
            >
              {loading
                ? 'Registrando…'
                : 'Registrar compra'}
            </button>
          </div>
        </form>
      </div>

      {/* Modal de selección de productos, arriba del modal de la compra. */}
      {pickerAbierto && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-3">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between p-5 border-b border-gray-200">
              <h3 className="text-base font-bold text-gray-900">Elegir productos</h3>
              <button
                type="button"
                onClick={() => setNuevoProductoAbierto(true)}
                className="ml-auto mr-3 inline-flex items-center gap-1 text-sm px-2.5 py-1 border border-primary-300 text-primary-700 rounded-lg hover:bg-primary-50"
                title="Si el producto todavía no existe, créalo y vuelve a esta compra"
              >
                <Plus size={14} />
                Nuevo producto
              </button>
              <button type="button" onClick={() => setPickerAbierto(false)} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>

            <div className="p-5 pb-3 space-y-3 border-b border-gray-200">
              <div className="flex items-center border border-gray-300 rounded-lg px-3 py-2 gap-2">
                <Search size={16} className="text-gray-400 flex-shrink-0" />
                <input
                  type="text"
                  autoFocus
                  value={busquedaPicker}
                  onChange={(e) => setBusquedaPicker(e.target.value)}
                  className="w-full outline-none text-sm"
                  placeholder="Buscar por nombre, SKU, color, medida o material…"
                />
              </div>
              <div className="flex flex-wrap gap-2">
                {TIPOS.map((t) => (
                  <button
                    key={t.valor}
                    type="button"
                    onClick={() => setTipoPickerActivo(tipoPickerActivo === t.valor ? null : t.valor)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium border ${
                      tipoPickerActivo === t.valor
                        ? 'border-primary-500 bg-primary-50 text-primary-700'
                        : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    {t.etiqueta}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-2">
              {productosDelPicker.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">
                  No se encontraron productos, o ya están todos agregados. Si no existe, usá «Nuevo producto».
                </p>
              ) : (
                productosDelPicker.map((p) => {
                  const marcado = seleccionados.has(p.id);
                  return (
                    <label
                      key={p.id}
                      className={`flex items-center gap-3 px-3 py-2 rounded-lg cursor-pointer ${
                        marcado ? 'bg-primary-50' : 'hover:bg-gray-50'
                      }`}
                    >
                      <span
                        className={`flex-shrink-0 w-5 h-5 rounded border-2 flex items-center justify-center ${
                          marcado ? 'bg-primary-600 border-primary-600' : 'border-gray-300'
                        }`}
                      >
                        {marcado && <Check size={14} className="text-white" />}
                      </span>
                      <input
                        type="checkbox"
                        checked={marcado}
                        onChange={() => toggleSeleccion(p.id)}
                        className="sr-only"
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900">{p.nombre}</p>
                        <p className="text-xs text-gray-500">
                          SKU: {p.sku}{p.nombreCategoria ? ` · ${p.nombreCategoria}` : ''}
                        </p>
                        {detalleCorto(p) && <p className="text-xs text-gray-600">{detalleCorto(p)}</p>}
                      </div>
                      <div className="flex-shrink-0 text-right">
                        <p className="text-xs text-gray-500">Stock: <span className="font-semibold text-gray-800">{stockPorProducto.get(p.id) ?? 0}</span></p>
                        <button
                          type="button"
                          onClick={(e) => { e.preventDefault(); e.stopPropagation(); setProductoVer(p); }}
                          className="text-xs text-primary-600 hover:underline"
                        >
                          Ver detalle
                        </button>
                      </div>
                    </label>
                  );
                })
              )}
            </div>

            <div className="flex items-center justify-between gap-3 p-5 border-t border-gray-200 bg-gray-50">
              <span className="text-sm text-gray-600">
                {seleccionados.size} producto{seleccionados.size === 1 ? '' : 's'} marcado{seleccionados.size === 1 ? '' : 's'}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPickerAbierto(false)}
                  className="px-2 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 text-sm font-medium"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={confirmarSeleccion}
                  disabled={seleccionados.size === 0}
                  className="px-2 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium disabled:opacity-50"
                >
                  Agregar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {productoVer && <ProductoDetalleModal producto={productoVer} onClose={() => setProductoVer(null)} />}

      {nuevoProductoAbierto && (
        <div className="relative z-[80]">
          <ProductoModal
            producto={null}
            productoParaEditar={null}
            productos={todosLosProductos}
            categorias={categorias}
            imagenesActuales={[]}
            onAgregarImagen={imagenesDeProductoNuevo.agregar}
            onReemplazarImagen={imagenesDeProductoNuevo.reemplazar}
            onEliminarImagen={imagenesDeProductoNuevo.eliminar}
            loadingImages={false}
            stockActualBloqueado
            onClose={() => setNuevoProductoAbierto(false)}
            onSuccess={alCrearProducto}
          />
        </div>
      )}
    </div>
  );
}
