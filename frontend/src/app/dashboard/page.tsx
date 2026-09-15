'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import Link from 'next/link';
import {
  UserPlus,
  PackagePlus,
  FileText,
  TrendingUp,
  Package,
  Users,
  AlertTriangle,
  Boxes,
  DollarSign,
} from 'lucide-react';
import { getDashboardEstadisticas } from '@/lib/api';
import { DashboardEstadisticas } from '@/types/dashboard';

/**
 * Panel de inicio.
 *
 * Todas las cifras salen de /api/dashboard/estadisticas. Antes esta pantalla
 * mostraba valores fijos escritos en el código ($250.000 en ventas, 1.200
 * productos, 350 usuarios, 4.8/5 en reseñas y tres productos destacados
 * inventados), que no correspondían a ningún dato del sistema.
 */
export default function DashboardPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [stats, setStats] = useState<DashboardEstadisticas | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Proteger el dashboard - solo ADMIN y EMPLEADO
  useEffect(() => {
    if (user && user.role === 'CLIENTE') {
      router.replace('/tienda');
    }
  }, [user, router]);

  useEffect(() => {
    let activo = true;
    (async () => {
      try {
        const data = await getDashboardEstadisticas();
        if (activo) setStats(data);
      } catch (err: any) {
        if (activo) setError(err?.message ?? 'No se pudieron cargar las estadísticas');
      } finally {
        if (activo) setLoading(false);
      }
    })();
    return () => {
      activo = false;
    };
  }, []);

  const bs = (monto?: number) =>
    `Bs ${Number(monto ?? 0).toLocaleString('es-BO', { minimumFractionDigits: 2 })}`;

  const num = (valor?: number) => Number(valor ?? 0).toLocaleString('es-BO');

  return (
    <div className="space-y-6">
      {/* Saludo */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">
          Bienvenido, {user?.nombre} {user?.apellido}
        </h1>
        <p className="text-gray-500 mt-1">
          Resumen del negocio al{' '}
          {new Date().toLocaleDateString('es-BO', {
            day: '2-digit',
            month: 'long',
            year: 'numeric',
          })}
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-lg bg-red-50 border border-red-200 text-red-800">
          {error}
        </div>
      )}

      {/* Tarjetas de estadísticas */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-gradient-to-br from-primary-500 to-primary-600 p-6 rounded-xl shadow-lg text-white">
          <div className="flex items-start justify-between">
            <h3 className="text-sm font-medium opacity-90">Ventas del mes</h3>
            <TrendingUp size={18} className="opacity-80" />
          </div>
          <p className="text-3xl font-bold mt-2">
            {loading ? '—' : bs(stats?.ventasStats?.montoVentasMes)}
          </p>
          <p className="text-sm mt-2 opacity-90">
            {loading ? '' : `${num(stats?.ventasStats?.totalVentasMes)} ventas registradas`}
          </p>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-md border border-gray-100">
          <div className="flex items-start justify-between">
            <h3 className="text-sm font-medium text-gray-500">Ventas de hoy</h3>
            <DollarSign size={18} className="text-gray-400" />
          </div>
          <p className="text-3xl font-bold mt-2 text-gray-900">
            {loading ? '—' : bs(stats?.ventasStats?.montoVentasHoy)}
          </p>
          <p className="text-sm mt-2 text-gray-500">
            {loading ? '' : `${num(stats?.ventasStats?.totalVentasHoy)} hoy`}
          </p>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-md border border-gray-100">
          <div className="flex items-start justify-between">
            <h3 className="text-sm font-medium text-gray-500">Productos activos</h3>
            <Package size={18} className="text-gray-400" />
          </div>
          <p className="text-3xl font-bold mt-2 text-gray-900">
            {loading ? '—' : num(stats?.productosStats?.productosActivos)}
          </p>
          <p className="text-sm mt-2 text-gray-500">
            {loading ? '' : `${num(stats?.productosStats?.totalProductos)} en el catálogo`}
          </p>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-md border border-gray-100">
          <div className="flex items-start justify-between">
            <h3 className="text-sm font-medium text-gray-500">Clientes</h3>
            <Users size={18} className="text-gray-400" />
          </div>
          <p className="text-3xl font-bold mt-2 text-gray-900">
            {loading ? '—' : num(stats?.clientesStats?.totalClientes)}
          </p>
          <p className="text-sm mt-2 text-gray-500">
            {loading ? '' : `${num(stats?.clientesStats?.clientesNuevosMes)} nuevos este mes`}
          </p>
        </div>
      </div>

      {/* Alertas de stock */}
      {!loading && (stats?.productosStats?.productosBajoStock ?? 0) > 0 && (
        <Link
          href="/dashboard/inventario"
          className="flex items-center gap-3 p-4 rounded-xl bg-amber-50 border border-amber-200 hover:bg-amber-100 transition-colors"
        >
          <AlertTriangle className="text-amber-600 flex-shrink-0" size={22} />
          <div>
            <p className="font-medium text-amber-900">
              {num(stats?.productosStats?.productosBajoStock)} producto(s) por debajo del stock mínimo
            </p>
            <p className="text-sm text-amber-800">
              {num(stats?.productosStats?.productosSinStock)} sin stock · Valor del inventario:{' '}
              {bs(stats?.inventarioStats?.valorTotalInventario)}
            </p>
          </div>
        </Link>
      )}

      {/* Acciones Rápidas */}
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-4">Acciones Rápidas</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {user?.role === 'ADMIN' && (
            <Link
              href="/dashboard/productos"
              className="flex items-center justify-center gap-2 bg-gray-900 text-white px-6 py-4 rounded-xl hover:bg-gray-800 transition-colors font-medium text-sm shadow-md"
            >
              <PackagePlus size={18} />
              Nuevo producto
            </Link>
          )}

          <Link
            href="/dashboard/inventario"
            className="flex items-center justify-center gap-2 bg-white border border-gray-200 text-gray-700 px-6 py-4 rounded-xl hover:bg-gray-50 transition-colors font-medium text-sm shadow-sm"
          >
            <Boxes size={18} />
            Ver inventario
          </Link>

          <Link
            href="/dashboard/ventas"
            className="flex items-center justify-center gap-2 bg-white border border-gray-200 text-gray-700 px-6 py-4 rounded-xl hover:bg-gray-50 transition-colors font-medium text-sm shadow-sm"
          >
            <DollarSign size={18} />
            Registrar venta
          </Link>

          {user?.role === 'ADMIN' && (
            <Link
              href="/dashboard/reportes"
              className="flex items-center justify-center gap-2 bg-white border border-gray-200 text-gray-700 px-6 py-4 rounded-xl hover:bg-gray-50 transition-colors font-medium text-sm shadow-sm"
            >
              <FileText size={18} />
              Ver reportes
            </Link>
          )}
        </div>
      </div>

      {/* Productos más vendidos */}
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-4">Productos más vendidos</h2>

        {loading ? (
          <div className="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-500">
            Cargando…
          </div>
        ) : !stats?.productosMasVendidos || stats.productosMasVendidos.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 p-8 text-center">
            <Package size={40} className="mx-auto text-gray-300 mb-3" />
            <p className="text-gray-600 font-medium">Todavía no hay ventas registradas</p>
            <p className="text-sm text-gray-500 mt-1">
              Cuando registres ventas, acá vas a ver qué productos se venden más.
            </p>
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Producto</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Unidades</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Monto</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {stats.productosMasVendidos.map((p) => (
                    <tr key={p.idProducto} className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm font-medium text-gray-900">{p.nombreProducto}</td>
                      <td className="px-6 py-4 text-sm text-gray-500 font-mono">{p.skuProducto}</td>
                      <td className="px-6 py-4 text-sm text-right text-gray-900">{num(p.cantidadVendida)}</td>
                      <td className="px-6 py-4 text-sm text-right font-semibold text-gray-900">{bs(p.montoTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
