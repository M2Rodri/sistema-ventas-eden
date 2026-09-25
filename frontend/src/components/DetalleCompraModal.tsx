'use client';

import { useRef, useState } from 'react';
import { X, Building2, Calendar, DollarSign, Package, FileText, User, Printer, Share2 } from 'lucide-react';
import { Compra } from '@/types/proveedor';

interface DetalleCompraModalProps {
  compra: Compra;
  onClose: () => void;
}

/**
 * Detalle de una compra, con lo mismo que ya tiene el comprobante de venta:
 * imprimir/guardar PDF y compartir. Antes esta información solo vivía en
 * pantalla — para cotejarla contra la factura física del proveedor hace
 * falta poder sacarla de acá.
 */
export default function DetalleCompraModal({ compra, onClose }: DetalleCompraModalProps) {
  const printRef = useRef<HTMLDivElement>(null);
  const [compartiendo, setCompartiendo] = useState(false);
  const [avisoCompartir, setAvisoCompartir] = useState<string | null>(null);

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
    });
  };

  const getEstadoBadge = () => {
    const colors: Record<string, string> = {
      POR_CONFIRMAR: 'bg-yellow-100 text-yellow-800',
      CONFIRMADA: 'bg-green-100 text-green-800',
      CANCELADA: 'bg-red-100 text-red-800',
    };
    const labels: Record<string, string> = {
      POR_CONFIRMAR: 'Por confirmar',
      CONFIRMADA: 'Confirmada',
      CANCELADA: 'Cancelada',
    };
    return {
      color: colors[compra.estado] || 'bg-gray-100 text-gray-800',
      label: labels[compra.estado] || compra.estado
    };
  };

  const estadoBadge = getEstadoBadge();

  // Misma mecánica que el comprobante de venta: .area-impresion (definida en
  // globals.css) oculta todo lo demás de la pantalla al imprimir, así que
  // alcanza con pedir la impresión — no hace falta armar una vista aparte.
  const handleImprimir = () => {
    window.print();
  };

  const handleCompartir = async () => {
    if (!printRef.current) return;

    setCompartiendo(true);
    setAvisoCompartir(null);

    try {
      const html2pdf = (await import('html2pdf.js')).default;
      const nombreArchivo = `Compra-${compra.id}.pdf`;

      const blob: Blob = await html2pdf()
        .from(printRef.current)
        .set({
          margin: 10,
          filename: nombreArchivo,
          html2canvas: { scale: 2 },
          jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
        })
        .outputPdf('blob');

      const archivo = new File([blob], nombreArchivo, { type: 'application/pdf' });

      if (navigator.canShare && navigator.canShare({ files: [archivo] })) {
        await navigator.share({
          files: [archivo],
          title: `Compra #${compra.id}`,
          text: `Compra #${compra.id} a ${compra.nombreProveedor}`,
        });
      } else {
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = nombreArchivo;
        document.body.appendChild(enlace);
        enlace.click();
        document.body.removeChild(enlace);
        URL.revokeObjectURL(url);
        setAvisoCompartir(
          'Este navegador no permite compartir el archivo directo: se descargó el PDF para adjuntarlo a mano.',
        );
      }
    } catch (err: any) {
      if (err?.name !== 'AbortError') {
        setAvisoCompartir(err.message || 'Error al generar o compartir el PDF');
      }
    } finally {
      setCompartiendo(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 print:static print:block print:p-0 print:bg-white">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto print:max-h-none print:overflow-visible print:static">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 sticky top-0 z-10">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Detalle de Compra</h2>
            <p className="text-sm text-gray-600 mt-1">
              ID: #{compra.id}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handleImprimir}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              <Printer size={18} />
              Imprimir / Guardar PDF
            </button>
            <button
              onClick={handleCompartir}
              disabled={compartiendo}
              className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Share2 size={18} />
              {compartiendo ? 'Generando...' : 'Compartir PDF'}
            </button>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X size={24} />
            </button>
          </div>
        </div>

        {avisoCompartir && (
          <div className="mx-6 mt-6 p-3 bg-amber-50 border border-amber-200 rounded-lg text-sm text-amber-800">
            {avisoCompartir}
          </div>
        )}

        <div ref={printRef} className="area-impresion">
          {/* Encabezado del negocio, solo visible al imprimir/exportar —
              en pantalla ya está el header de arriba con los botones. */}
          <div className="hidden print:block px-6 pt-6 border-b-2 border-gray-300 pb-6 mb-2 text-center">
            <h1 className="text-2xl font-bold text-gray-900 mb-1">MI TIENDA</h1>
            <p className="text-gray-600 text-sm">NIT: 123456789</p>
            <p className="text-gray-600 text-sm">Dirección: Calle Principal #123, Tarija - Bolivia</p>
            <p className="text-gray-600 text-sm">Teléfono: +591 12345678</p>
            <h2 className="text-xl font-bold text-blue-600 uppercase mt-4">Registro de Compra</h2>
            <p className="text-base font-semibold text-gray-700 mt-1">Compra #{compra.id}</p>
          </div>

          {/* Información General */}
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Proveedor */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <Building2 className="text-secondary-600" size={20} />
                <h3 className="font-semibold text-gray-900">Proveedor</h3>
              </div>
              <p className="text-gray-900 font-medium">{compra.nombreProveedor}</p>
              <p className="text-sm text-gray-600">NIT: {compra.nitProveedor}</p>
            </div>

            {/* Usuario: quién lo cargó en el sistema. Útil en pantalla para
                trazabilidad interna, pero no sirve para cotejar contra la
                factura del proveedor (que nunca lo va a tener) — por eso no
                sale en la versión impresa. */}
            <div className="bg-gray-50 p-4 rounded-lg print:hidden">
              <div className="flex items-center gap-2 mb-3">
                <User className="text-secondary-600" size={20} />
                <h3 className="font-semibold text-gray-900">Registrado por</h3>
              </div>
              <p className="text-gray-900 font-medium">{compra.nombreUsuario}</p>
            </div>

            {/* Fecha */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <Calendar className="text-secondary-600" size={20} />
                <h3 className="font-semibold text-gray-900">Fecha de Compra</h3>
              </div>
              <p className="text-gray-900">{formatDate(compra.fechaCompra)}</p>
            </div>

            {/* Número de Factura: respaldo legal del gasto. */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <FileText className="text-secondary-600" size={20} />
                <h3 className="font-semibold text-gray-900">Número de Factura</h3>
              </div>
              <p className="text-gray-900">{compra.numeroFactura || 'No registrado'}</p>
            </div>

            {/* Costo Total */}
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center gap-2 mb-3">
                <DollarSign className="text-secondary-600" size={20} />
                <h3 className="font-semibold text-gray-900">Costo Total</h3>
              </div>
              <p className="text-2xl font-bold text-secondary-600">{formatPrice(compra.montoTotal)}</p>
            </div>
          </div>

          {/* Estado: dato del flujo interno del sistema, no algo que
              aparezca en la factura del proveedor. Solo en pantalla. */}
          <div className="px-6 pb-6 print:hidden">
            <div className="bg-gray-50 p-4 rounded-lg">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">Estado de la Compra:</span>
                <span className={`px-4 py-1.5 rounded-full text-sm font-semibold ${estadoBadge.color}`}>
                  {estadoBadge.label}
                </span>
              </div>
            </div>
          </div>

          {/* Detalle de Productos */}
          <div className="px-6 pb-6">
            <div className="flex items-center gap-2 mb-4">
              <Package className="text-secondary-600" size={20} />
              <h3 className="font-semibold text-gray-900">Productos Comprados</h3>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                    <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Precio Unit.</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Subtotal</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {compra.detalles.map((detalle, index) => (
                    <tr key={index}>
                      <td className="px-4 py-3">
                        <div className="text-sm font-medium text-gray-900">{detalle.nombreProducto}</div>
                        <div className="text-xs text-gray-500">{detalle.skuProducto}</div>
                      </td>
                      <td className="px-4 py-3 text-center text-sm text-gray-900">{detalle.cantidad}</td>
                      <td className="px-4 py-3 text-right text-sm text-gray-900">{formatPrice(detalle.precioUnitario)}</td>
                      <td className="px-4 py-3 text-right text-sm font-semibold text-gray-900">
                        {formatPrice(detalle.subtotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Total */}
          <div className="px-6 pb-6">
            <div className="bg-secondary-50 border-2 border-secondary-200 p-4 rounded-lg">
              <div className="flex justify-between items-center">
                <span className="text-lg font-bold text-gray-900">COSTO TOTAL:</span>
                <span className="text-3xl font-bold text-secondary-600">{formatPrice(compra.montoTotal)}</span>
              </div>
            </div>
          </div>

          {/* Notas */}
          {compra.notas && (
            <div className="px-6 pb-6">
              <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg">
                <div className="flex items-center gap-2 mb-2">
                  <FileText className="text-yellow-600" size={18} />
                  <h4 className="font-semibold text-gray-900">Notas / Observaciones:</h4>
                </div>
                <p className="text-sm text-gray-700">{compra.notas}</p>
              </div>
            </div>
          )}

          {/* Información de Actualización: dato técnico de sistema, no de
              negocio. Solo en pantalla. */}
          <div className="px-6 pb-6 print:hidden">
            <div className="bg-gray-50 p-3 rounded-lg">
              <p className="text-xs text-gray-600">
                Última actualización: {formatDate(compra.fechaActualizacion)}
              </p>
            </div>
          </div>
        </div>

        {/* Botón */}
        <div className="flex gap-3 p-6 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
