'use client';

import { useState } from 'react';
import { 
  TrendingUp, 
  Package, 
  Users, 
  DollarSign, 
  PieChart, 
  CreditCard,
  AlertTriangle,
  Building2,
  Truck,
  BarChart3
} from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import ReporteCard from '@/components/ReporteCard';
import ReporteParametrosModal from '@/components/ReporteParametrosModal';
import { TipoReporte, ConfiguracionReporte } from '@/types/reporte';

export default function ReportesPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [tipoReporteSeleccionado, setTipoReporteSeleccionado] = useState<TipoReporte | null>(null);

  const configuracionesReportes: ConfiguracionReporte[] = [
    {
      id: 'VENTAS',
      titulo: 'Reporte de Ventas',
      descripcion: 'Análisis de ventas por período, productos y clientes',
      icono: 'TrendingUp',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: true,
      requiereLimite: false,
      categorias: ['Ventas', 'Financiero']
    },
    {
      id: 'PRODUCTOS_MAS_VENDIDOS',
      titulo: 'Productos Más Vendidos',
      descripcion: 'Top de productos con mayor cantidad de ventas',
      icono: 'Package',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: false,
      requiereLimite: true,
      categorias: ['Productos', 'Ventas']
    },
    {
      id: 'CLIENTES_FRECUENTES',
      titulo: 'Clientes Frecuentes',
      descripcion: 'Clientes con mayor cantidad de compras',
      icono: 'Users',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: false,
      requiereLimite: true,
      categorias: ['Clientes', 'Ventas']
    },
    {
      id: 'INVENTARIO_VALORIZADO',
      titulo: 'Inventario Valorizado',
      descripcion: 'Valor total del inventario actual',
      icono: 'DollarSign',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: false,
      requiereLimite: false,
      categorias: ['Inventario', 'Financiero']
    },
    {
      id: 'VENTAS_POR_CATEGORIA',
      titulo: 'Ventas por Categoría',
      descripcion: 'Distribución de ventas según categorías de productos',
      icono: 'PieChart',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: true,
      requiereLimite: false,
      categorias: ['Ventas', 'Productos']
    },
    {
      id: 'VENTAS_POR_METODO_PAGO',
      titulo: 'Ventas por Método de Pago',
      descripcion: 'Métodos de pago más utilizados',
      icono: 'CreditCard',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: true,
      requiereLimite: false,
      categorias: ['Ventas', 'Pagos']
    },
    {
      id: 'INVENTARIO_STOCK_BAJO',
      titulo: 'Inventario Stock Bajo',
      descripcion: 'Productos que requieren reabastecimiento',
      icono: 'AlertTriangle',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: false,
      requiereLimite: false,
      categorias: ['Inventario', 'Alertas']
    },
    {
      id: 'PROVEEDORES',
      titulo: 'Reporte de Proveedores',
      descripcion: 'Análisis de compras y proveedores activos',
      icono: 'Building2',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: false,
      requiereLimite: false,
      categorias: ['Proveedores', 'Compras']
    },
    // Transportadoras queda fuera de alcance (declarado en el documento del
    // entregable): el módulo de Envíos/Transportadoras ya está comentado en
    // el menú lateral, mismo patrón acá.
    // {
    //   id: 'TRANSPORTADORAS',
    //   titulo: 'Reporte de Transportadoras',
    //   descripcion: 'Rendimiento y estadísticas de envíos',
    //   icono: 'Truck',
    //   color: 'from-primary-500 to-primary-600',
    //   requiereFechas: false,
    //   requiereLimite: false,
    //   categorias: ['Envíos', 'Logística']
    // },
    {
      id: 'FINANCIERO',
      titulo: 'Reporte Financiero',
      descripcion: 'Ingresos, gastos y ganancias del negocio',
      icono: 'BarChart3',
      color: 'from-primary-500 to-primary-600',
      requiereFechas: true,
      requiereLimite: false,
      categorias: ['Financiero', 'Análisis']
    }
  ];

  const handleOpenReporte = (tipoReporte: TipoReporte) => {
    setTipoReporteSeleccionado(tipoReporte);
    setIsModalOpen(true);
  };

  const getIconComponent = (iconName: string) => {
    const icons: Record<string, any> = {
      TrendingUp,
      Package,
      Users,
      DollarSign,
      PieChart,
      CreditCard,
      AlertTriangle,
      Building2,
      Truck,
      BarChart3
    };
    return icons[iconName] || Package;
  };

  return (
    <div className="max-w-full">
      <Breadcrumbs items={[{ label: 'Reportes' }]} />

      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Reportes y Análisis</h1>
        <p className="text-gray-600 mt-1">Genera informes detallados del sistema</p>
      </div>

      {/* Información */}
      <div className="bg-primary-50 border border-primary-200 rounded-lg p-4 mb-6">
        <div className="flex items-start gap-3">
          <BarChart3 className="text-primary-600 flex-shrink-0 mt-0.5" size={20} />
          <div>
            <h3 className="text-sm font-semibold text-primary-900 mb-1">¿Cómo generar un reporte?</h3>
            <p className="text-sm text-primary-800">
              Selecciona el tipo de reporte que deseas consultar y configura los parámetros
              (fechas, límites, etc.) para verlo en pantalla con sus gráficos.
            </p>
          </div>
        </div>
      </div>

      {/* Grid de Reportes */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {configuracionesReportes.map((config) => (
          <ReporteCard
            key={config.id}
            config={config}
            IconComponent={getIconComponent(config.icono)}
            onGenerar={() => handleOpenReporte(config.id)}
          />
        ))}
      </div>

      {/* Modal de Parámetros */}
      {isModalOpen && tipoReporteSeleccionado && (
        <ReporteParametrosModal
          tipoReporte={tipoReporteSeleccionado}
          configuracion={configuracionesReportes.find(c => c.id === tipoReporteSeleccionado)!}
          onClose={() => {
            setIsModalOpen(false);
            setTipoReporteSeleccionado(null);
          }}
        />
      )}
    </div>
  );
}