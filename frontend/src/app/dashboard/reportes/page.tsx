'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { TrendingUp, Package, DollarSign, Wallet, SlidersHorizontal, Clock, X } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import ReporteCard from '@/components/ReporteCard';
import ReporteParametrosModal from '@/components/ReporteParametrosModal';
import ReportePersonalizadoModal from '@/components/ReportePersonalizadoModal';
import { TipoReporte, ConfiguracionReporte, ReporteReciente } from '@/types/reporte';
import { resumenCriterios } from '@/lib/reporteCriterios';
import { agregarReciente, leerRecientes, limpiarRecientes, quitarReciente } from '@/lib/reportesRecientes';
import { useAuth } from '@/hooks/useAuth';

/**
 * Las cuatro tarjetas son los reportes que más se piden. Todo lo demás (y filtrar por estado,
 * método de pago, cliente, categoría...) sale del generador personalizado, y lo generado queda en
 * "Reportes recientes".
 */
const configuracionesReportes: ConfiguracionReporte[] = [
  {
    id: 'VENTAS',
    titulo: 'Ventas del período',
    descripcion: 'Cada venta del período, con sus totales',
    icono: 'TrendingUp',
    color: 'from-primary-500 to-primary-600',
    requiereFechas: true,
    requiereLimite: false,
    categorias: ['Ventas'],
  },
  {
    id: 'CUENTAS_POR_COBRAR',
    titulo: 'Cuentas por cobrar',
    descripcion: 'Ventas con saldo pendiente, ordenadas por antigüedad',
    icono: 'Wallet',
    color: 'from-primary-500 to-primary-600',
    requiereFechas: false,
    requiereLimite: false,
    categorias: ['Cobranza'],
  },
  {
    id: 'PRODUCTOS_MAS_VENDIDOS',
    titulo: 'Productos más vendidos',
    descripcion: 'Los productos con mayor cantidad de ventas',
    icono: 'Package',
    color: 'from-primary-500 to-primary-600',
    requiereFechas: false,
    requiereLimite: true,
    categorias: ['Productos'],
  },
  {
    id: 'INVENTARIO_VALORIZADO',
    titulo: 'Inventario valorizado',
    descripcion: 'Cuánto vale el stock actual',
    icono: 'DollarSign',
    color: 'from-primary-500 to-primary-600',
    requiereFechas: false,
    requiereLimite: false,
    categorias: ['Inventario'],
  },
];

const iconos: Record<string, any> = { TrendingUp, Package, DollarSign, Wallet };

export default function ReportesPage() {
  const router = useRouter();
  const { user, loading, isAdmin } = useAuth();
  const [tipoReporteSeleccionado, setTipoReporteSeleccionado] = useState<TipoReporte | null>(null);
  const [generadorAbierto, setGeneradorAbierto] = useState(false);
  const [recientes, setRecientes] = useState<ReporteReciente[]>([]);
  const [recienteAbierto, setRecienteAbierto] = useState<ReporteReciente | null>(null);

  // Reportes es solo para ADMIN. El menú ya no le muestra el enlace al
  // EMPLEADO, pero esto cierra el acceso directo por URL.
  useEffect(() => {
    if (!loading && user && !isAdmin()) {
      router.replace('/dashboard');
    }
  }, [loading, user, isAdmin, router]);

  useEffect(() => {
    setRecientes(leerRecientes());
  }, []);

  if (loading || !user || !isAdmin()) {
    return null;
  }

  const fechaHora = (iso: string) =>
    new Date(iso).toLocaleString('es-BO', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });

  const periodo = (r: ReporteReciente) =>
    r.fechaInicio && r.fechaFin ? `${r.fechaInicio.split('-').reverse().join('/')} al ${r.fechaFin.split('-').reverse().join('/')}` : null;

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Reportes' }]} />

      <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Reportes del negocio</h1>
          <p className="text-gray-600 mt-1">Los reportes más usados, o armá el tuyo</p>
        </div>
        <button
          onClick={() => setGeneradorAbierto(true)}
          className="inline-flex items-center gap-2 bg-primary-600 text-white px-5 py-2.5 rounded-lg hover:bg-primary-700 transition-colors font-medium shadow-sm"
        >
          <SlidersHorizontal size={20} />
          Generar reporte personalizado
        </button>
      </div>

      {/* Reportes recientes */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <Clock size={20} className="text-gray-500" />
            Reportes recientes
          </h2>
          {recientes.length > 0 && (
            <button
              onClick={() => setRecientes(limpiarRecientes())}
              className="text-sm text-gray-500 hover:text-gray-800"
            >
              Limpiar lista
            </button>
          )}
        </div>

        {recientes.length === 0 ? (
          <div className="bg-white border border-dashed border-gray-300 rounded-lg p-8 text-center">
            <p className="text-gray-600 font-medium">Todavía no generaste reportes personalizados</p>
            <p className="text-sm text-gray-500 mt-1">
              Los que armes con «Generar reporte personalizado» quedan acá para abrirlos de nuevo.
            </p>
          </div>
        ) : (
          <ul className="bg-white border border-gray-200 rounded-lg divide-y divide-gray-200">
            {recientes.map((r) => {
              const chips = [periodo(r), r.limite ? `Los ${r.limite} primeros` : null, ...resumenCriterios(r.criterios)].filter(
                Boolean
              ) as string[];
              return (
                <li key={r.id} className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50">
                  <button onClick={() => setRecienteAbierto(r)} className="flex-1 min-w-0 text-left">
                    <p className="text-sm font-semibold text-gray-900">{r.titulo}</p>
                    <div className="flex flex-wrap gap-1.5 mt-1">
                      {chips.map((chip) => (
                        <span key={chip} className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-700">
                          {chip}
                        </span>
                      ))}
                    </div>
                  </button>
                  <span className="text-xs text-gray-500 whitespace-nowrap hidden sm:block">{fechaHora(r.generado)}</span>
                  <button
                    onClick={() => setRecientes(quitarReciente(r.id))}
                    className="text-gray-400 hover:text-gray-700 p-1"
                    title="Quitar de la lista"
                  >
                    <X size={16} />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {/* Las cuatro tarjetas principales */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 items-stretch">
        {configuracionesReportes.map((config) => (
          <ReporteCard
            key={config.id}
            config={config}
            IconComponent={iconos[config.icono] ?? Package}
            onGenerar={() => setTipoReporteSeleccionado(config.id)}
          />
        ))}
      </div>

      {/* Parámetros de una tarjeta */}
      {tipoReporteSeleccionado && (
        <ReporteParametrosModal
          tipoReporte={tipoReporteSeleccionado}
          configuracion={configuracionesReportes.find((c) => c.id === tipoReporteSeleccionado)!}
          onClose={() => setTipoReporteSeleccionado(null)}
        />
      )}

      {/* Generador personalizado */}
      {generadorAbierto && (
        <ReportePersonalizadoModal
          onClose={() => setGeneradorAbierto(false)}
          onGenerado={(reporte) => setRecientes(agregarReciente(reporte))}
        />
      )}

      {/* Un reciente que se vuelve a abrir */}
      {recienteAbierto && (
        <ReportePersonalizadoModal reciente={recienteAbierto} onClose={() => setRecienteAbierto(null)} onGenerado={() => {}} />
      )}
    </div>
  );
}
