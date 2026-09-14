'use client';

import { useEffect, useState, Suspense } from 'react';
import { useParams, useSearchParams } from 'next/navigation';
import { getComprobanteByVenta, getVentaById } from '@/lib/api';
import type { Comprobante } from '@/lib/api';
import type { Venta } from '@/types/venta';
import { Printer, ArrowLeft, FileText } from 'lucide-react';

function ComprobanteContent() {
  const params = useParams();
  const searchParams = useSearchParams();
  const [comprobante, setComprobante] = useState<Comprobante | null>(null);
  const [venta, setVenta] = useState<Venta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Detectar si se debe imprimir automáticamente
  const autoPrint = searchParams.get('print') === 'true';

  useEffect(() => {
    loadData();
  }, [params.id]);

  useEffect(() => {
    // Imprimir automáticamente si se especificó en la URL
    if (autoPrint && !loading && venta && comprobante) {
      setTimeout(() => {
        window.print();
      }, 500);
    }
  }, [autoPrint, loading, venta, comprobante]);

  const loadData = async () => {
    try {
      const idVenta = Number(params.id);
      
      if (isNaN(idVenta)) {
        throw new Error('ID de venta inválido');
      }

      // Promise.all a proposito, no allSettled: un comprobante sin su venta
      // (o al reves) seria un documento incompleto. Aca es preferible fallar
      // entero y mostrar el error que emitir un comprobante a medias.
      const [comp, ventaData] = await Promise.all([
        getComprobanteByVenta(idVenta),
        getVentaById(idVenta)
      ]);
      
      setComprobante(comp);
      setVenta(ventaData);
    } catch (err: any) {
      setError(err.message || 'Error al cargar comprobante');
      console.error('Error al cargar comprobante:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleImprimir = () => {
    window.print();
  };

  const handleVolver = () => {
    window.close();
  };

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

  const getMetodoPagoLabel = (metodo: string) => {
    const labels: Record<string, string> = {
      EFECTIVO: 'Efectivo',
      TARJETA: 'Tarjeta',
      TRANSFERENCIA: 'Transferencia',
      QR: 'QR',
    };
    return labels[metodo] || metodo;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 font-medium">Cargando comprobante...</p>
        </div>
      </div>
    );
  }

  if (error || !comprobante || !venta) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <div className="text-center bg-white p-8 rounded-lg shadow-lg">
          <FileText className="mx-auto text-red-500 mb-4" size={64} />
          <h2 className="text-2xl font-bold text-red-600 mb-2">Comprobante no encontrado</h2>
          <p className="text-gray-600 mb-6">{error || 'No se pudo cargar el comprobante'}</p>
          <button
            onClick={handleVolver}
            className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
          >
            Cerrar ventana
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 py-8">
      <div className="max-w-4xl mx-auto px-4">
        
        {/* Botones de acción (no se imprimen) */}
        <div className="mb-6 flex gap-3 print:hidden">
          <button
            onClick={handleImprimir}
            className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium shadow-lg"
          >
            <Printer size={20} />
            Imprimir Comprobante
          </button>
          <button
            onClick={handleVolver}
            className="flex items-center gap-2 px-6 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors font-medium shadow-lg"
          >
            <ArrowLeft size={20} />
            Cerrar
          </button>
        </div>

        {/* Comprobante para imprimir */}
        <div className="bg-white shadow-2xl rounded-lg p-8 print:shadow-none print:rounded-none" id="comprobante-print">
          
          {/* Encabezado de la empresa */}
          <div className="border-b-2 border-gray-300 pb-6 mb-6">
            <div className="text-center">
              <h1 className="text-4xl font-bold text-gray-900 mb-2">MI TIENDA</h1>
              <p className="text-gray-600 font-medium">NIT: 123456789</p>
              <p className="text-gray-600">Dirección: Calle Principal #123, Tarija - Bolivia</p>
              <p className="text-gray-600">Teléfono: +591 12345678</p>
              <p className="text-gray-600">Email: contacto@mitienda.com</p>
            </div>
            <div className="mt-6 text-center">
              <h2 className="text-2xl font-bold text-blue-600 uppercase">Comprobante de Venta</h2>
              <p className="text-xl font-semibold text-gray-700 mt-2">{comprobante.numeroComprobante}</p>
              {comprobante.anulado && (
                <div className="mt-2">
                  <span className="px-4 py-2 bg-red-100 text-red-800 font-bold rounded-lg">
                    ANULADO
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Información del Cliente y Venta */}
          <div className="grid grid-cols-2 gap-8 mb-8">
            <div>
              <h3 className="font-bold text-gray-700 mb-3 text-lg border-b pb-2">Datos del Cliente:</h3>
              <p className="mb-1"><span className="font-semibold">Nombre:</span> {venta.nombreCliente}</p>
              {venta.telefonoCliente && (
                <p className="mb-1"><span className="font-semibold">Celular:</span> {venta.telefonoCliente}</p>
              )}
              {!venta.esClienteRegistrado && (
                <p className="text-sm text-blue-600 mt-2">(Cliente rápido)</p>
              )}
            </div>
            <div className="text-right">
              <h3 className="font-bold text-gray-700 mb-3 text-lg border-b pb-2">Datos de la Venta:</h3>
              <p className="mb-1"><span className="font-semibold">Fecha:</span> {formatDate(venta.fechaVenta)}</p>
              <p className="mb-1"><span className="font-semibold">Método de Pago:</span> {getMetodoPagoLabel(venta.metodoPago ?? "")}</p>
              <p className="mb-1"><span className="font-semibold">Vendedor:</span> {venta.nombreUsuario || 'Sistema'}</p>
              <p className="mb-1"><span className="font-semibold">Venta #:</span> {venta.id}</p>
            </div>
          </div>

          {/* Detalle de Productos */}
          <div className="mb-8">
            <h3 className="font-bold text-gray-700 mb-4 text-lg border-b pb-2">Detalle de Productos:</h3>
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-gray-100 border-b-2 border-gray-300">
                  <th className="px-4 py-3 text-left text-sm font-bold text-gray-700">Producto</th>
                  <th className="px-4 py-3 text-center text-sm font-bold text-gray-700">Cant.</th>
                  <th className="px-4 py-3 text-right text-sm font-bold text-gray-700">P. Unitario</th>
                  <th className="px-4 py-3 text-right text-sm font-bold text-gray-700">Subtotal</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {venta.detalles.map((detalle, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="font-medium text-gray-900">{detalle.nombreProducto}</div>
                      <div className="text-xs text-gray-500">Código: {detalle.skuProducto}</div>
                    </td>
                    <td className="px-4 py-3 text-center font-medium text-gray-900">{detalle.cantidad}</td>
                    <td className="px-4 py-3 text-right text-gray-900">
                      {formatPrice(detalle.precioUnitario)}
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-gray-900">
                      {formatPrice(detalle.subtotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Total */}
          <div className="border-t-2 border-gray-300 pt-6 mb-8">
            <div className="flex justify-end">
              <div className="w-80">
                <div className="bg-blue-50 border-2 border-blue-300 rounded-lg p-6">
                  <div className="flex justify-between items-center">
                    <span className="text-2xl font-bold text-gray-900">TOTAL A PAGAR:</span>
                    <span className="text-4xl font-bold text-blue-600">
                      {formatPrice(venta.montoTotal)}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Observaciones (si existen) */}
          {comprobante.observaciones && (
            <div className="mb-8 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <h3 className="font-bold text-gray-700 mb-2">Observaciones:</h3>
              <p className="text-gray-700">{comprobante.observaciones}</p>
            </div>
          )}

          {/* Pie del comprobante */}
          <div className="border-t-2 border-gray-300 pt-6 text-center">
            <p className="text-gray-700 font-medium mb-2">¡Gracias por su compra!</p>
            <p className="text-sm text-gray-500 mb-1">Este documento no es válido como factura fiscal</p>
            <p className="text-xs text-gray-400">Comprobante generado electrónicamente - {formatDate(comprobante.fechaEmision)}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ComprobantePage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 font-medium">Cargando comprobante...</p>
        </div>
      </div>
    }>
      <ComprobanteContent />
    </Suspense>
  );
}
