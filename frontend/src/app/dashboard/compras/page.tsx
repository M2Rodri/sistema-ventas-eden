'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  ArrowLeft, Plus, Search, Eye, PackageCheck, XCircle, Truck, CheckCircle2, ShoppingCart, X,
} from 'lucide-react';
import {
  getAllCompras, recibirCompra, cancelarCompra, cambiarEstadoCompra,
} from '@/lib/api';
import { Compra, EstadoCompra } from '@/types/proveedor';
import Breadcrumbs from '@/components/Breadcrumbs';
import CompraModal from '@/components/CompraModal';
import DetalleCompraModal from '@/components/DetalleCompraModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';

/**
 * Compras a proveedores.
 *
 * El módulo existía completo en el backend desde el principio —incluida la
 * carga automática de stock al recibir— pero no tenía pantalla: las compras
 * solo se veían como un listado de lectura dentro de Proveedores.
 *
 * Vive dentro de Inventario porque ese es el recorrido real: se ve que falta
 * stock, se registra la compra, llega, y el stock sube.
 */
export default function ComprasPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const proveedorFiltroId = searchParams.get('proveedor');

  const [compras, setCompras] = useState<Compra[]>([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [estadoFiltro, setEstadoFiltro] = useState<string>('TODOS');
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);

  const [modalNuevaAbierto, setModalNuevaAbierto] = useState(false);
  const [compraDetalle, setCompraDetalle] = useState<Compra | null>(null);
  const [compraARecibir, setCompraARecibir] = useState<Compra | null>(null);
  const [compraACancelar, setCompraACancelar] = useState<Compra | null>(null);

  const cargar = async () => {
    try {
      setLoading(true);
      setCompras(await getAllCompras());
    } catch (error: any) {
      avisar('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { cargar(); }, []);

  const avisar = (tipo: 'success' | 'error', texto: string) => {
    setMensaje({ tipo, texto });
    setTimeout(() => setMensaje(null), 4000);
  };

  const bs = (n?: number) =>
    `Bs ${Number(n ?? 0).toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  const fecha = (f?: string) => (f ? new Date(f).toLocaleDateString('es-BO') : '—');

  const badge = (estado: EstadoCompra | string) => {
    const estilos: Record<string, string> = {
      PENDIENTE: 'bg-amber-100 text-amber-800',
      CONFIRMADA: 'bg-blue-100 text-blue-800',
      EN_TRANSITO: 'bg-indigo-100 text-indigo-800',
      RECIBIDA: 'bg-green-100 text-green-800',
      CANCELADA: 'bg-red-100 text-red-800',
    };
    return estilos[estado] ?? 'bg-gray-100 text-gray-800';
  };

  const handleAvanzar = async (compra: Compra, accion: 'CONFIRMADA' | 'EN_TRANSITO') => {
    try {
      await cambiarEstadoCompra(compra.id, accion);
      avisar('success', `Compra #${compra.id} marcada como ${accion.replace('_', ' ').toLowerCase()}`);
      cargar();
    } catch (error: any) {
      avisar('error', error.message);
    }
  };

  const handleRecibir = async (compra: Compra) => {
    try {
      await recibirCompra(compra.id);
      avisar('success', `Compra #${compra.id} recibida. El stock fue actualizado.`);
      cargar();
    } catch (error: any) {
      avisar('error', error.message);
    } finally {
      setCompraARecibir(null);
    }
  };

  const handleCancelar = async (compra: Compra) => {
    try {
      await cancelarCompra(compra.id);
      avisar('success', `Compra #${compra.id} cancelada`);
      cargar();
    } catch (error: any) {
      avisar('error', error.message);
    } finally {
      setCompraACancelar(null);
    }
  };

  const filtradas = compras.filter((c) => {
    const texto = busqueda.toLowerCase();
    const coincide =
      !busqueda ||
      c.nombreProveedor?.toLowerCase().includes(texto) ||
      (c.numeroFactura ?? '').toLowerCase().includes(texto) ||
      String(c.id).includes(texto);
    const coincideEstado = estadoFiltro === 'TODOS' || c.estado === estadoFiltro;
    const coincideProveedor = !proveedorFiltroId || c.idProveedor === Number(proveedorFiltroId);
    return coincide && coincideEstado && coincideProveedor;
  });

  const nombreProveedorFiltrado = proveedorFiltroId
    ? compras.find((c) => c.idProveedor === Number(proveedorFiltroId))?.nombreProveedor
    : null;

  const contar = (estado: string) => compras.filter((c) => c.estado === estado).length;

  return (
    <div className="space-y-6">
      <Breadcrumbs items={[{ label: "Inventario", href: "/dashboard/inventario" }, { label: "Compras" }]} />

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link
            href="/dashboard/inventario"
            className="inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900 mb-2"
          >
            <ArrowLeft size={16} />
            Volver a Inventario
          </Link>
          <h1 className="text-3xl font-bold text-gray-900">Compras a proveedores</h1>
          <p className="text-gray-600 mt-1">
            Registrá la mercadería que ingresa. Al marcarla como recibida, el stock se actualiza solo.
          </p>
        </div>

        <button
          onClick={() => setModalNuevaAbierto(true)}
          className="inline-flex items-center gap-2 bg-primary-600 text-white px-6 py-2.5 rounded-lg hover:bg-primary-700 transition-colors font-medium"
        >
          <Plus size={20} />
          Nueva compra
        </button>
      </div>

      {mensaje && (
        <div className={`p-4 rounded-lg ${
          mensaje.tipo === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'
        }`}>
          {mensaje.texto}
        </div>
      )}

      {/* Resumen por estado */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { estado: 'PENDIENTE', etiqueta: 'Pendientes', icono: ShoppingCart },
          { estado: 'CONFIRMADA', etiqueta: 'Confirmadas', icono: CheckCircle2 },
          { estado: 'EN_TRANSITO', etiqueta: 'En tránsito', icono: Truck },
          { estado: 'RECIBIDA', etiqueta: 'Recibidas', icono: PackageCheck },
        ].map(({ estado, etiqueta, icono: Icono }) => (
          <div key={estado} className="bg-white p-5 rounded-xl border border-gray-200">
            <div className="flex items-center justify-between">
              <p className="text-sm text-gray-500">{etiqueta}</p>
              <Icono size={18} className="text-gray-400" />
            </div>
            <p className="text-2xl font-bold text-gray-900 mt-1">{contar(estado)}</p>
          </div>
        ))}
      </div>

      {/* Filtros */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por proveedor, factura o número de compra…"
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <select
          value={estadoFiltro}
          onChange={(e) => setEstadoFiltro(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="TODOS">Todos los estados</option>
          <option value="PENDIENTE">Pendientes</option>
          <option value="CONFIRMADA">Confirmadas</option>
          <option value="EN_TRANSITO">En tránsito</option>
          <option value="RECIBIDA">Recibidas</option>
          <option value="CANCELADA">Canceladas</option>
        </select>
        {proveedorFiltroId && (
          <span className="inline-flex items-center gap-2 px-3 py-2 bg-primary-50 text-primary-700 rounded-lg text-sm font-medium">
            Proveedor: {nombreProveedorFiltrado ?? `#${proveedorFiltroId}`}
            <button
              onClick={() => router.push('/dashboard/compras')}
              className="hover:text-primary-900"
              title="Quitar filtro de proveedor"
            >
              <X size={14} />
            </button>
          </span>
        )}
      </div>

      {/* Tabla */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        {loading ? (
          <p className="p-8 text-center text-gray-500">Cargando compras…</p>
        ) : filtradas.length === 0 ? (
          <div className="p-12 text-center">
            <ShoppingCart size={40} className="mx-auto text-gray-300 mb-3" />
            <p className="text-gray-600 font-medium">
              {compras.length === 0 ? 'Todavía no registraste compras' : 'Ninguna compra coincide con el filtro'}
            </p>
            {compras.length === 0 && (
              <p className="text-sm text-gray-500 mt-1">
                Registrá una compra para dejar constancia de la mercadería que ingresa y de su costo real.
              </p>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Compra</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Proveedor</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Factura</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Fecha</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Total</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estado</th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Acciones</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filtradas.map((compra) => (
                  <tr key={compra.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm font-medium text-gray-900">#{compra.id}</td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {compra.nombreProveedor}
                      <span className="block text-xs text-gray-500">NIT: {compra.nitProveedor || '—'}</span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-700">{compra.numeroFactura || '—'}</td>
                    <td className="px-6 py-4 text-sm text-gray-700">{fecha(compra.fechaCompra)}</td>
                    <td className="px-6 py-4 text-sm text-right font-semibold text-gray-900">
                      {bs(compra.montoTotal)}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${badge(compra.estado)}`}>
                        {String(compra.estado).replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => setCompraDetalle(compra)}
                          className="text-gray-600 hover:text-gray-900 p-1.5"
                          title="Ver detalle"
                        >
                          <Eye size={18} />
                        </button>

                        {compra.estado === 'PENDIENTE' && (
                          <button
                            onClick={() => handleAvanzar(compra, 'CONFIRMADA')}
                            className="text-blue-600 hover:text-blue-800 p-1.5"
                            title="Confirmar pedido al proveedor"
                          >
                            <CheckCircle2 size={18} />
                          </button>
                        )}

                        {compra.estado === 'CONFIRMADA' && (
                          <button
                            onClick={() => handleAvanzar(compra, 'EN_TRANSITO')}
                            className="text-indigo-600 hover:text-indigo-800 p-1.5"
                            title="Marcar en tránsito"
                          >
                            <Truck size={18} />
                          </button>
                        )}

                        {compra.estado !== 'RECIBIDA' && compra.estado !== 'CANCELADA' && (
                          <button
                            onClick={() => setCompraARecibir(compra)}
                            className="text-green-600 hover:text-green-800 p-1.5"
                            title="Recibir mercadería y sumar al stock"
                          >
                            <PackageCheck size={18} />
                          </button>
                        )}

                        {(compra.estado === 'PENDIENTE' || compra.estado === 'CONFIRMADA') && (
                          <button
                            onClick={() => setCompraACancelar(compra)}
                            className="text-red-600 hover:text-red-800 p-1.5"
                            title="Cancelar compra"
                          >
                            <XCircle size={18} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalNuevaAbierto && (
        <CompraModal
          onClose={() => setModalNuevaAbierto(false)}
          onSuccess={() => {
            setModalNuevaAbierto(false);
            avisar('success', 'Compra registrada. Marcala como recibida cuando llegue la mercadería.');
            cargar();
          }}
        />
      )}

      {compraDetalle && (
        <DetalleCompraModal compra={compraDetalle} onClose={() => setCompraDetalle(null)} />
      )}

      {compraARecibir && (
        <DeleteConfirmModal
          title="Recibir mercadería"
          message={
            `¿Confirmás que llegó la mercadería de la compra #${compraARecibir.id}?\n\n` +
            `Las cantidades se van a sumar al inventario y queda registrado el movimiento. ` +
            `Esta acción no se deshace.`
          }
          confirmLabel="Recibir mercadería"
          onConfirm={() => handleRecibir(compraARecibir)}
          onCancel={() => setCompraARecibir(null)}
        />
      )}

      {compraACancelar && (
        <DeleteConfirmModal
          title="Cancelar compra"
          message={`¿Cancelar la compra #${compraACancelar.id}?`}
          confirmLabel="Cancelar compra"
          onConfirm={() => handleCancelar(compraACancelar)}
          onCancel={() => setCompraACancelar(null)}
        />
      )}
    </div>
  );
}
