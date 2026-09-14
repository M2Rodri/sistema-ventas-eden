'use client';

import { useEffect, useState, useRef } from 'react';
import { X, Printer } from 'lucide-react';
import { getComprobanteByVenta, getVentaById, createComprobante } from '@/lib/api';
import type { Comprobante } from '@/lib/api';
import type { Venta } from '@/types/venta';

interface ComprobanteModalProps {
  isOpen: boolean;
  onClose: () => void;
  idVenta: number;
}

export default function ComprobanteModal({ isOpen, onClose, idVenta }: ComprobanteModalProps) {
  const [comprobante, setComprobante] = useState<Comprobante | null>(null);
  const [venta, setVenta] = useState<Venta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const printRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      loadData();
    }
  }, [isOpen, idVenta]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Intentar obtener la venta primero
      const ventaData = await getVentaById(idVenta);
      setVenta(ventaData);

      // Intentar obtener el comprobante
      try {
        const comp = await getComprobanteByVenta(idVenta);
        setComprobante(comp);
      } catch (err) {
        // Si no existe, crearlo automáticamente
        console.log('Comprobante no existe, creando...');
        const nuevoComprobante = await createComprobante({
          idVenta: idVenta,
          tipoComprobante: 'RECIBO',
          nombreCliente: ventaData.nombreCliente,
        });
        setComprobante(nuevoComprobante);
      }
    } catch (err: any) {
      setError(err.message || 'Error al cargar datos');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  // Antes esto hacía document.body.innerHTML = contenido y después
  // window.location.reload(), o sea que imprimir recargaba toda la página.
  // Ahora la hoja de estilos @media print de globals.css oculta todo salvo
  // .area-impresion, así que alcanza con pedir la impresión.
  const handleImprimir = () => {
    window.print();
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

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        
        {/* Header con botones de acción */}
        <div className="sticky top-0 bg-white border-b border-gray-200 p-4 flex items-center justify-between z-10">
          <h2 className="text-xl font-bold text-gray-900">Comprobante de Venta</h2>
          <div className="flex items-center gap-2">
            <button
              onClick={handleImprimir}
              disabled={loading || !comprobante}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Printer size={18} />
              Imprimir / Guardar PDF
            </button>
            <button
              onClick={onClose}
              className="p-2 text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X size={24} />
            </button>
          </div>
        </div>

        {/* Contenido del comprobante */}
        <div className="p-6">
          {loading && (
            <div className="flex items-center justify-center py-12">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                <p className="mt-4 text-gray-600">Cargando comprobante...</p>
              </div>
            </div>
          )}

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center">
              <p className="text-red-800 font-medium">{error}</p>
              <button
                onClick={loadData}
                className="mt-3 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
              >
                Reintentar
              </button>
            </div>
          )}

          {!loading && !error && venta && comprobante && (
            <div ref={printRef} className="bg-white area-impresion">
              {/* Encabezado de la empresa */}
              <div className="border-b-2 border-gray-300 pb-6 mb-6">
                <div className="text-center">
                  <h1 className="text-3xl font-bold text-gray-900 mb-2">MI TIENDA</h1>
                  <p className="text-gray-600 font-medium">NIT: 123456789</p>
                  <p className="text-gray-600">Dirección: Calle Principal #123, Tarija - Bolivia</p>
                  <p className="text-gray-600">Teléfono: +591 12345678</p>
                  <p className="text-gray-600">Email: contacto@mitienda.com</p>
                </div>
                <div className="mt-4 text-center">
                  <h2 className="text-2xl font-bold text-blue-600 uppercase">Comprobante de Venta</h2>
                  <p className="text-lg font-semibold text-gray-700 mt-2">{comprobante.numeroComprobante}</p>
                </div>
              </div>

              {/* Información del Cliente y Venta */}
              <div className="grid grid-cols-2 gap-6 mb-6">
                <div>
                  <h3 className="font-bold text-gray-700 mb-3 text-base border-b pb-2">Datos del Cliente:</h3>
                  <p className="mb-1 text-sm"><span className="font-semibold">Nombre:</span> {venta.nombreCliente}</p>
                  {venta.telefonoCliente && (
                    <p className="mb-1 text-sm"><span className="font-semibold">Celular:</span> {venta.telefonoCliente}</p>
                  )}
                  {!venta.esClienteRegistrado && (
                    <p className="text-xs text-blue-600 mt-2">(Cliente rápido)</p>
                  )}
                </div>
                <div className="text-right">
                  <h3 className="font-bold text-gray-700 mb-3 text-base border-b pb-2">Datos de la Venta:</h3>
                  <p className="mb-1 text-sm"><span className="font-semibold">Fecha:</span> {formatDate(venta.fechaVenta)}</p>
                  <p className="mb-1 text-sm"><span className="font-semibold">Método:</span> {getMetodoPagoLabel(venta.metodoPago ?? "")}</p>
                  <p className="mb-1 text-sm"><span className="font-semibold">Vendedor:</span> {venta.nombreUsuario || 'Sistema'}</p>
                  <p className="mb-1 text-sm"><span className="font-semibold">Venta #:</span> {venta.id}</p>
                </div>
              </div>

              {/* Detalle de Productos */}
              <div className="mb-6">
                <h3 className="font-bold text-gray-700 mb-3 text-base border-b pb-2">Detalle de Productos:</h3>
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="bg-gray-100 border-b-2 border-gray-300">
                      <th className="px-3 py-2 text-left text-xs font-bold text-gray-700">Producto</th>
                      <th className="px-3 py-2 text-center text-xs font-bold text-gray-700">Cant.</th>
                      <th className="px-3 py-2 text-right text-xs font-bold text-gray-700">P. Unit.</th>
                      <th className="px-3 py-2 text-right text-xs font-bold text-gray-700">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {venta.detalles.map((detalle, index) => (
                      <tr key={index}>
                        <td className="px-3 py-2">
                          <div className="text-sm font-medium text-gray-900">{detalle.nombreProducto}</div>
                          <div className="text-xs text-gray-500">Cód: {detalle.skuProducto}</div>
                        </td>
                        <td className="px-3 py-2 text-center text-sm font-medium text-gray-900">{detalle.cantidad}</td>
                        <td className="px-3 py-2 text-right text-sm text-gray-900">
                          {formatPrice(detalle.precioUnitario)}
                        </td>
                        <td className="px-3 py-2 text-right text-sm font-semibold text-gray-900">
                          {formatPrice(detalle.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Total */}
              <div className="border-t-2 border-gray-300 pt-4 mb-6">
                <div className="flex justify-end">
                  <div className="w-72">
                    <div className="bg-blue-50 border-2 border-blue-300 rounded-lg p-4">
                      <div className="flex justify-between items-center">
                        <span className="text-xl font-bold text-gray-900">TOTAL:</span>
                        <span className="text-3xl font-bold text-blue-600">
                          {formatPrice(venta.montoTotal)}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Observaciones */}
              {comprobante.observaciones && (
                <div className="mb-6 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <h3 className="font-bold text-gray-700 mb-2 text-sm">Observaciones:</h3>
                  <p className="text-sm text-gray-700">{comprobante.observaciones}</p>
                </div>
              )}

              {/* Pie del comprobante */}
              <div className="border-t-2 border-gray-300 pt-4 text-center">
                <p className="text-gray-700 font-medium mb-1">¡Gracias por su compra!</p>
                <p className="text-xs text-gray-500">Este documento no es válido como factura fiscal</p>
                <p className="text-xs text-gray-400 mt-1">
                  Comprobante generado: {formatDate(comprobante.fechaEmision)}
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}