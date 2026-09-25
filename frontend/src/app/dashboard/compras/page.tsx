'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useDragScrollTable } from '@/hooks/useDragScrollTable';
import {
  Plus, Search, Eye, Edit, PackageCheck, XCircle, ShoppingCart, X, AlertCircle,
} from 'lucide-react';
import {
  getAllCompras, recibirCompra, cancelarCompra,
} from '@/lib/api';
import { Compra, EstadoCompra } from '@/types/proveedor';
import Breadcrumbs from '@/components/Breadcrumbs';
import CompraModal from '@/components/CompraModal';
import DetalleCompraModal from '@/components/DetalleCompraModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import StatCard from '@/components/StatCard';
import { mensajeError } from '@/lib/errores';

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
function ComprasContent() {
  const searchParams = useSearchParams();
  const proveedorParam = searchParams.get('proveedor');
  // Qué proveedor se sacó del filtro por última vez, guardado en
  // sessionStorage (no en useState): el click en "Compras" del Sidebar a
  // veces se resuelve con una navegación real del lado de Next (afuera de
  // mi control) que puede llegar a remontar este componente antes de que
  // termine de aplicarse el reset. Un useState se pierde en ese remount;
  // sessionStorage no, sobrevive tanto a eso como a un F5.
  const CLAVE_SUPRIMIDO = 'compras:proveedor-suprimido';
  const [proveedorSuprimido, setProveedorSuprimido] = useState(
    () => typeof window !== 'undefined' && sessionStorage.getItem(CLAVE_SUPRIMIDO) === proveedorParam
  );
  useEffect(() => {
    // Se reactiva solo si aparece un proveedor nuevo de verdad (entraste
    // de nuevo desde Proveedores), nunca solo porque cambió el render.
    if (proveedorParam && sessionStorage.getItem(CLAVE_SUPRIMIDO) !== proveedorParam) {
      setProveedorSuprimido(false);
    }
  }, [proveedorParam]);
  const proveedorFiltroId = proveedorSuprimido ? null : proveedorParam;
  const suprimirFiltroProveedor = () => {
    sessionStorage.setItem(CLAVE_SUPRIMIDO, proveedorParam ?? '');
    setProveedorSuprimido(true);
  };

  const [compras, setCompras] = useState<Compra[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [busqueda, setBusqueda] = useState('');
  const [estadoFiltro, setEstadoFiltro] = useState<string>('TODOS');
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);

  const [modalNuevaAbierto, setModalNuevaAbierto] = useState(false);
  const [compraDetalle, setCompraDetalle] = useState<Compra | null>(null);
  const [compraAEditar, setCompraAEditar] = useState<Compra | null>(null);
  const [compraARecibir, setCompraARecibir] = useState<Compra | null>(null);
  const [compraACancelar, setCompraACancelar] = useState<Compra | null>(null);

  const cargar = async () => {
    try {
      setLoading(true);
      setLoadError(null);
      setCompras(await getAllCompras());
    } catch (error: any) {
      setLoadError(mensajeError(error, 'No se pudieron cargar las compras.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { cargar(); }, []);

  // Clickear "Compras" en el menú, aunque ya estés en esta pantalla, tiene
  // que volver todo a su estado normal (mismo mecanismo que Ventas e
  // Inventario: ver Sidebar.tsx).
  useEffect(() => {
    const resetearFiltros = () => {
      setBusqueda('');
      setEstadoFiltro('TODOS');
      suprimirFiltroProveedor();
    };
    window.addEventListener('compras:reset-filtros', resetearFiltros);
    return () => window.removeEventListener('compras:reset-filtros', resetearFiltros);
  }, []);

  // Los de éxito se cierran solos; los de error se quedan hasta que el
  // usuario los cierra a mano (el botón X del banner).
  const avisar = (tipo: 'success' | 'error', texto: string) => {
    setMensaje({ tipo, texto });
    if (tipo === 'success') {
      setTimeout(() => setMensaje(null), 4000);
    }
  };

  const bs = (n?: number) =>
    `Bs ${Number(n ?? 0).toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  const fecha = (f?: string) => (f ? new Date(f).toLocaleDateString('es-BO') : '—');

  const badge = (estado: EstadoCompra | string) => {
    const estilos: Record<string, string> = {
      POR_CONFIRMAR: 'bg-amber-100 text-amber-800',
      CONFIRMADA: 'bg-green-100 text-green-800',
      CANCELADA: 'bg-red-100 text-red-800',
    };
    return estilos[estado] ?? 'bg-gray-100 text-gray-800';
  };

  const etiquetaEstado = (estado: EstadoCompra | string) => {
    const etiquetas: Record<string, string> = {
      POR_CONFIRMAR: 'Por confirmar',
      CONFIRMADA: 'Confirmada',
      CANCELADA: 'Cancelada',
    };
    return etiquetas[estado] ?? String(estado);
  };

  const handleRecibir = async (compra: Compra) => {
    try {
      await recibirCompra(compra.id);
      avisar('success', `Compra #${compra.id} confirmada. El stock fue actualizado.`);
      cargar();
    } catch (error: any) {
      avisar('error', mensajeError(error));
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
      avisar('error', mensajeError(error));
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

  // Arrastrar la tabla desde el encabezado, como si fuera una barra de
  // scroll horizontal. Ver hooks/useDragScrollTable.ts.
  const {
    scrollContainerRef,
    tableRef,
    theadRef,
    hasOverflow,
    theadProps,
  } = useDragScrollTable([loading, compras, busqueda, estadoFiltro, proveedorFiltroId]);

  return (
    <>
    <div className="space-y-6">
      <Breadcrumbs items={[{ label: "Compras" }]} />

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Compras a proveedores</h1>
          <p className="text-gray-600 mt-1">
            Registrá compras ya realizadas. Confirmalas para actualizar el stock.
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
        <div className={`p-4 rounded-lg flex items-start justify-between gap-3 ${
          mensaje.tipo === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'
        }`}>
          <span>{mensaje.texto}</span>
          <button onClick={() => setMensaje(null)} className="flex-shrink-0 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Resumen por estado. Cada tarjeta fija TODOS los filtros (búsqueda,
          estado, proveedor de la URL), no solo el que le importa — mismo
          criterio que Ventas e Inventario. */}
      <div className="grid grid-cols-2 gap-4">
        <StatCard
          titulo="Por confirmar"
          valor={contar('POR_CONFIRMAR')}
          icon={<ShoppingCart size={22} />}
          loading={loading}
          onClick={() => {
            setBusqueda('');
            setEstadoFiltro('POR_CONFIRMAR');
            suprimirFiltroProveedor();
          }}
        />
        <StatCard
          titulo="Confirmadas"
          valor={contar('CONFIRMADA')}
          icon={<PackageCheck size={22} />}
          loading={loading}
          onClick={() => {
            setBusqueda('');
            setEstadoFiltro('CONFIRMADA');
            suprimirFiltroProveedor();
          }}
        />
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
          <option value="POR_CONFIRMAR">Por confirmar</option>
          <option value="CONFIRMADA">Confirmadas</option>
          <option value="CANCELADA">Canceladas</option>
        </select>
        {proveedorFiltroId && (
          <span className="inline-flex items-center gap-2 px-3 py-2 bg-primary-50 text-primary-700 rounded-lg text-sm font-medium">
            Proveedor: {nombreProveedorFiltrado ?? `#${proveedorFiltroId}`}
            <button
              onClick={suprimirFiltroProveedor}
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
          <div className="flex items-center justify-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : loadError ? (
          <div className="p-12 text-center">
            <AlertCircle size={40} className="mx-auto text-red-400 mb-3" />
            <p className="text-gray-700 font-medium mb-1">No se pudieron cargar las compras</p>
            <p className="text-sm text-gray-500 mb-4">{loadError}</p>
            <button
              onClick={cargar}
              className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
            >
              Reintentar
            </button>
          </div>
        ) : filtradas.length === 0 ? (
          <div className="p-12 text-center">
            <ShoppingCart size={40} className="mx-auto text-gray-300 mb-3" />
            <p className="text-gray-600 font-medium">
              {compras.length === 0 ? 'Todavía no registraste compras' : 'Ninguna compra coincide con el filtro'}
            </p>
            {compras.length === 0 && (
              <p className="text-sm text-gray-500 mt-1">
                Registrá una compra para dejar constancia de los productos que ingresan y de su costo real.
              </p>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto" ref={scrollContainerRef}>
            <table className="min-w-full divide-y divide-gray-200" ref={tableRef}>
              <thead
                ref={theadRef}
                className={`bg-gray-50 ${hasOverflow ? 'cursor-grab select-none' : ''}`}
                {...theadProps}
              >
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
                        {etiquetaEstado(compra.estado)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-center gap-0.5">
                        <button
                          onClick={() => setCompraDetalle(compra)}
                          className="text-gray-600 hover:text-gray-900 p-1.5"
                          title="Ver detalle"
                        >
                          <Eye size={18} />
                        </button>

                        {compra.estado === 'POR_CONFIRMAR' && (
                          <button
                            onClick={() => setCompraAEditar(compra)}
                            className="text-blue-600 hover:text-blue-800 p-1.5"
                            title="Editar compra"
                          >
                            <Edit size={18} />
                          </button>
                        )}

                        {compra.estado !== 'CONFIRMADA' && compra.estado !== 'CANCELADA' && (
                          <button
                            onClick={() => setCompraARecibir(compra)}
                            className="text-green-600 hover:text-green-800 p-1.5"
                            title="Confirmar compra y sumar al stock"
                          >
                            <PackageCheck size={18} />
                          </button>
                        )}

                        {compra.estado === 'POR_CONFIRMAR' && (
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

      {compraDetalle && (
        <DetalleCompraModal compra={compraDetalle} onClose={() => setCompraDetalle(null)} />
      )}

      {compraARecibir && (
        <DeleteConfirmModal
          title="Confirmar compra"
          message={
            `¿Confirmás los datos de la compra #${compraARecibir.id}?\n\n` +
            `Las cantidades se van a sumar al inventario y queda registrado el movimiento. ` +
            `Esta acción no se deshace.`
          }
          confirmLabel="Confirmar compra"
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

      {modalNuevaAbierto && (
        <CompraModal
          onClose={() => setModalNuevaAbierto(false)}
          onSuccess={() => {
            setModalNuevaAbierto(false);
            avisar('success', 'Compra registrada. Confirmala para actualizar el stock.');
            cargar();
          }}
        />
      )}

      {compraAEditar && (
        <CompraModal
          compra={compraAEditar}
          onClose={() => setCompraAEditar(null)}
          onSuccess={() => {
            setCompraAEditar(null);
            avisar('success', `Compra #${compraAEditar.id} actualizada.`);
            cargar();
          }}
        />
      )}
    </>
  );
}

export default function ComprasPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center py-16">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    }>
      <ComprasContent />
    </Suspense>
  );
}
