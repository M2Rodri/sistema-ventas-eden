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
  X,
} from 'lucide-react';
import { getDashboardEstadisticas } from '@/lib/api';
import { DashboardEstadisticas } from '@/types/dashboard';
import StatCard from '@/components/StatCard';

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
        <div className="p-4 rounded-lg bg-red-50 border border-red-200 text-red-800 flex items-start justify-between gap-3">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="flex-shrink-0 opacity-70 hover:opacity-100" title="Cerrar">
            <X size={16} />
          </button>
        </div>
      )}

      {/* Tarjetas de estadísticas */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          titulo="Ventas del mes"
          valor={bs(stats?.ventasStats?.montoVentasMes)}
          subtitulo={`${num(stats?.ventasStats?.totalVentasMes)} ventas registradas`}
          icon={<TrendingUp size={22} />}
          loading={loading}
        />
        <StatCard
          titulo="Ventas de hoy"
          valor={bs(stats?.ventasStats?.montoVentasHoy)}
          subtitulo={`${num(stats?.ventasStats?.totalVentasHoy)} hoy`}
          icon={<DollarSign size={22} />}
          loading={loading}
        />
        <StatCard
          titulo="Productos activos"
          valor={num(stats?.productosStats?.productosActivos)}
          subtitulo={`${num(stats?.productosStats?.totalProductos)} en el catálogo`}
          icon={<Package size={22} />}
          loading={loading}
        />
        <StatCard
          titulo="Clientes"
          valor={num(stats?.clientesStats?.totalClientes)}
          subtitulo={`${num(stats?.clientesStats?.clientesNuevosMes)} nuevos este mes`}
          icon={<Users size={22} />}
          loading={loading}
        />
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
              className="flex items-center justify-center gap-2 bg-primary-600 text-white px-6 py-4 rounded-xl hover:bg-primary-700 transition-colors font-medium text-sm shadow-sm"
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
