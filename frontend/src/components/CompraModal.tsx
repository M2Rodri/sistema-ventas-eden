'use client';

import { useState, useEffect } from 'react';
import { X, AlertCircle, Plus, Trash2, Package } from 'lucide-react';
import { createCompra, getActiveProductos, getActiveProveedores } from '@/lib/api';
import { CompraRequest, Proveedor } from '@/types/proveedor';
import { Producto } from '@/types/producto';

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

/**
 * Registro de una compra a proveedor.
 *
 * La versión anterior enviaba `items: []`, es decir una compra sin líneas: no
 * podía cargar stock y su total quedaba en cero. Acá se cargan los productos
 * con su cantidad y su costo, que es lo que el backend necesita para sumar al
 * inventario cuando la compra pase a RECIBIDA.
 */
export default function CompraModal({
  idProveedor,
  nombreProveedor,
  onClose,
  onSuccess,
}: CompraModalProps) {
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [productos, setProductos] = useState<Producto[]>([]);
  const [proveedorSeleccionado, setProveedorSeleccionado] = useState<number>(idProveedor ?? 0);
  const [numeroFactura, setNumeroFactura] = useState('');
  const [notas, setNotas] = useState('');
  const [lineas, setLineas] = useState<LineaCompra[]>([]);
  const [loading, setLoading] = useState(false);
  const [cargandoDatos, setCargandoDatos] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const [prods, provs] = await Promise.allSettled([
        getActiveProductos(),
        getActiveProveedores(),
      ]);
      if (prods.status === 'fulfilled') setProductos(prods.value);
      if (provs.status === 'fulfilled') setProveedores(provs.value);
      setCargandoDatos(false);
    })();
  }, []);

  const agregarLinea = () => {
    const disponible = productos.find((p) => !lineas.some((l) => l.idProducto === p.id));
    if (!disponible) return;
    setLineas([
      ...lineas,
      {
        idProducto: disponible.id,
        cantidad: 1,
        // Se propone el costo de referencia del producto como punto de partida.
        precioUnitario: Number(disponible.costoReferencial) || 0,
      },
    ]);
  };

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
    // El mismo producto dos veces viola la restricción de la base.
    const ids = lineas.map((l) => l.idProducto);
    if (new Set(ids).size !== ids.length) {
      setError('Hay un producto repetido: sumá las cantidades en una sola línea');
      return;
    }

    setLoading(true);
    try {
      const compra: CompraRequest = {
        idProveedor: proveedorSeleccionado,
        numeroFactura: numeroFactura.trim() || undefined,
        notas: notas.trim() || undefined,
        items: lineas,
      };
      await createCompra(compra);
      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const bs = (n: number) => `Bs ${n.toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Registrar compra</h2>
            <p className="text-sm text-gray-600 mt-1">
              {nombreProveedor
                ? <>Proveedor: <strong>{nombreProveedor}</strong></>
                : 'La compra queda pendiente hasta que marques la mercadería como recibida'}
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" disabled={loading}>
            <X size={24} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (
            <div className="flex items-center gap-2 p-4 bg-red-50 text-red-800 rounded-lg">
              <AlertCircle size={20} className="flex-shrink-0" />
              <span className="text-sm">{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {!idProveedor && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Proveedor *</label>
                <select
                  value={proveedorSeleccionado}
                  onChange={(e) => setProveedorSeleccionado(Number(e.target.value))}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
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
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Número de factura
              </label>
              <input
                type="text"
                value={numeroFactura}
                onChange={(e) => setNumeroFactura(e.target.value)}
                maxLength={50}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="Ej: F-2026-0451"
              />
              <p className="text-xs text-gray-500 mt-1">Respaldo del gasto. No se puede repetir para el mismo proveedor.</p>
            </div>
          </div>

          {/* Productos */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="block text-sm font-medium text-gray-700">Productos comprados *</label>
              <button
                type="button"
                onClick={agregarLinea}
                disabled={cargandoDatos || lineas.length >= productos.length}
                className="inline-flex items-center gap-1.5 text-sm px-3 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
              >
                <Plus size={16} />
                Agregar producto
              </button>
            </div>

            {lineas.length === 0 ? (
              <div className="text-center py-8 bg-gray-50 border border-dashed border-gray-300 rounded-lg">
                <Package size={32} className="mx-auto text-gray-400 mb-2" />
                <p className="text-sm text-gray-600">Todavía no agregaste productos</p>
                <p className="text-xs text-gray-500 mt-1">
                  Al recibir la compra, estas cantidades se suman al inventario.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {lineas.map((linea, i) => {
                  const usados = lineas.filter((_, j) => j !== i).map((l) => l.idProducto);
                  return (
                    <div key={i} className="grid grid-cols-12 gap-2 items-end bg-gray-50 p-3 rounded-lg">
                      <div className="col-span-12 sm:col-span-5">
                        <label className="block text-xs text-gray-500 mb-1">Producto</label>
                        <select
                          value={linea.idProducto}
                          onChange={(e) => actualizarLinea(i, { idProducto: Number(e.target.value) })}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-white text-sm"
                        >
                          {productos
                            .filter((p) => !usados.includes(p.id))
                            .map((p) => (
                              <option key={p.id} value={p.id}>{p.sku} · {p.nombre}</option>
                            ))}
                        </select>
                      </div>

                      <div className="col-span-4 sm:col-span-2">
                        <label className="block text-xs text-gray-500 mb-1">Cantidad</label>
                        <input
                          type="number"
                          min={1}
                          value={linea.cantidad}
                          onChange={(e) => actualizarLinea(i, { cantidad: Number(e.target.value) })}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                        />
                      </div>

                      <div className="col-span-5 sm:col-span-3">
                        <label className="block text-xs text-gray-500 mb-1">Costo unitario</label>
                        <input
                          type="number"
                          min={0}
                          step="0.01"
                          value={linea.precioUnitario}
                          onChange={(e) => actualizarLinea(i, { precioUnitario: Number(e.target.value) })}
                          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                        />
                      </div>

                      <div className="col-span-2 sm:col-span-1 text-right">
                        <p className="text-xs text-gray-500 mb-1">Subtotal</p>
                        <p className="text-sm font-semibold text-gray-900 whitespace-nowrap">
                          {bs(linea.cantidad * linea.precioUnitario)}
                        </p>
                      </div>

                      <div className="col-span-1 flex justify-end">
                        <button
                          type="button"
                          onClick={() => quitarLinea(i)}
                          className="text-red-600 hover:text-red-800 p-2"
                          title="Quitar producto"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </div>
                  );
                })}

                <div className="flex justify-end items-baseline gap-3 pt-3 border-t border-gray-200">
                  <span className="text-sm text-gray-600">Total de la compra:</span>
                  <span className="text-2xl font-bold text-primary-600">{bs(total)}</span>
                </div>
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Notas</label>
            <textarea
              value={notas}
              onChange={(e) => setNotas(e.target.value)}
              rows={2}
              maxLength={500}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              placeholder="Ej: Reposición de stock para temporada"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="flex-1 px-4 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading || cargandoDatos}
              className="flex-1 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium disabled:opacity-50"
            >
              {loading ? 'Registrando…' : 'Registrar compra'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
