'use client';
import { useState, useEffect } from 'react';
import { ArrowLeft, Loader2, TrendingUp } from 'lucide-react';
import { TipoReporte } from '@/types/reporte';
import {
  getReporteVentas,
  getReporteProductosMasVendidos,
  getReporteClientesFrecuentes,
  getReporteInventarioValorizado,
  getReporteVentasPorCategoria,
  getReporteVentasPorMetodoPago,
  getReporteInventarioStockBajo,
  getReporteProveedores,
  getReporteTransportadoras,
  getReporteFinanciero,
} from '@/lib/api';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
} from 'recharts';

interface ReporteVistaPreviaProps {
  tipoReporte: TipoReporte;
  fechaInicio: string;
  fechaFin: string;
  limite: number;
  onVolver: () => void;
}

const COLORS = ['#00a0a0', '#aa8f67', '#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D'];

export default function ReporteVistaPrevia({
  tipoReporte,
  fechaInicio,
  fechaFin,
  limite,
  onVolver,
}: ReporteVistaPreviaProps) {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    cargarDatos();
  }, []);

  const cargarDatos = async () => {
    try {
      setLoading(true);
      setError(null);
      // Función auxiliar para ajustar la fecha final
      // Formato ISO sin Z (hora local del servidor/BD)
      const toLocalISOString = (dateStr: string): string => {
        // Ej: "2025-11-16" → "2025-11-16T00:00:00"
        return dateStr + 'T00:00:00';
      };

      const toEndOfDayLocal = (dateStr: string): string => {
        // Ej: "2025-11-16" → "2025-11-16T23:59:59.999"
        return dateStr + 'T23:59:59.999';
      };

      const inicio = fechaInicio ? toLocalISOString(fechaInicio) : '';
      const fin = fechaFin ? toEndOfDayLocal(fechaFin) : '';
      let resultado;

      switch (tipoReporte) {
        case 'VENTAS':
          resultado = await getReporteVentas(inicio, fin);
          break;
        case 'PRODUCTOS_MAS_VENDIDOS':
          resultado = await getReporteProductosMasVendidos(limite);
          break;
        case 'CLIENTES_FRECUENTES':
          resultado = await getReporteClientesFrecuentes(limite);
          break;
        case 'INVENTARIO_VALORIZADO':
          resultado = await getReporteInventarioValorizado();
          break;
        case 'VENTAS_POR_CATEGORIA':
          resultado = await getReporteVentasPorCategoria(inicio, fin);
          break;
        case 'VENTAS_POR_METODO_PAGO':
          resultado = await getReporteVentasPorMetodoPago(inicio, fin);
          break;
        case 'INVENTARIO_STOCK_BAJO':
          resultado = await getReporteInventarioStockBajo();
          break;
        case 'PROVEEDORES':
          resultado = await getReporteProveedores();
          break;
        case 'TRANSPORTADORAS':
          resultado = await getReporteTransportadoras();
          break;
        case 'FINANCIERO':
          resultado = await getReporteFinanciero(inicio, fin);
          break;
        default:
          throw new Error('Tipo de reporte no soportado');
      }
      setData(resultado);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('es-BO', {
      style: 'currency',
      currency: 'BOB',
    }).format(price);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <Loader2 size={48} className="animate-spin text-primary-600 mb-4" />
        <p className="text-gray-600">Generando reporte...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <div className="text-red-600 mb-4">❌ Error al generar reporte</div>
        <p className="text-gray-600 mb-6">{error}</p>
        <button
          onClick={onVolver}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
        >
          Volver
        </button>
      </div>
    );
  }

  return (
    <div>
      {/* Botón Volver */}
      <button
        onClick={onVolver}
        className="flex items-center gap-2 text-primary-600 hover:text-primary-700 mb-6 font-medium"
      >
        <ArrowLeft size={20} />
        Volver a parámetros
      </button>

      {/* Contenido del Reporte */}
      <div className="space-y-6">
        {/* REPORTE DE VENTAS */}
        {tipoReporte === 'VENTAS' && data && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-700 mb-1">Total Ventas</p>
                <p className="text-2xl font-bold text-blue-900">{data.totalVentas}</p>
              </div>
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-sm text-green-700 mb-1">Monto Total</p>
                <p className="text-2xl font-bold text-green-900">{formatPrice(data.montoTotalVentas)}</p>
              </div>
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
                <p className="text-sm text-purple-700 mb-1">Ticket Promedio</p>
                <p className="text-2xl font-bold text-purple-900">{formatPrice(data.ticketPromedio)}</p>
              </div>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Fecha</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cliente</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Monto</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Método Pago</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Estado</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.ventas.map((venta: any) => (
                    <tr key={venta.idVenta}>
                      <td className="px-4 py-3 text-sm">#{venta.idVenta}</td>
                      <td className="px-4 py-3 text-sm">{formatDate(venta.fechaVenta)}</td>
                      <td className="px-4 py-3 text-sm">{venta.nombreCliente}</td>
                      <td className="px-4 py-3 text-sm font-semibold">{formatPrice(venta.montoTotal)}</td>
                      <td className="px-4 py-3 text-sm">{venta.metodoPago}</td>
                      <td className="px-4 py-3 text-sm">
                        <span className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full">
                          {venta.estado}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE DE PRODUCTOS MÁS VENDIDOS */}
        {tipoReporte === 'PRODUCTOS_MAS_VENDIDOS' && data && (
          <>
            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Top {limite} Productos</h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={data.productos}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="nombreProducto" angle={-45} textAnchor="end" height={100} />
                  <YAxis />
                  <Tooltip formatter={(value: any) => [value, 'Cantidad']} />
                  <Legend />
                  <Bar dataKey="cantidadVendida" fill="#00a0a0" name="Cantidad Vendida" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Categoría</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Monto Total</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.productos.map((producto: any) => (
                    <tr key={producto.idProducto}>
                      <td className="px-4 py-3 text-sm font-medium">{producto.nombreProducto}</td>
                      <td className="px-4 py-3 text-sm font-mono">{producto.skuProducto}</td>
                      <td className="px-4 py-3 text-sm">{producto.categoria}</td>
                      <td className="px-4 py-3 text-sm font-semibold text-primary-600">{producto.cantidadVendida}</td>
                      <td className="px-4 py-3 text-sm font-semibold">{formatPrice(producto.montoTotal)}</td>
                      <td className="px-4 py-3 text-sm">{producto.stockActual}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE DE CLIENTES FRECUENTES */}
        {tipoReporte === 'CLIENTES_FRECUENTES' && data && (
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cliente</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Contacto</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Compras</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Monto Total</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Promedio</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.clientes.map((cliente: any) => (
                  <tr key={cliente.idCliente}>
                    <td className="px-4 py-3 text-sm font-medium">{cliente.nombreCliente}</td>
                    <td className="px-4 py-3 text-sm">
                      <div>{cliente.celular}</div>
                      <div className="text-xs text-gray-500">{cliente.correo}</div>
                    </td>
                    <td className="px-4 py-3 text-sm font-semibold text-primary-600">{cliente.cantidadCompras}</td>
                    <td className="px-4 py-3 text-sm font-semibold">{formatPrice(cliente.montoTotalCompras)}</td>
                    <td className="px-4 py-3 text-sm">{formatPrice(cliente.promedioCompra)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* REPORTE DE INVENTARIO VALORIZADO */}
        {tipoReporte === 'INVENTARIO_VALORIZADO' && data && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-sm text-green-700 mb-1">Valor Total Inventario</p>
                <p className="text-2xl font-bold text-green-900">{formatPrice(data.valorTotal)}</p>
              </div>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-700 mb-1">Total Productos</p>
                <p className="text-2xl font-bold text-blue-900">{data.totalProductos}</p>
              </div>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cantidad</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Precio Unit.</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Valor Total</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ubicación</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.inventarios.map((item: any) => (
                    <tr key={item.idProducto}>
                      <td className="px-4 py-3 text-sm font-medium">{item.nombreProducto}</td>
                      <td className="px-4 py-3 text-sm font-mono">{item.skuProducto}</td>
                      <td className="px-4 py-3 text-sm">{item.cantidadDisponible}</td>
                      <td className="px-4 py-3 text-sm">{formatPrice(item.precioUnitario)}</td>
                      <td className="px-4 py-3 text-sm font-semibold text-primary-600">{formatPrice(item.valorTotal)}</td>
                      <td className="px-4 py-3 text-sm">{item.ubicacion || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE DE VENTAS POR CATEGORÍA */}
        {tipoReporte === 'VENTAS_POR_CATEGORIA' && data && (
          <>
            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Distribución por Categoría</h3>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={data}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ payload }) => {
                      // Forzamos que payload tenga 'categoria'
                      const cat = (payload as any)?.categoria;
                      return cat ? String(cat) : '';
                    }}
                    outerRadius={100}
                    fill="#8884d8"
                    dataKey="montoTotal"
                  >
                    {data.map((entry: any, index: number) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatPrice(Number(value))} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Categoría</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cantidad Vendida</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Monto Total</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.map((item: any, index: number) => (
                    <tr key={index}>
                      <td className="px-4 py-3 text-sm font-medium">{item.categoria || '—'}</td>
                      <td className="px-4 py-3 text-sm font-semibold text-primary-600">{item.cantidadVendida || 0}</td>
                      <td className="px-4 py-3 text-sm font-semibold">
                        {item.montoTotal ? formatPrice(item.montoTotal) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE DE VENTAS POR MÉTODO DE PAGO */}
        {tipoReporte === 'VENTAS_POR_METODO_PAGO' && data && (
          <>
            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Ventas por Método de Pago</h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={data}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="metodoPago" />
                  <YAxis />
                  <Tooltip formatter={(value: any) => formatPrice(value)} />
                  <Legend />
                  <Bar dataKey="montoTotal" fill="#00C49F" name="Monto Total" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Método de Pago</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ventas</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Monto Total</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.map((item: any, index: number) => (
                    <tr key={index}>
                      <td className="px-4 py-3 text-sm font-medium">{item.metodoPago}</td>
                      <td className="px-4 py-3 text-sm font-semibold text-primary-600">{item.cantidadVentas}</td>
                      <td className="px-4 py-3 text-sm font-semibold">{formatPrice(item.montoTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE DE INVENTARIO STOCK BAJO */}
        {tipoReporte === 'INVENTARIO_STOCK_BAJO' && data && (
          <div className="border border-gray-200 rounded-lg overflow-hidden">
            <div className="bg-yellow-50 border-b border-yellow-200 px-4 py-3">
              <p className="text-sm text-yellow-700">
                <strong>{data.totalProductos}</strong> productos con stock por debajo del mínimo
              </p>
            </div>
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock Actual</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock Mínimo</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Faltante</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.productos.map((item: any) => (
                  <tr key={item.idProducto}>
                    <td className="px-4 py-3 text-sm font-medium">{item.nombreProducto}</td>
                    <td className="px-4 py-3 text-sm font-mono">{item.skuProducto}</td>
                    <td className="px-4 py-3 text-sm text-red-600 font-semibold">{item.cantidadDisponible}</td>
                    <td className="px-4 py-3 text-sm">{item.stockMinimo}</td>
                    <td className="px-4 py-3 text-sm font-semibold text-red-600">-{item.diferencia}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* REPORTE DE PROVEEDORES */}
        {tipoReporte === 'PROVEEDORES' && data && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                <p className="text-sm text-orange-700 mb-1">Total Proveedores</p>
                <p className="text-2xl font-bold text-orange-900">{data.totalProveedores}</p>
              </div>
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-sm text-green-700 mb-1">Proveedores Activos</p>
                <p className="text-2xl font-bold text-green-900">{data.proveedoresActivos}</p>
              </div>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-700 mb-1">Total Compras</p>
                <p className="text-2xl font-bold text-blue-900">{formatPrice(data.montoTotalCompras)}</p>
              </div>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Proveedor</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">NIT</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Compras</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Monto Total</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Última Compra</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Activo</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.proveedores.map((prov: any) => (
                    <tr key={prov.idProveedor}>
                      <td className="px-4 py-3 text-sm font-medium">{prov.nombreProveedor}</td>
                      <td className="px-4 py-3 text-sm font-mono">{prov.nit}</td>
                      <td className="px-4 py-3 text-sm">{prov.cantidadCompras}</td>
                      <td className="px-4 py-3 text-sm font-semibold">{formatPrice(prov.montoTotalCompras)}</td>
                      <td className="px-4 py-3 text-sm">
                        {prov.ultimaCompra ? formatDate(prov.ultimaCompra) : '—'}
                      </td>
                      <td className="px-4 py-3 text-sm">
                        <span className={`px-2 py-1 text-xs rounded-full ${
                          prov.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}>
                          {prov.activo ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE DE TRANSPORTADORAS */}
        {tipoReporte === 'TRANSPORTADORAS' && data && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-teal-50 border border-teal-200 rounded-lg p-4">
                <p className="text-sm text-teal-700 mb-1">Total Transportadoras</p>
                <p className="text-2xl font-bold text-teal-900">{data.totalTransportadoras}</p>
              </div>
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-sm text-green-700 mb-1">Activas</p>
                <p className="text-2xl font-bold text-green-900">{data.transportadorasActivas}</p>
              </div>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-700 mb-1">Total Envíos</p>
                <p className="text-2xl font-bold text-blue-900">{data.totalEnvios}</p>
              </div>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Transportadora</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Envíos</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Entregados</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Pendientes</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tasa Entrega (%)</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Activo</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {data.transportadoras.map((t: any) => (
                    <tr key={t.idTransportadora}>
                      <td className="px-4 py-3 text-sm font-medium">{t.nombreTransportadora}</td>
                      <td className="px-4 py-3 text-sm">{t.cantidadEnvios}</td>
                      <td className="px-4 py-3 text-sm text-green-600">{t.enviosEntregados}</td>
                      <td className="px-4 py-3 text-sm text-orange-600">{t.enviosPendientes}</td>
                      <td className="px-4 py-3 text-sm font-semibold">
                        {t.tasaEntrega.toFixed(1)}%
                      </td>
                      <td className="px-4 py-3 text-sm">
                        <span className={`px-2 py-1 text-xs rounded-full ${
                          t.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}>
                          {t.activo ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* REPORTE FINANCIERO */}
        {tipoReporte === 'FINANCIERO' && data && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-sm text-green-700 mb-1">Ingresos Totales</p>
                <p className="text-2xl font-bold text-green-900">{formatPrice(data.ingresosTotales)}</p>
              </div>
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <p className="text-sm text-red-700 mb-1">Gastos Totales</p>
                <p className="text-2xl font-bold text-red-900">{formatPrice(data.gastosTotales)}</p>
              </div>
              <div className={`border rounded-lg p-4 ${
                data.gananciaNeta >= 0 ? 'bg-blue-50 border-blue-200' : 'bg-red-50 border-red-200'
              }`}>
                <p className="text-sm mb-1">Ganancia Neta</p>
                <p className={`text-2xl font-bold ${
                  data.gananciaNeta >= 0 ? 'text-blue-900' : 'text-red-900'
                }`}>
                  {formatPrice(data.gananciaNeta)}
                </p>
              </div>
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
                <p className="text-sm text-purple-700 mb-1">Margen de Ganancia</p>
                <p className="text-2xl font-bold text-purple-900">{data.margenGanancia.toFixed(2)}%</p>
              </div>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Evolución Financiera</h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart
                  data={[
                    ...data.detalleIngresos.map((i: any) => ({ ...i, tipo: 'ingreso' })),
                    ...data.detalleGastos.map((g: any) => ({ ...g, tipo: 'gasto' }))
                  ].reduce((acc: any[], curr) => {
                    const existing = acc.find((item: any) => item.fecha === curr.fecha);
                    if (existing) {
                      if (curr.tipo === 'ingreso') existing.ingreso = curr.monto;
                      if (curr.tipo === 'gasto') existing.gasto = curr.monto;
                    } else {
                      acc.push({
                        fecha: curr.fecha,
                        ingreso: curr.tipo === 'ingreso' ? curr.monto : 0,
                        gasto: curr.tipo === 'gasto' ? curr.monto : 0,
                      });
                    }
                    return acc;
                  }, []).sort((a: any, b: any) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime())}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="fecha" />
                  <YAxis />
                  <Tooltip formatter={(value: any) => formatPrice(value)} />
                  <Legend />
                  <Line type="monotone" dataKey="ingreso" stroke="#00C49F" name="Ingresos" />
                  <Line type="monotone" dataKey="gasto" stroke="#FF8042" name="Gastos" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </>
        )}

        {/* Mensaje genérico para reportes sin vista específica (por si acaso) */}
        {![
          'VENTAS',
          'PRODUCTOS_MAS_VENDIDOS',
          'CLIENTES_FRECUENTES',
          'INVENTARIO_VALORIZADO',
          'VENTAS_POR_CATEGORIA',
          'VENTAS_POR_METODO_PAGO',
          'INVENTARIO_STOCK_BAJO',
          'PROVEEDORES',
          'TRANSPORTADORAS',
          'FINANCIERO'
        ].includes(tipoReporte) && (
          <div className="text-center py-12 bg-gray-50 rounded-lg border border-gray-200">
            <TrendingUp size={48} className="mx-auto text-gray-400 mb-4" />
            <p className="text-gray-600">Vista previa no disponible para este tipo de reporte.</p>
          </div>
        )}
      </div>
    </div>
  );
}