'use client';

import { formatearFechaLimite } from '@/lib/fechaLimite';

import React, { useState, useEffect } from 'react';
import { X, User, Calendar, CreditCard, Package, AlertCircle, FileText, Upload, Image as ImageIcon, Banknote, Truck } from 'lucide-react';
import { Venta, Pago, EstadoEntrega, EstadoVenta, ModalidadEntrega } from '@/types/venta';
import { actualizarFechaLimitePago, adjuntarComprobantePago, marcarVentaEntregada, urlArchivo } from '@/lib/api';
import { ACCEPT_COMPROBANTE, archivoDeEventoSoltar, errorDeComprobante, imagenDelPortapapeles, TEXTO_FORMATOS_COMPROBANTE, urlEsPdf } from '@/lib/comprobanteImagen';
import {
  CORREGIR_ENTREGA_ACTIVO,
  claseBadgeEstadoEntrega,
  etiquetaEstadoEntrega,
  etiquetaModalidad,
  faltaCompletarEnvio,
} from '@/lib/entrega';
import ComprobanteModal from '@/components/ComprobanteModal';
import DeleteConfirmModal from '@/components/DeleteConfirmModal';
import DeshacerEntregaModal from '@/components/DeshacerEntregaModal';
import EditarEntregaModal from '@/components/EditarEntregaModal';

interface DetalleVentaModalProps {
  isOpen: boolean;
  onClose: () => void;
  venta: Venta;
  onUpdated?: () => void;
  onCobrarSaldo?: () => void;
  /** Corregir a pendiente y editar la entrega son solo de ADMIN. */
  userRole?: 'ADMIN' | 'EMPLEADO';
  /** Se llama con la venta ya actualizada después de una acción de entrega. */
  onVentaActualizada?: (venta: Venta) => void;
}

export default function DetalleVentaModal({
  isOpen,
  onClose,
  venta,
  onUpdated,
  onCobrarSaldo,
  userRole = 'EMPLEADO',
  onVentaActualizada,
}: DetalleVentaModalProps) {
  const [showComprobanteModal, setShowComprobanteModal] = useState(false);
  const [confirmarEntrega, setConfirmarEntrega] = useState(false);
  const [showDeshacerModal, setShowDeshacerModal] = useState(false);
  const [showEditarEntregaModal, setShowEditarEntregaModal] = useState(false);
  const [entregaError, setEntregaError] = useState<string | null>(null);
  const [entregaProcesando, setEntregaProcesando] = useState(false);
  const [guardandoFecha, setGuardandoFecha] = useState(false);
  const [errorFecha, setErrorFecha] = useState<string | null>(null);

  // Poner o cambiar la fecha límite del pago pendiente. Los botones mandan los
  // días (el servidor calcula la fecha con su reloj); el calendario, la fecha.
  const guardarFechaLimite = async (datos: { plazoDiasPago?: number; fechaLimitePago?: string }) => {
    setErrorFecha(null);
    setGuardandoFecha(true);
    try {
      const actualizada = await actualizarFechaLimitePago(venta.id, datos);
      onVentaActualizada?.(actualizada);
      onUpdated?.();
    } catch (e: any) {
      setErrorFecha(e?.message ?? 'No se pudo guardar la fecha límite');
    } finally {
      setGuardandoFecha(false);
    }
  };
  const [pagosState, setPagosState] = useState<Pago[]>(venta.pagos);
  const [subiendoId, setSubiendoId] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  // Si venta.pagos cambia (por ejemplo, al cobrar el saldo pendiente desde
  // este mismo modal), este estado local se pone al día.
  useEffect(() => {
    setPagosState(venta.pagos);
  }, [venta.pagos]);

  // Botón "Pegar imagen": toma la imagen copiada y la sube como comprobante de ese pago.
  const handlePegarComprobante = async (idPago: number) => {
    setUploadError(null);
    try {
      const imagen = await imagenDelPortapapeles();
      if (!imagen) {
        setUploadError('No hay una imagen copiada. Copiá la captura o la imagen del comprobante y volvé a intentar.');
        return;
      }
      await handleAdjuntarComprobante(idPago, imagen);
    } catch (err: any) {
      setUploadError(err?.message || 'No se pudo pegar la imagen. Probá con "Adjuntar comprobante".');
    }
  };

  const tienePagosSinRespaldo = pagosState.some((p) => p.sinRespaldo);

  // Total pagado: lo que ya se cobró. En una venta anulada el saldo queda en 0,
  // así que no sirve restarlo: se suman los pagos registrados. No se sabe si el
  // dinero se devolvió, por eso en ese caso la fila dice "antes de anular".
  const esAnulada = venta.estado === 'CANCELADA';
  const totalPagado = Math.round(
    (esAnulada
      ? pagosState.filter((p) => p.estado !== 'RECHAZADO').reduce((acc, p) => acc + p.monto, 0)
      : venta.montoTotal - (venta.saldoPendiente ?? 0)) * 100,
  ) / 100;
  const hayDeuda = venta.estado === 'PENDIENTE_PAGO' && (venta.saldoPendiente ?? 0) > 0;

  const handleAdjuntarComprobante = async (idPago: number, file: File) => {
    setUploadError(null);
    const motivo = errorDeComprobante(file);
    if (motivo) {
      setUploadError(motivo);
      return;
    }
    setSubiendoId(idPago);
    try {
      const actualizado = await adjuntarComprobantePago(idPago, file);
      setPagosState((prev) =>
        prev.map((p) =>
          p.id === idPago
            ? { ...p, urlComprobante: actualizado.urlComprobante, sinRespaldo: actualizado.sinRespaldo }
            : p,
        ),
      );
      onUpdated?.();
    } catch (err: any) {
      setUploadError(err.message || 'Error al subir el comprobante');
    } finally {
      setSubiendoId(null);
    }
  };

  // Entregar devuelve la venta actualizada: se la pasa al padre
  // para que la tabla y este detalle muestren el estado nuevo.
  const ejecutarAccionEntrega = async (accion: () => Promise<Venta>) => {
    setEntregaError(null);
    setEntregaProcesando(true);
    try {
      const actualizada = await accion();
      onVentaActualizada?.(actualizada);
    } catch (err: any) {
      setEntregaError(err.message || 'No se pudo actualizar la entrega');
    } finally {
      setEntregaProcesando(false);
    }
  };

  if (!isOpen) return null;

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(price);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  const getEstadoBadge = () => {
    const colors = {
      COMPLETADA: 'bg-green-100 text-green-800',
      PENDIENTE_PAGO: 'bg-yellow-100 text-yellow-800',
      CANCELADA: 'bg-red-100 text-red-800',
    };
    const labels = {
      COMPLETADA: 'Completada',
      PENDIENTE_PAGO: 'Pago pendiente',
      CANCELADA: 'Cancelada',
    };
    return { color: colors[venta.estado], label: labels[venta.estado] };
  };

  const getMetodoPagoIcon = () => {
    const icons: Record<string, string> = {
      EFECTIVO: '💵',
      TRANSFERENCIA: '🏦',
      QR: '📱',
    };
    // metodoPago puede venir vacío (venta sin cobros) o como "VARIOS"
    // cuando la venta se pagó con más de un método.
    return icons[venta.metodoPago ?? ''] || '💰';
  };

  const getMetodoPagoLabel = (metodo: string) => {
    const labels: Record<string, string> = {
      EFECTIVO: 'Efectivo',
      TRANSFERENCIA: 'Transferencia',
      QR: 'QR',
    };
    return labels[metodo] || metodo;
  };

  const handleVerComprobante = () => {
    setShowComprobanteModal(true);
  };

  // Antes había acá dos botones simulados: "Imprimir Comprobante" y
  // "Descargar PDF", que solo mostraban un alert. Se quitaron porque
  // "Ver Comprobante" ya abre el comprobante real, y desde ahí se imprime
  // o se guarda como PDF con el diálogo del navegador.
  //
  // También había una observación inventada que aparecía en toda venta cuyo
  // id fuera múltiplo de 3. La tabla ventas no tiene columna de
  // observaciones, así que la sección se eliminó.

  const estadoBadge = getEstadoBadge();

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-3">
        <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-3.5 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
            <div>
              <h2 className="text-lg font-bold text-gray-900">Detalle de Venta</h2>
              <p className="text-xs text-gray-600 mt-0.5">
                ID: <span className="font-bold text-blue-600">#{venta.id}</span>
              </p>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors p-1"
            >
              <X size={20} />
            </button>
          </div>

          {/* Información General */}
          <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-3.5">
            {/* Cliente */}
            <div className="bg-gray-50 p-3 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <User className="text-blue-600" size={18} />
                <h3 className="text-sm font-semibold text-gray-900">Cliente</h3>
              </div>
              <p className="text-gray-900 font-medium text-sm">{venta.nombreCliente}</p>
              <p className="text-xs text-gray-600">
                {venta.ciCliente && <>NIT / CI: {venta.ciCliente}</>}
                {venta.ciCliente && venta.telefonoCliente && ' · '}
                {venta.telefonoCliente && <>Cel: {venta.telefonoCliente}</>}
                {!venta.ciCliente && !venta.telefonoCliente && '—'}
              </p>
            </div>

            {/* Vendedor */}
            <div className="bg-gray-50 p-3 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <User className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Vendedor</h3>
              </div>
              <p className="text-gray-900 font-medium">{venta.nombreUsuario}</p>
            </div>

            {/* Fecha */}
            <div className="bg-gray-50 p-3 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <Calendar className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Fecha de Venta</h3>
              </div>
              <p className="text-gray-900">{formatDate(venta.fechaVenta)}</p>
            </div>

            {/* Método de Pago */}
            <div className="bg-gray-50 p-3 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <CreditCard className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Método de Pago</h3>
              </div>
              <p className="text-gray-900 flex items-center gap-2">
                <span>{getMetodoPagoIcon()}</span>
                {getMetodoPagoLabel(venta.metodoPago ?? "")}
              </p>
            </div>
          </div>

          {/* Estado */}
          <div className="px-5 pb-6">
            <div className="bg-gray-50 p-3 rounded-lg">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Estado de pago:</span>
                <span className={`px-4 py-1.5 rounded-full text-sm font-semibold ${estadoBadge.color}`}>
                  {estadoBadge.label}
                </span>
              </div>
            </div>
          </div>

          {/* Entrega */}
          {(() => {
            const cancelada = venta.estado === EstadoVenta.CANCELADA;
            const esAdmin = userRole === 'ADMIN';
            const esRetiro = venta.modalidadEntrega === ModalidadEntrega.RETIRO;
            const esTransportadora = venta.modalidadEntrega === ModalidadEntrega.TRANSPORTADORA;
            const estado = venta.estadoEntrega;
            const puedeEntregar = estado !== EstadoEntrega.ENTREGADO && !cancelada;
            const puedeCorregir = CORREGIR_ENTREGA_ACTIVO && esAdmin && !esRetiro && estado === EstadoEntrega.ENTREGADO && !cancelada;
            const puedeEditar = esAdmin && !esRetiro && !cancelada && estado !== EstadoEntrega.ENTREGADO;
            const dato = (valor?: string) => valor?.trim() || null;

            return (
              <div className="px-5 pb-6">
                <div className="bg-gray-50 p-3 rounded-lg">
                  <div className="flex items-center justify-between gap-3 flex-wrap">
                    <div className="flex items-center gap-2">
                      <Truck className="text-blue-600" size={20} />
                      <h3 className="font-semibold text-gray-900">Entrega</h3>
                    </div>
                    <div className="flex items-center gap-2">
                      {faltaCompletarEnvio(venta) && (
                        <span className="px-3 py-1 text-xs font-semibold rounded-full border bg-orange-50 text-orange-700 border-orange-200">
                          Falta completar
                        </span>
                      )}
                      {puedeCorregir ? (
                        <button
                          type="button"
                          onClick={() => setShowDeshacerModal(true)}
                          disabled={entregaProcesando}
                          title="Corregir a Pendiente"
                          className={`px-3 py-1 text-xs font-semibold rounded-full border cursor-pointer hover:opacity-80 transition-opacity ${claseBadgeEstadoEntrega(estado)}`}
                        >
                          {etiquetaEstadoEntrega(estado)}
                        </button>
                      ) : cancelada ? (
                        <span className="px-3 py-1 text-xs font-semibold rounded-full border bg-red-100 text-red-800 border-red-200">
                          Cancelada
                        </span>
                      ) : (
                        <span className={`px-3 py-1 text-xs font-semibold rounded-full border ${claseBadgeEstadoEntrega(estado)}`}>
                          {etiquetaEstadoEntrega(estado)}
                        </span>
                      )}
                    </div>
                  </div>

                  <p className="text-sm font-medium text-gray-900 mt-3">{etiquetaModalidad(venta.modalidadEntrega)}</p>

                  {!esRetiro && (
                    <dl className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-sm">
                      <div>
                        <dt className="text-gray-500">Dirección</dt>
                        <dd className="text-gray-900">{dato(venta.direccionDestino) ?? '—'}</dd>
                      </div>
                      {esTransportadora && (
                        <>
                          <div>
                            <dt className="text-gray-500">Ciudad</dt>
                            <dd className="text-gray-900">{dato(venta.ciudad) ?? '—'}</dd>
                          </div>
                          <div>
                            <dt className="text-gray-500">Transportadora</dt>
                            <dd className="text-gray-900">{dato(venta.transportadora) ?? 'Sin completar'}</dd>
                          </div>
                          <div>
                            <dt className="text-gray-500">Guía de remisión</dt>
                            <dd className="text-gray-900">{dato(venta.guiaRemision) ?? 'Sin completar'}</dd>
                          </div>
                        </>
                      )}
                    </dl>
                  )}

                  {entregaError && (
                    <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
                      {entregaError}
                    </div>
                  )}

                  {(puedeEntregar || puedeEditar) && (
                    <div className="flex flex-wrap gap-2 mt-3">
                      {puedeEntregar && (
                        <button
                          onClick={() => setConfirmarEntrega(true)}
                          disabled={entregaProcesando}
                          className="px-3.5 py-1.5 text-sm font-medium bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-60"
                        >
                          Marcar entregada
                        </button>
                      )}
                      {puedeEditar && (
                        <button
                          onClick={() => setShowEditarEntregaModal(true)}
                          disabled={entregaProcesando}
                          className="px-3.5 py-1.5 text-sm font-medium border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors disabled:opacity-60"
                        >
                          Editar datos de entrega
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })()}

          {/* Aviso: pagos sin respaldo */}
          {tienePagosSinRespaldo && (
            <div className="px-5 pb-6">
              <div className="bg-orange-50 border border-orange-200 p-3 rounded-lg flex items-start gap-3">
                <AlertCircle className="text-orange-600 flex-shrink-0" size={20} />
                <p className="text-sm text-orange-800">
                  Esta venta tiene pagos sin respaldo: falta la foto del comprobante de un pago
                  por QR o transferencia. Se puede adjuntar más abajo, en "Pagos Registrados".
                </p>
              </div>
            </div>
          )}



          {/* Detalle de Productos */}
          <div className="px-5 pb-6">
            <div className="flex items-center gap-2 mb-3">
              <Package className="text-blue-600" size={20} />
              <h3 className="font-semibold text-gray-900">Productos</h3>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    {/* NUEVA COLUMNA: Código */}
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                    <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Precio Unit.</th>
                    <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Subtotal</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {venta.detalles.map((detalle, index) => (
                    <tr key={index}>
                      {/* NUEVA COLUMNA: Código del producto */}
                      <td className="px-4 py-2">
                        <span className="text-xs font-mono bg-gray-100 px-2 py-1 rounded text-gray-700">
                          {detalle.skuProducto}
                        </span>
                      </td>
                      <td className="px-4 py-2">
                        <div className="text-sm font-medium text-gray-900">{detalle.nombreProducto}</div>
                      </td>
                      <td className="px-4 py-2 text-center text-sm text-gray-900">{detalle.cantidad}</td>
                      <td className="px-4 py-2 text-right text-sm text-gray-900">{formatPrice(detalle.precioUnitario)}</td>
                      <td className="px-4 py-2 text-right text-sm font-semibold text-gray-900">
                        {formatPrice(detalle.subtotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Total */}
          <div className="px-5 pb-6">
            <div className="bg-blue-50 border-2 border-blue-200 p-3 rounded-lg">
              <div className="flex justify-between items-center">
                <span className="text-lg font-bold text-gray-900">TOTAL:</span>
                <span className="text-2xl font-bold text-blue-600">{formatPrice(venta.montoTotal)}</span>
              </div>
              <div className="flex justify-between items-center mt-2 pt-2 border-t border-blue-200">
                <span className="text-sm font-medium text-gray-700">
                  {esAnulada ? 'Pagado antes de cancelar:' : 'Total pagado:'}
                </span>
                <span className="text-base font-semibold text-gray-900">{formatPrice(totalPagado)}</span>
              </div>
              {hayDeuda && (
                <div className="flex justify-between items-center mt-1">
                  <span className="text-sm font-medium text-gray-700">Saldo pendiente:</span>
                  <span className="text-base font-bold text-red-600">{formatPrice(venta.saldoPendiente ?? 0)}</span>
                </div>
              )}
              {hayDeuda && (
                <div className="mt-2 pt-2 border-t border-blue-200">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium text-gray-700">Fecha límite:</span>
                    <span
                      className={`text-sm font-semibold ${
                        venta.fechaLimiteVencida ? 'text-red-600' : 'text-gray-900'
                      }`}
                    >
                      {venta.fechaLimitePago
                        ? `${formatearFechaLimite(venta.fechaLimitePago)}${venta.fechaLimiteVencida ? ' (vencida)' : ''}`
                        : 'Sin fecha'}
                    </span>
                  </div>
                  {/* Solo para ponerla si se olvidó; con fecha ya puesta no se muestra nada. */}
                  {!venta.fechaLimitePago && (
                    <div className="flex flex-wrap items-center gap-2 mt-2">
                      <span className="text-xs text-gray-500">
                        Poner fecha:
                      </span>
                      {[7, 15, 30].map((dias) => (
                        <button
                          key={dias}
                          type="button"
                          disabled={guardandoFecha}
                          onClick={() => guardarFechaLimite({ plazoDiasPago: dias })}
                          className="px-3 py-1 text-xs rounded-lg border border-gray-300 text-gray-700 hover:bg-white font-medium disabled:opacity-50"
                        >
                          {dias} días
                        </button>
                      ))}
                      <input
                        type="date"
                        disabled={guardandoFecha}
                        value=""
                        onChange={(e) => e.target.value && guardarFechaLimite({ fechaLimitePago: e.target.value })}
                        className="px-2 py-1 text-sm border border-gray-300 rounded-lg disabled:opacity-50"
                      />
                    </div>
                  )}
                  {errorFecha && <p className="text-xs text-red-600 mt-1">{errorFecha}</p>}
                </div>
              )}
            </div>
          </div>

          {/* Pagos registrados */}
          {pagosState && pagosState.length > 0 && (
            <div className="px-5 pb-6">
              <div className="flex items-center gap-2 mb-3">
                <CreditCard className="text-blue-600" size={20} />
                <h3 className="font-semibold text-gray-900">Pagos Registrados</h3>
              </div>
              {venta.estado === 'CANCELADA' && (
                <div className="mb-2 p-3 bg-gray-100 border border-gray-300 rounded-lg text-sm text-gray-600">
                  Esta venta está cancelada: los pagos de abajo ya no son válidos, quedan solo como registro histórico.
                </div>
              )}
              {uploadError && (
                <div className="mb-2 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
                  {uploadError}
                </div>
              )}
              <div className="space-y-3">
                {pagosState.map((pago, index) => (
                  <div key={index} className={`border p-3 rounded-lg ${
                    venta.estado === 'CANCELADA'
                      ? 'bg-gray-50 border-gray-200 opacity-60'
                      : 'bg-green-50 border-green-200'
                  }`}>
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="text-sm font-medium text-gray-900 flex items-center gap-2">
                          {getMetodoPagoLabel(pago.metodoPago)}
                          {pago.sinRespaldo && (
                            <span className="px-2 py-0.5 text-xs bg-orange-100 text-orange-700 rounded-full">
                              Sin respaldo
                            </span>
                          )}
                        </p>
                        {pago.referencia && (
                          <p className="text-xs text-gray-600">Ref: {pago.referencia}</p>
                        )}
                        <p className="text-xs text-gray-500">
                          {formatDate(pago.fechaPago)}
                          {pago.nombreUsuario && ` · Registrado por ${pago.nombreUsuario}`}
                        </p>
                      </div>
                      <span className={`text-lg font-bold ${venta.estado === 'CANCELADA' ? 'text-gray-500 line-through' : 'text-green-700'}`}>
                        {formatPrice(pago.monto)}
                      </span>
                    </div>

                    <div
                      className="mt-2 pt-2 border-t border-green-200 flex flex-wrap items-center gap-x-3 gap-y-2"
                      onDragOver={(e) => e.preventDefault()}
                      onDrop={(e) => {
                        e.preventDefault();
                        const soltado = archivoDeEventoSoltar(e);
                        if (soltado) handleAdjuntarComprobante(pago.id, soltado);
                      }}
                    >
                      {pago.urlComprobante ? (
                        <a
                          href={urlArchivo(pago.urlComprobante)}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800"
                        >
                          {urlEsPdf(pago.urlComprobante) ? (
                            <FileText size={14} />
                          ) : (
                            <img
                              src={urlArchivo(pago.urlComprobante)}
                              alt="Comprobante"
                              className="h-10 w-10 rounded border border-gray-200 object-cover"
                            />
                          )}
                          <ImageIcon size={14} /> Ver comprobante
                        </a>
                      ) : (
                        <p className="text-xs text-gray-400">Sin comprobante adjunto<span className="hidden sm:inline"> (podés arrastrar el archivo acá)</span></p>
                      )}

                      <button
                        type="button"
                        onClick={() => handlePegarComprobante(pago.id)}
                        disabled={subiendoId === pago.id}
                        title={`Pegar la imagen copiada (${TEXTO_FORMATOS_COMPROBANTE})`}
                        className="flex items-center gap-1 text-xs text-gray-600 hover:text-gray-900 ml-auto disabled:opacity-50"
                      >
                        <ImageIcon size={14} /> Pegar imagen
                      </button>

                      <label className="flex items-center gap-1 text-xs text-gray-600 hover:text-gray-900 cursor-pointer min-h-[2.5rem] md:min-h-0">
                        <Upload size={14} />
                        {subiendoId === pago.id
                          ? 'Subiendo...'
                          : pago.urlComprobante
                            ? 'Reemplazar'
                            : 'Adjuntar comprobante'}
                        <input
                          type="file"
                          accept={ACCEPT_COMPROBANTE}
                          className="hidden"
                          disabled={subiendoId === pago.id}
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) handleAdjuntarComprobante(pago.id, file);
                            e.target.value = '';
                          }}
                        />
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Botones */}
          <div className="flex flex-wrap gap-2 sm:gap-3 px-4 sm:px-5 py-3.5 border-t border-gray-200 bg-gray-50">
            
            {/* El comprobante se genera para toda venta al registrarla, sin
                importar el estado: documenta qué se vendió, no si está
                pagada. Por eso este botón no depende del estado. */}
            <button
              onClick={handleVerComprobante}
              className="flex flex-1 sm:flex-none items-center justify-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors font-medium"
            >
              <FileText size={20} />
              Ver Comprobante
            </button>

            {venta.estado === 'PENDIENTE_PAGO' && (venta.saldoPendiente ?? 0) > 0 && onCobrarSaldo && (
              <button
                onClick={onCobrarSaldo}
                className="flex flex-1 sm:flex-none items-center justify-center gap-2 px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
              >
                <Banknote size={20} />
                Cobrar saldo pendiente
              </button>
            )}

            <button
              onClick={onClose}
              className="flex-1 basis-full sm:basis-0 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>

      {/* Modal de Comprobante */}
      {showComprobanteModal && (
        <ComprobanteModal
          isOpen={showComprobanteModal}
          onClose={() => setShowComprobanteModal(false)}
          idVenta={venta.id}
        />
      )}

      {/* Confirmar la entrega. El saldo pendiente no la bloquea: solo se avisa. */}
      {confirmarEntrega && (
        <DeleteConfirmModal
          title="Marcar como entregada"
          message={
            (venta.saldoPendiente ?? 0) > 0
              ? `Esta venta todavía tiene un saldo pendiente de ${formatPrice(venta.saldoPendiente ?? 0)}.\n¿Marcarla como entregada de todos modos?`
              : '¿Confirmar que esta venta ya fue entregada?'
          }
          confirmLabel="Marcar entregada"
          onConfirm={() => {
            setConfirmarEntrega(false);
            ejecutarAccionEntrega(() => marcarVentaEntregada(venta.id));
          }}
          onCancel={() => setConfirmarEntrega(false)}
        />
      )}

      {showDeshacerModal && (
        <DeshacerEntregaModal
          venta={venta}
          onClose={() => setShowDeshacerModal(false)}
          onHecho={(actualizada) => onVentaActualizada?.(actualizada)}
        />
      )}

      {showEditarEntregaModal && (
        <EditarEntregaModal
          venta={venta}
          onClose={() => setShowEditarEntregaModal(false)}
          onGuardado={(actualizada) => onVentaActualizada?.(actualizada)}
        />
      )}
    </>
  );
}